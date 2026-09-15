package eu.kanade.tachiyomi.data.library

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.zacsweers.metro.Inject
import eu.kanade.domain.manga.interactor.RefreshMangaCover
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.isRunning
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import logcat.LogPriority
import mihon.app.di.AppGraph
import mihon.core.metro.metroGraph
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement

@OptIn(ExperimentalAtomicApi::class)
class CoverUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val graph: AppGraph = context.metroGraph()

    @Inject private lateinit var getLibraryManga: GetLibraryManga

    @Inject private lateinit var getManga: GetManga

    @Inject private lateinit var refreshMangaCover: RefreshMangaCover

    @Inject private lateinit var notifier: LibraryUpdateNotifier

    private var mangaToUpdate: List<Manga> = mutableListOf()

    override suspend fun doWork(): Result {
        graph.inject(this)

        setForegroundSafely()

        addMangaToQueue()

        return withIOContext {
            try {
                updateCovers()
                Result.success()
            } catch (e: Exception) {
                if (e is CancellationException) {
                    Result.success()
                } else {
                    logcat(LogPriority.ERROR, e)
                    Result.failure()
                }
            } finally {
                notifier.cancelProgressNotification()
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            Notifications.ID_LIBRARY_PROGRESS,
            notifier.progressNotificationBuilder.build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    private suspend fun addMangaToQueue() {
        val mangaIds = inputData.getLongArray(KEY_MANGA_IDS)?.toList()
        mangaToUpdate = if (!mangaIds.isNullOrEmpty()) {
            mangaIds.mapNotNull { getManga.await(it) }
        } else {
            getLibraryManga.await().map { it.manga }
        }
    }

    private suspend fun updateCovers() {
        val semaphore = Semaphore(3)
        val progressCount = AtomicInt(0)
        val currentlyUpdatingManga = CopyOnWriteArrayList<Manga>()

        coroutineScope {
            mangaToUpdate.groupBy { it.source }
                .values
                .map { mangaInSource ->
                    async {
                        semaphore.withPermit {
                            mangaInSource.forEach { manga ->
                                ensureActive()

                                withUpdateNotification(
                                    currentlyUpdatingManga,
                                    progressCount,
                                    manga,
                                ) {
                                    try {
                                        refreshMangaCover(manga).getOrThrow()
                                    } catch (e: Throwable) {
                                        logcat(LogPriority.ERROR, e) { "Failed to refresh cover in bulk for ${manga.title}" }
                                    }
                                }
                            }
                        }
                    }
                }
                .awaitAll()
        }

        notifier.cancelProgressNotification()
    }

    private suspend fun withUpdateNotification(
        updatingManga: CopyOnWriteArrayList<Manga>,
        completed: AtomicInt,
        manga: Manga,
        block: suspend () -> Unit,
    ) = coroutineScope {
        ensureActive()

        updatingManga.add(manga)
        notifier.showProgressNotification(
            updatingManga,
            completed.load(),
            mangaToUpdate.size,
        )

        block()

        ensureActive()

        updatingManga.remove(manga)
        completed.fetchAndIncrement()
        notifier.showProgressNotification(
            updatingManga,
            completed.load(),
            mangaToUpdate.size,
        )
    }

    companion object {
        private const val TAG = "CoverUpdate"
        private const val WORK_NAME_MANUAL = "CoverUpdate"
        const val KEY_MANGA_IDS = "manga_ids"

        fun startNow(workManager: WorkManager, mangaIds: List<Long>? = null): Boolean {
            if (workManager.isRunning(TAG)) {
                return false
            }
            val data = workDataOf(
                KEY_MANGA_IDS to (mangaIds?.toLongArray() ?: LongArray(0)),
            )
            val request = OneTimeWorkRequestBuilder<CoverUpdateJob>()
                .addTag(TAG)
                .addTag(WORK_NAME_MANUAL)
                .setInputData(data)
                .build()
            workManager.enqueueUniqueWork(WORK_NAME_MANUAL, ExistingWorkPolicy.KEEP, request)
            return true
        }
    }
}
