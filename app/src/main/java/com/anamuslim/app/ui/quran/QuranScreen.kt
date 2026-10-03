@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.anamuslim.app.ui.quran

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.anamuslim.app.R
import com.anamuslim.app.core.ServiceLocator
import com.anamuslim.app.data.settings.QuranDisplayMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

private const val QURAN_PDF_ASSET = "quran.pdf"
private const val TAG = "QuranScreen"

// نسبة أبعاد صفحات هذا المصحف. تم التحقق فعلياً أن كل الصفحات الـ569 بلا استثناء
// بنفس المقاس (595×842 نقطة)، فيُستخدم هذا الثابت لتخطيط الصفحات دون أي "قفزة"
// بصرية في الارتفاع قبل اكتمال رسم الصفحة.
private const val PAGE_ASPECT_RATIO = 595f / 842f

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f

@Composable
fun QuranScreen() {
    val context = LocalContext.current.applicationContext
    var documentState by remember { mutableStateOf<PdfDocumentState>(PdfDocumentState.Loading) }

    LaunchedEffect(context) {
        documentState = loadPdfDocument(context)
    }

    val displayMode by ServiceLocator.settings(context).quranDisplayMode
        .collectAsState(initial = QuranDisplayMode.CONTINUOUS_SCROLL)

    // عرض رسم مناسب لعرض الشاشة الفعلي (وليس أكبر مما يلزم) تُرسم به كل صفحة.
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val renderWidthPx = remember(screenWidthDp, density) {
        with(density) { screenWidthDp.dp.roundToPx() }.coerceIn(600, 1600)
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.nav_quran)) })

        when (val state = documentState) {
            PdfDocumentState.Loading -> LoadingPdf()
            PdfDocumentState.Missing -> PdfMessage(stringResource(R.string.quran_pdf_missing))
            PdfDocumentState.Error -> PdfMessage(stringResource(R.string.quran_pdf_error))
            is PdfDocumentState.Ready -> {
                // تنظيف الموارد (renderer + descriptor) مرتبط بمرجع ثابت محلي
                // (state.document) لا بإعادة قراءة متغيّر قابل للتغيّر، لتفادي أي
                // إغلاق مبكر غير مقصود للملف فور فتحه.
                DisposableEffect(state.document) {
                    onDispose { state.document.close() }
                }
                val cache = remember(state.document) { PageBitmapCache() }

                when (displayMode) {
                    QuranDisplayMode.CONTINUOUS_SCROLL ->
                        ContinuousScrollQuranReader(state.document, cache, renderWidthPx)
                    QuranDisplayMode.TAP_BUTTON ->
                        TapButtonQuranReader(state.document, cache, renderWidthPx)
                }
            }
        }
    }
}

/* ----------------------------- تحميل المستند ----------------------------- */

private sealed interface PdfDocumentState {
    object Loading : PdfDocumentState
    object Missing : PdfDocumentState
    object Error : PdfDocumentState
    data class Ready(val document: PdfDocument) : PdfDocumentState
}

/** يغلّف PdfRenderer + واصف الملف معاً، مع قفل يمنع استخدام الراسم من أكثر من مكان بنفس اللحظة. */
private class PdfDocument(
    private val descriptor: ParcelFileDescriptor,
    val renderer: PdfRenderer
) {
    val renderMutex = Mutex()
    fun close() {
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
    }
}

private suspend fun loadPdfDocument(context: Context): PdfDocumentState = withContext(Dispatchers.IO) {
    try {
        if (context.assets.list("")?.contains(QURAN_PDF_ASSET) != true) {
            return@withContext PdfDocumentState.Missing
        }
        val assetFile = File(context.cacheDir, QURAN_PDF_ASSET)
        context.assets.open(QURAN_PDF_ASSET).use { input ->
            assetFile.outputStream().use(input::copyTo)
        }
        val descriptor = ParcelFileDescriptor.open(assetFile, ParcelFileDescriptor.MODE_READ_ONLY)
        PdfDocumentState.Ready(PdfDocument(descriptor, PdfRenderer(descriptor)))
    } catch (e: Exception) {
        // سبب العطل الحقيقي يُسجَّل الآن بدل أن يُبتلع بصمت، ليسهل تشخيص أي مشكلة
        // مشابهة مستقبلاً. (السبب الذي كان يحدث فعلياً هنا: ملف PDF مشفّر بخوارزمية
        // RC4 — بلا أي كلمة مرور حقيقية — لا يدعمه PdfRenderer في أندرويد إطلاقاً
        // ويرفضه بـ SecurityException دائماً مهما كانت كلمة المرور فارغة.)
        Log.e(TAG, "فشل تحميل ملف القرآن (quran.pdf)", e)
        PdfDocumentState.Error
    }
}

