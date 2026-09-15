package eu.kanade.presentation.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.history.components.HistoryPeriod
import eu.kanade.presentation.history.components.HistoryStatsPage
import eu.kanade.presentation.history.components.PageDotsIndicator
import eu.kanade.presentation.history.components.ReadingHeatmapCard
import eu.kanade.presentation.history.components.ReadingTimelineCard
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.ui.history.HistoryViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.DeleteSweep
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(
    state: HistoryViewModel.State,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChange: (String?) -> Unit,
    onClickCover: (mangaId: Long) -> Unit,
    onClickResume: (mangaId: Long, chapterId: Long) -> Unit,
    onClickFavorite: (mangaId: Long) -> Unit,
    onDialogChange: (HistoryViewModel.Dialog?) -> Unit,
    onPeriodChange: (HistoryPeriod) -> Unit = {},
) {
    Scaffold(
        topBar = { scrollBehavior ->
            SearchToolbar(
                titleContent = { AppBarTitle(stringResource(MR.strings.history)) },
                searchQuery = state.searchQuery,
                onChangeSearchQuery = onSearchQueryChange,
                actions = {
                    AppBarActions(
                        listOf(
                            AppBar.Action(
                                title = stringResource(MR.strings.pref_clear_history),
                                icon = MaterialSymbols.Rounded.DeleteSweep,
                                onClick = {
                                    onDialogChange(HistoryViewModel.Dialog.DeleteAll)
                                },
                            ),
                        ),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        state.list.let {
            if (it == null) {
                LoadingScreen(Modifier.padding(contentPadding))
            } else if (it.isEmpty() && !state.searchQuery.isNullOrEmpty()) {
                EmptyScreen(
                    stringRes = MR.strings.no_results_found,
                    modifier = Modifier.padding(contentPadding),
                )
            } else {
                val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

                Box(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        when (page) {
                            0 -> {
                                ReadingTimelinePageContent(
                                    history = it,
                                    activityByDate = state.stats.activityByDate,
                                    contentPadding = PaddingValues(
                                        top = contentPadding.calculateTopPadding(),
                                        bottom = contentPadding.calculateBottomPadding() + 44.dp,
                                    ),
                                    onClickCover = onClickCover,
                                    onClickResume = onClickResume,
                                )
                            }
                            1 -> {
                                HistoryStatsPage(
                                    stats = state.stats,
                                    period = state.period,
                                    onPeriodChange = onPeriodChange,
                                    onClickManga = onClickCover,
                                    contentPadding = PaddingValues(
                                        top = contentPadding.calculateTopPadding(),
                                        bottom = contentPadding.calculateBottomPadding() + 44.dp,
                                    ),
                                )
                            }
                        }
                    }

                    // Dot page indicator at the bottom
                    PageDotsIndicator(
                        pagerState = pagerState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = contentPadding.calculateBottomPadding() + 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingTimelinePageContent(
    history: List<HistoryUiModel>,
    activityByDate: Map<LocalDate, Long>,
    contentPadding: PaddingValues,
    onClickCover: (Long) -> Unit,
    onClickResume: (mangaId: Long, chapterId: Long) -> Unit,
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.US)
    }

    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        // 1. Mini heatmap activity widget at the top
        item(key = "reading-heatmap") {
            ReadingHeatmapCard(activityByDate = activityByDate)
        }

        // 2. Large bold "Reading Timeline" header
        item(key = "reading-timeline-title") {
            Text(
                text = "Reading Timeline",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        // 3. Timeline items with uppercase date headers & card items
        items(
            items = history,
            key = { "history-${it.hashCode()}" },
            contentType = {
                when (it) {
                    is HistoryUiModel.Header -> "header"
                    is HistoryUiModel.Item -> "item"
                }
            },
        ) { item ->
            when (item) {
                is HistoryUiModel.Header -> {
                    val dateFormatted = remember(item.date) {
                        "📅 " + dateFormatter.format(item.date.toJavaLocalDate()).uppercase()
                    }
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                    )
                }
                is HistoryUiModel.Item -> {
                    ReadingTimelineCard(
                        modifier = Modifier.animateItem(),
                        history = item,
                        onClickCover = { onClickCover(item.item.mangaId) },
                        onClickResume = { onClickResume(item.item.mangaId, item.latestChapterId) },
                    )
                }
            }
        }
    }
}

sealed interface HistoryUiModel {
    data class Header(val date: LocalDate) : HistoryUiModel
    data class Item(
        val item: HistoryWithRelations,
        val chapters: List<Double> = listOf(item.chapterNumber),
        val totalDuration: Long = item.readDuration,
        val latestChapterId: Long = item.chapterId,
    ) : HistoryUiModel
}

@PreviewLightDark
@Composable
internal fun HistoryScreenPreviews(
    @PreviewParameter(HistoryviewModelStateProvider::class)
    historyState: HistoryViewModel.State,
) {
    TachiyomiPreviewTheme {
        HistoryScreen(
            state = historyState,
            snackbarHostState = SnackbarHostState(),
            onSearchQueryChange = {},
            onClickCover = {},
            onClickResume = { _, _ -> run {} },
            onDialogChange = {},
            onClickFavorite = {},
        )
    }
}
