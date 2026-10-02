@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.anamuslim.app.ui.quran

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.ImageView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.anamuslim.app.R
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val QURAN_PDF_ASSET = "quran.pdf"

@Composable
fun QuranScreen() {
    val context = LocalContext.current.applicationContext
    var documentState by remember { mutableStateOf<PdfDocumentState>(PdfDocumentState.Loading) }
    var currentPage by remember { mutableIntStateOf(0) }

    LaunchedEffect(context) {
        documentState = loadPdfDocument(context)
    }

    DisposableEffect(documentState) {
        onDispose {
            (documentState as? PdfDocumentState.Ready)?.document?.close()
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.nav_quran)) })

        when (val state = documentState) {
            PdfDocumentState.Loading -> LoadingPdf()
            is PdfDocumentState.Missing -> MissingPdf()
            is PdfDocumentState.Error -> PdfError()
            is PdfDocumentState.Ready -> {
                val lastPage = state.document.renderer.pageCount - 1
                val displayedPage = currentPage.coerceIn(0, lastPage)
                AndroidView(
                    factory = { PdfPageImageView(it) },
                    modifier = Modifier.weight(1f),
                    update = { it.showPage(state.document.renderer, displayedPage) }
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { currentPage = displayedPage - 1 }, enabled = displayedPage > 0) {
                        Text(stringResource(R.string.quran_previous_page))
                    }
                    Text(stringResource(R.string.quran_page_number, displayedPage + 1))
                    TextButton(onClick = { currentPage = displayedPage + 1 }, enabled = displayedPage < lastPage) {
                        Text(stringResource(R.string.quran_next_page))
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingPdf() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
}

@Composable
private fun MissingPdf() = PdfMessage(R.string.quran_pdf_missing)

@Composable
private fun PdfError() = PdfMessage(R.string.quran_pdf_error)

@Composable
private fun PdfMessage(messageRes: Int) = Box(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    contentAlignment = Alignment.Center
) {
    Text(
        text = stringResource(messageRes),
        style = MaterialTheme.typography.bodyLarge
    )
}

private sealed interface PdfDocumentState {
    data object Loading : PdfDocumentState
    data object Missing : PdfDocumentState
    data object Error : PdfDocumentState
    data class Ready(val document: PdfDocument) : PdfDocumentState
}

private class PdfDocument(
    val descriptor: ParcelFileDescriptor,
    val renderer: PdfRenderer
) : AutoCloseable {
    override fun close() {
        renderer.close()
        descriptor.close()
    }
}

private suspend fun loadPdfDocument(context: Context): PdfDocumentState = withContext(Dispatchers.IO) {
    try {
        val assetFile = File(context.cacheDir, QURAN_PDF_ASSET)
        if (context.assets.list("")?.contains(QURAN_PDF_ASSET) != true) {
            return@withContext PdfDocumentState.Missing
        }
        context.assets.open(QURAN_PDF_ASSET).use { input ->
            assetFile.outputStream().use(input::copyTo)
        }
        val descriptor = ParcelFileDescriptor.open(assetFile, ParcelFileDescriptor.MODE_READ_ONLY)
        PdfDocumentState.Ready(PdfDocument(descriptor, PdfRenderer(descriptor)))
    } catch (_: Exception) {
        PdfDocumentState.Error
    }
}

private class PdfPageImageView(context: Context) : ImageView(context) {
    private var renderer: PdfRenderer? = null
    private var pageIndex: Int = 0

    init {
        setBackgroundColor(Color.WHITE)
        scaleType = ScaleType.FIT_CENTER
    }

    fun showPage(renderer: PdfRenderer, pageIndex: Int) {
        this.renderer = renderer
        this.pageIndex = pageIndex
        renderPage()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        renderPage()
    }

    private fun renderPage() {
        val activeRenderer = renderer ?: return
        if (width <= 0 || height <= 0 || pageIndex !in 0 until activeRenderer.pageCount) return

        activeRenderer.openPage(pageIndex).use { page ->
            val scale = minOf(width.toFloat() / page.width, height.toFloat() / page.height)
            val bitmap = Bitmap.createBitmap(
                (page.width * scale).toInt().coerceAtLeast(1),
                (page.height * scale).toInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            setImageBitmap(bitmap)
        }
    }
}