@Composable
private fun LoadingPdf() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PdfMessage(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

/* -------------------------- تخزين مؤقت لصور الصفحات -------------------------- */

/**
 * ذاكرة تخزين مؤقت محدودة الحجم (LRU) لصور الصفحات المُرسَّمة. ضرورية لأن مصحفاً من
 * 569 صفحة لا يمكن رسمه بالكامل في الذاكرة دفعة واحدة، بينما يكتفي كل من الوضعين
 * (زر / تمرير متواصل) بما يُعرض فعلياً على الشاشة وما حولها فقط. مشتركة بين
 * الوضعين كي يكون التبديل بينهما من الإعدادات فورياً دون إعادة رسم ما سبق عرضه.
 */
private class PageBitmapCache(private val maxEntries: Int = 16) {
    private val map = LinkedHashMap<Int, Bitmap>(maxEntries, 0.75f, true)

    @Synchronized
    fun get(index: Int): Bitmap? = map[index]

    @Synchronized
    fun put(index: Int, bitmap: Bitmap) {
        map[index] = bitmap
        val iterator = map.keys.iterator()
        while (map.size > maxEntries && iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }
}

private suspend fun renderPageBitmap(
    document: PdfDocument,
    cache: PageBitmapCache,
    pageIndex: Int,
    targetWidthPx: Int
): Bitmap {
    cache.get(pageIndex)?.let { return it }
    return document.renderMutex.withLock {
        // أعد التحقق داخل القفل: ربما طلب آخر انتظر دوره ورسم هذه الصفحة فعلاً.
        cache.get(pageIndex) ?: withContext(Dispatchers.Default) {
            document.renderer.openPage(pageIndex).use { page ->
                val scale = targetWidthPx.toFloat() / page.width.toFloat()
                val w = targetWidthPx.coerceAtLeast(1)
                val h = (page.height * scale).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }.also { cache.put(pageIndex, it) }
    }
}

/* ------------------------------- وضع الزر ------------------------------- */

@Composable
private fun TapButtonQuranReader(document: PdfDocument, cache: PageBitmapCache, renderWidthPx: Int) {
    val lastPage = (document.renderer.pageCount - 1).coerceAtLeast(0)
    var currentPage by remember(document) { mutableIntStateOf(0) }
    val displayedPage = currentPage.coerceIn(0, lastPage)

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            ZoomableQuranPage(
                document = document,
                cache = cache,
                pageIndex = displayedPage,
                renderWidthPx = renderWidthPx,
                modifier = Modifier.fillMaxSize().padding(8.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { currentPage = displayedPage - 1 }, enabled = displayedPage > 0) {
                Text(stringResource(R.string.quran_previous_page))
            }
            Text(
                stringResource(R.string.quran_page_number, displayedPage + 1),
                style = MaterialTheme.typography.labelLarge
            )
            TextButton(onClick = { currentPage = displayedPage + 1 }, enabled = displayedPage < lastPage) {
                Text(stringResource(R.string.quran_next_page))
            }
        }
    }
}

/* -------------------------- وضع التمرير المتواصل -------------------------- */

@Composable
private fun ContinuousScrollQuranReader(document: PdfDocument, cache: PageBitmapCache, renderWidthPx: Int) {
    val pageCount = document.renderer.pageCount
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        items(count = pageCount, key = { it }) { index ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp)) {
                ZoomableQuranPage(
                    document = document,
                    cache = cache,
                    pageIndex = index,
                    renderWidthPx = renderWidthPx,
                    modifier = Modifier.fillMaxWidth().aspectRatio(PAGE_ASPECT_RATIO)
                )
                Text(
                    stringResource(R.string.quran_page_number, index + 1),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }
    }
}

/* --------------------- صفحة قابلة للتكبير، مشتركة بين الوضعين --------------------- */

/**
 * صفحة واحدة قابلة للتكبير والتصغير والتحريك باللمس. تُستخدم من كلا وضعي العرض، وتحتفظ
 * كل صفحة بحالة تكبيرها الخاصة بشكل مستقل (تكبير صفحة لا يؤثر على غيرها).
 *
 * إيماءة التكبير: إصبعان = قرص للتكبير/التصغير دائماً حتى داخل قائمة قابلة للتمرير،
 * وإصبع واحد = تحريك (Pan) فقط إن كانت الصفحة مكبّرة بالفعل. خلاف ذلك يُترك السحب
 * بإصبع واحد ليمرّ دون استهلاك إلى القائمة الأم، فيعمل التمرير العمودي الطبيعي بين
 * الصفحات بلا أي تعارض — تماماً كما في تطبيقات قراءة PDF المعروفة.
 */
@Composable
private fun ZoomableQuranPage(
    document: PdfDocument,
    cache: PageBitmapCache,
    pageIndex: Int,
    renderWidthPx: Int,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(document, pageIndex) { mutableStateOf(cache.get(pageIndex)) }
    var scale by remember(document, pageIndex) { mutableFloatStateOf(MIN_ZOOM) }
    var offset by remember(document, pageIndex) { mutableStateOf(Offset.Zero) }
    var containerSize by remember(document, pageIndex) { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(document, pageIndex, renderWidthPx) {
        if (bitmap == null) {
            bitmap = runCatching { renderPageBitmap(document, cache, pageIndex, renderWidthPx) }
                .onFailure { Log.e(TAG, "فشل رسم صفحة رقم ${pageIndex + 1}", it) }
                .getOrNull()
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .clipToBounds()
            .pointerInput(document, pageIndex) {
                detectPinchZoomAndPan(isZoomedIn = { scale > 1.001f }) { pan, zoomChange ->
                    val newScale = (scale * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
                    scale = newScale
                    offset = if (newScale <= 1.001f) {
                        Offset.Zero
                    } else {
                        val maxX = containerSize.width * (newScale - 1f) / 2f
                        val maxY = containerSize.height * (newScale - 1f) / 2f
                        Offset(
                            (offset.x + pan.x).coerceIn(-maxX, maxX),
                            (offset.y + pan.y).coerceIn(-maxY, maxY)
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = stringResource(R.string.quran_page_number, pageIndex + 1),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
        } else {
            CircularProgressIndicator()
        }
    }
}

/**
 * نسخة مبسّطة من كاشف إيماءات القرص القياسي في Compose (detectTransformGestures)،
 * بفارق جوهري واحد: لا تستهلك أي حركة لإصبع واحد إطلاقاً إلا إذا كانت الصفحة مكبّرة
 * بالفعل، كي لا تتعارض مع التمرير العمودي الطبيعي بإصبع واحد بين الصفحات.
 */
private suspend fun PointerInputScope.detectPinchZoomAndPan(
    isZoomedIn: () -> Boolean,
    onGesture: (pan: Offset, zoomChange: Float) -> Unit
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var keepGoing = true
        while (keepGoing) {
            val event = awaitPointerEvent()
            val changes = event.changes
            val canceled = changes.any { it.isConsumed }
            if (!canceled) {
                val shouldHandle = changes.size >= 2 || isZoomedIn()
                if (shouldHandle) {
                    val zoomChange = event.calculateZoom()
                    val panChange = event.calculatePan()
                    if (zoomChange != 1f || panChange != Offset.Zero) {
                        onGesture(panChange, zoomChange)
                    }
                    changes.forEach { it.consume() }
                }
            }
            keepGoing = !canceled && changes.any { it.pressed }
        }
    }
}
