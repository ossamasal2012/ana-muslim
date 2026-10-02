@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.anamuslim.app.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anamuslim.app.R
import com.anamuslim.app.data.quran.QURAN_TOTAL_PAGES
import com.anamuslim.app.data.quran.QuranPageCache
import kotlinx.coroutines.launch

@Composable
fun QuranScreen(viewModel: QuranViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.nav_quran)) })

        // ننتظر اكتمال استرجاع "آخر صفحة محفوظة" قبل إنشاء الـ Pager حتى يبدأ
        // من الصفحة الصحيحة مباشرة، بدل أن يومض أولاً على الصفحة 1
        if (state.pageData == null && state.errorMessage == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return
        }

        val pagerState = rememberPagerState(
            initialPage = state.currentPage - 1,
            pageCount = { QURAN_TOTAL_PAGES }
        )
        val scope = rememberCoroutineScope()

        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage + 1 != state.currentPage) {
                viewModel.goToPage(pagerState.currentPage + 1)
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            val pageNumber = pageIndex + 1
            if (pageNumber == state.currentPage && state.pageData != null) {
                QuranPageContent(state.pageData!!)
            } else if (pageNumber == state.currentPage && state.errorMessage != null) {
                QuranPageError(onRetry = { viewModel.goToPage(pageNumber) })
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }

        QuranBottomBar(
            pageNumber = state.currentPage,
            juzNumber = state.pageData?.juzNumber ?: 1,
            onPrev = { scope.launch { pagerState.animateScrollToPage((state.currentPage - 2).coerceIn(0, QURAN_TOTAL_PAGES - 1)) } },
            onNext = { scope.launch { pagerState.animateScrollToPage(state.currentPage.coerceIn(0, QURAN_TOTAL_PAGES - 1)) } }
        )
    }
}

@Composable
private fun QuranPageContent(page: QuranPageCache) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        val builder = StringBuilder()
        page.ayahs.forEachIndexed { idx, ayah ->
            if (ayah.isNewSurahStart) {
                if (builder.isNotEmpty()) {
                    RenderAyahBlock(builder.toString())
                    builder.clear()
                }
                Spacer(Modifier.height(12.dp))
                SurahHeader(ayah.surahName)
                Spacer(Modifier.height(12.dp))
            }
            builder.append(ayah.text)
            builder.append(" \u06DD") // رمز نهاية آية بسيط (فاصلة قرآنية دائرية تقليدية)
            builder.append(toArabicDigits(ayah.numberInSurah))
            builder.append("  ")
        }
        if (builder.isNotEmpty()) RenderAyahBlock(builder.toString())

        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.quran_attribution),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            stringResource(R.string.quran_attribution_api),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            stringResource(R.string.quran_attribution_license),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun RenderAyahBlock(text: String) {
    Text(
        text = text,
        fontFamily = com.anamuslim.app.ui.theme.QuranFontFamily,
        fontSize = 22.sp,
        lineHeight = 42.sp,
        textAlign = TextAlign.Justify,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun SurahHeader(name: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun QuranPageError(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.quran_no_internet_first_time), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.quran_retry)) }
    }
}

@Composable
private fun QuranBottomBar(pageNumber: Int, juzNumber: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onPrev, enabled = pageNumber < QURAN_TOTAL_PAGES) { Text("‹ " + stringResource(R.string.quran_page_number, pageNumber + 1)) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.quran_page_number, pageNumber), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.quran_juz_number, juzNumber), style = MaterialTheme.typography.labelMedium)
            }
            TextButton(onClick = onNext, enabled = pageNumber > 1) { Text(stringResource(R.string.quran_page_number, pageNumber - 1) + " ›") }
        }
    }
}

private val arabicDigits = charArrayOf('٠','١','٢','٣','٤','٥','٦','٧','٨','٩')
private fun toArabicDigits(n: Int): String = n.toString().map { c -> if (c.isDigit()) arabicDigits[c - '0'] else c }.joinToString("")
