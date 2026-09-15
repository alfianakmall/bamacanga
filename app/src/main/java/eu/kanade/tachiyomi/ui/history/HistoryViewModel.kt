package eu.kanade.tachiyomi.ui.history

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.core.util.insertSeparators
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.presentation.history.HistoryUiModel
import eu.kanade.tachiyomi.util.lang.toLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.history.interactor.RemoveHistory
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetDuplicateLibraryManga
import eu.kanade.presentation.history.components.HistoryPeriod
import eu.kanade.presentation.history.components.HistoryStatsData
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.source.service.SourceManager
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class HistoryViewModel(
    private val addTracks: AddTracks,
    private val getCategories: GetCategories,
    private val getDuplicateLibraryManga: GetDuplicateLibraryManga,
    private val getHistory: GetHistory,
    private val getLibraryManga: GetLibraryManga,
    private val getManga: GetManga,
    private val getNextChapters: GetNextChapters,
    private val libraryPreferences: LibraryPreferences,
    private val removeHistory: RemoveHistory,
    private val setMangaCategories: SetMangaCategories,
    private val updateManga: UpdateManga,
    private val sourceManager: SourceManager,
) : ViewModel() {

    val snackbarHostState: SnackbarHostState = SnackbarHostState()

    private val _events: Channel<Event> = Channel(Channel.UNLIMITED)
    val events: Flow<Event> = _events.receiveAsFlow()

    private val searchQuery = MutableStateFlow<String?>(null)

    private val dialog = MutableStateFlow<Dialog?>(null)

    private val selectedPeriod = MutableStateFlow(HistoryPeriod.THIS_MONTH)

    private val rawHistory = searchQuery
        .flatMapLatest { query ->
            getHistory.subscribe(query ?: "")
                .distinctUntilChanged()
                .catch { error ->
                    logcat(LogPriority.ERROR, error)
                    _events.send(Event.InternalError)
                }
                .flowOn(Dispatchers.IO)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), emptyList())

    private val libraryMangaFlow = getLibraryManga.subscribe()
        .catch { logcat(LogPriority.ERROR, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), emptyList())

    private val stats: StateFlow<HistoryStatsData> = combine(
        rawHistory,
        libraryMangaFlow,
        selectedPeriod,
    ) { historyList, libraryList, period ->
        calculateStats(historyList, libraryList, period)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), HistoryStatsData())

    val state: StateFlow<State> = combine(
        searchQuery,
        rawHistory.map { it.toHistoryUiModels() },
        dialog,
        stats,
        selectedPeriod,
    ) { searchQuery, historyList, dialog, statsData, period ->
        State(
            searchQuery = searchQuery,
            list = historyList,
            dialog = dialog,
            stats = statsData,
            period = period,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), State())

    private fun List<HistoryWithRelations>.toHistoryUiModels(): List<HistoryUiModel> {
        val result = mutableListOf<HistoryUiModel>()
        val groupedByDate = this
            .filter { it.readAt != null }
            .groupBy { it.readAt!!.time.toLocalDate() }
            .toSortedMap(compareByDescending { it })

        for ((date, entriesOnDate) in groupedByDate) {
            result.add(HistoryUiModel.Header(date))
            val groupedByManga = entriesOnDate.groupBy { it.mangaId }
            for ((_, mangaEntries) in groupedByManga) {
                val latest = mangaEntries.first()
                val chapterNumbers = mangaEntries.map { it.chapterNumber }.distinct().sorted()
                val totalDuration = mangaEntries.sumOf { it.readDuration }
                result.add(
                    HistoryUiModel.Item(
                        item = latest,
                        chapters = chapterNumbers,
                        totalDuration = totalDuration,
                        latestChapterId = latest.chapterId,
                    ),
                )
            }
        }
        return result
    }

    fun setPeriod(period: HistoryPeriod) {
        selectedPeriod.update { period }
    }

    private fun calculateStats(
        historyList: List<HistoryWithRelations>,
        libraryList: List<LibraryManga>,
        period: HistoryPeriod,
    ): HistoryStatsData {
        if (historyList.isEmpty()) {
            return HistoryStatsData(
                titlesAdded = libraryList.size,
                triviaText = "Read manga to see fun comparison trivia!",
            )
        }

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val allDates = historyList.mapNotNull { it.readAt?.time?.toLocalDate() }.toSet()

        // 1. Current Streak
        var currentStreak = 0
        if (allDates.contains(today)) {
            currentStreak = 1
            var checkDate = today.minus(1, DateTimeUnit.DAY)
            while (allDates.contains(checkDate)) {
                currentStreak++
                checkDate = checkDate.minus(1, DateTimeUnit.DAY)
            }
        } else {
            val yesterday = today.minus(1, DateTimeUnit.DAY)
            if (allDates.contains(yesterday)) {
                currentStreak = 1
                var checkDate = yesterday.minus(1, DateTimeUnit.DAY)
                while (allDates.contains(checkDate)) {
                    currentStreak++
                    checkDate = checkDate.minus(1, DateTimeUnit.DAY)
                }
            }
        }

        // Longest Streak
        val sortedDates = allDates.sorted()
        var longestStreak = 0
        var tempStreak = 0
        var prevDate: LocalDate? = null
        for (date in sortedDates) {
            if (prevDate == null) {
                tempStreak = 1
            } else {
                if (prevDate.daysUntil(date) == 1) {
                    tempStreak++
                } else {
                    tempStreak = 1
                }
            }
            if (tempStreak > longestStreak) {
                longestStreak = tempStreak
            }
            prevDate = date
        }

        // 2. Activity Heatmap
        val activityByDate = mutableMapOf<LocalDate, Long>()
        for (entry in historyList) {
            val d = entry.readAt?.time?.toLocalDate() ?: continue
            activityByDate[d] = (activityByDate[d] ?: 0L) + maxOf(entry.readDuration, 60_000L)
        }

        // 3. Filter by Period
        val periodStartDate = when (period) {
            HistoryPeriod.THIS_MONTH -> LocalDate(today.year, today.month, 1)
            HistoryPeriod.THIS_WEEK -> {
                val dayOfWeek = today.dayOfWeek.isoDayNumber
                today.minus(dayOfWeek - 1, DateTimeUnit.DAY)
            }
            HistoryPeriod.ALL_TIME -> LocalDate(1970, 1, 1)
        }

        val filteredEntries = historyList.filter {
            val d = it.readAt?.time?.toLocalDate()
            d != null && d >= periodStartDate && d <= today
        }

        val timeReadMs = filteredEntries.sumOf { it.readDuration }
        val daysRead = filteredEntries.mapNotNull { it.readAt?.time?.toLocalDate() }.distinct().size

        val chaptersReadCount = filteredEntries.size
        val pagesRead = chaptersReadCount * 21

        val favoriteGroup = filteredEntries.groupBy { it.mangaId }
            .maxByOrNull { (_, entries) -> entries.sumOf { it.readDuration }.takeIf { it > 0 } ?: entries.size.toLong() }

        val favoriteManga = favoriteGroup?.value?.firstOrNull()

        val titlesAdded = when (period) {
            HistoryPeriod.THIS_MONTH -> {
                val monthStartEpoch = periodStartDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                libraryList.count { it.manga.dateAdded >= monthStartEpoch }
            }
            HistoryPeriod.THIS_WEEK -> {
                val weekStartEpoch = periodStartDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                libraryList.count { it.manga.dateAdded >= weekStartEpoch }
            }
            HistoryPeriod.ALL_TIME -> libraryList.size
        }.let { if (it == 0 && libraryList.isNotEmpty()) libraryList.size else it }

        val avgChaptersPerDay = if (daysRead > 0) {
            chaptersReadCount.toDouble() / daysRead
        } else {
            0.0
        }

        val wordsEstimate = pagesRead * 50
        val fahrenheitWords = 46118.0
        val ratio = wordsEstimate / fahrenheitWords
        val ratioFormatted = String.format(Locale.US, "%.1f", maxOf(0.1, ratio))
        val triviaText = "You've read ${ratioFormatted}x as many words as Fahrenheit 451 this month."

        val startOfMonth = LocalDate(today.year, today.month, 1)
        val monthEntries = historyList.filter {
            val d = it.readAt?.time?.toLocalDate()
            d != null && d >= startOfMonth && d <= today
        }
        val pagesByDay = mutableMapOf<Int, Int>()
        for (entry in monthEntries) {
            val day = entry.readAt?.time?.toLocalDate()?.day ?: continue
            pagesByDay[day] = (pagesByDay[day] ?: 0) + 21
        }
        var cumPages = 0
        val trendPoints = mutableListOf<Pair<Int, Int>>()
        for (day in 1..today.day) {
            cumPages += pagesByDay[day] ?: 0
            trendPoints.add(Pair(day, cumPages))
        }

        return HistoryStatsData(
            currentStreak = currentStreak,
            longestStreak = maxOf(longestStreak, currentStreak),
            timeReadMs = timeReadMs,
            pagesRead = pagesRead,
            favoriteMangaTitle = favoriteManga?.title,
            favoriteMangaCover = favoriteManga?.coverData,
            favoriteMangaId = favoriteManga?.mangaId,
            daysRead = daysRead,
            titlesAdded = titlesAdded,
            avgChaptersPerDay = avgChaptersPerDay,
            triviaText = triviaText,
            activityByDate = activityByDate,
            trendChartPoints = trendPoints,
            totalPagesThisMonth = cumPages,
        )
    }

    suspend fun getNextChapter(): Chapter? {
        return withIOContext { getNextChapters.await(onlyUnread = false).firstOrNull() }
    }

    fun getNextChapterForManga(mangaId: Long, chapterId: Long) {
        viewModelScope.launchIO {
            sendNextChapterEvent(getNextChapters.await(mangaId, chapterId, onlyUnread = false))
        }
    }

    private suspend fun sendNextChapterEvent(chapters: List<Chapter>) {
        val chapter = chapters.firstOrNull()
        _events.send(Event.OpenChapter(chapter))
    }

    fun removeFromHistory(history: HistoryWithRelations) {
        viewModelScope.launchIO {
            removeHistory.await(history)
        }
    }

    fun removeAllFromHistory(mangaId: Long) {
        viewModelScope.launchIO {
            removeHistory.await(mangaId)
        }
    }

    fun removeAllHistory() {
        viewModelScope.launchIO {
            val result = removeHistory.awaitAll()
            if (!result) return@launchIO
            _events.send(Event.HistoryCleared)
        }
    }

    fun updateSearchQuery(query: String?) {
        searchQuery.update { query }
    }

    fun setDialog(dialog: Dialog?) {
        this.dialog.update { dialog }
    }

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    suspend fun getCategories(): List<Category> {
        return getCategories.await().filterNot { it.isSystemCategory }
    }

    private fun moveMangaToCategory(mangaId: Long, categories: Category?) {
        val categoryIds = listOfNotNull(categories).map { it.id }
        moveMangaToCategory(mangaId, categoryIds)
    }

    private fun moveMangaToCategory(mangaId: Long, categoryIds: List<Long>) {
        viewModelScope.launchIO {
            setMangaCategories.await(mangaId, categoryIds)
        }
    }

    fun moveMangaToCategoriesAndAddToLibrary(manga: Manga, categories: List<Long>) {
        moveMangaToCategory(manga.id, categories)
        if (manga.favorite) return

        viewModelScope.launchIO {
            updateManga.awaitUpdateFavorite(manga.id, true)
        }
    }

    private suspend fun getMangaCategoryIds(manga: Manga): List<Long> {
        return getCategories.await(manga.id)
            .map { it.id }
    }

    fun addFavorite(mangaId: Long) {
        viewModelScope.launchIO {
            val manga = getManga.await(mangaId) ?: return@launchIO

            val duplicates = getDuplicateLibraryManga(manga)
            if (duplicates.isNotEmpty()) {
                dialog.update { Dialog.DuplicateManga(manga, duplicates) }
                return@launchIO
            }

            addFavorite(manga)
        }
    }

    fun addFavorite(manga: Manga) {
        viewModelScope.launchIO {
            // Move to default category if applicable
            val categories = getCategories()
            val defaultCategoryId = libraryPreferences.defaultCategory.get().toLong()
            val defaultCategory = categories.find { it.id == defaultCategoryId }

            when {
                // Default category set
                defaultCategory != null -> {
                    val result = updateManga.awaitUpdateFavorite(manga.id, true)
                    if (!result) return@launchIO
                    moveMangaToCategory(manga.id, defaultCategory)
                }

                // Automatic 'Default' or no categories
                defaultCategoryId == 0L || categories.isEmpty() -> {
                    val result = updateManga.awaitUpdateFavorite(manga.id, true)
                    if (!result) return@launchIO
                    moveMangaToCategory(manga.id, null)
                }

                // Choose a category
                else -> showChangeCategoryDialog(manga)
            }

            // Sync with tracking services if applicable
            addTracks.bindEnhancedTrackers(manga, sourceManager.getOrStub(manga.source))
        }
    }

    fun showMigrateDialog(target: Manga, current: Manga) {
        dialog.update { Dialog.Migrate(target = target, current = current) }
    }

    fun showChangeCategoryDialog(manga: Manga) {
        viewModelScope.launch {
            val categories = getCategories()
            val selection = getMangaCategoryIds(manga)
            dialog.update {
                Dialog.ChangeCategory(
                    manga = manga,
                    initialSelection = categories.mapAsCheckboxState { it.id in selection },
                )
            }
        }
    }

    @Immutable
    data class State(
        val searchQuery: String? = null,
        val list: List<HistoryUiModel>? = null,
        val dialog: Dialog? = null,
        val stats: HistoryStatsData = HistoryStatsData(),
        val period: HistoryPeriod = HistoryPeriod.THIS_MONTH,
    )

    sealed interface Dialog {
        data object DeleteAll : Dialog
        data class Delete(val history: HistoryWithRelations) : Dialog
        data class DuplicateManga(val manga: Manga, val duplicates: List<MangaWithChapterCount>) : Dialog
        data class ChangeCategory(
            val manga: Manga,
            val initialSelection: List<CheckboxState<Category>>,
        ) : Dialog
        data class Migrate(val target: Manga, val current: Manga) : Dialog
    }

    sealed interface Event {
        data class OpenChapter(val chapter: Chapter?) : Event
        data object InternalError : Event
        data object HistoryCleared : Event
    }
}
