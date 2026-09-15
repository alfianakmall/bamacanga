package eu.kanade.presentation.history.components

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate
import tachiyomi.domain.manga.model.MangaCover

enum class HistoryPeriod {
    THIS_MONTH,
    THIS_WEEK,
    ALL_TIME,
}

@Immutable
data class HistoryStatsData(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val timeReadMs: Long = 0L,
    val pagesRead: Int = 0,
    val favoriteMangaTitle: String? = null,
    val favoriteMangaCover: MangaCover? = null,
    val favoriteMangaId: Long? = null,
    val daysRead: Int = 0,
    val titlesAdded: Int = 0,
    val avgChaptersPerDay: Double = 0.0,
    val triviaText: String = "",
    val activityByDate: Map<LocalDate, Long> = emptyMap(),
    val trendChartPoints: List<Pair<Int, Int>> = emptyList(),
    val totalPagesThisMonth: Int = 0,
)
