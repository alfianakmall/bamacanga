package eu.kanade.domain.manga.interactor

import android.content.Context
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import dev.zacsweers.metro.Inject
import eu.kanade.domain.manga.model.toSManga
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.source.service.SourceManager
import kotlin.time.Clock

@Inject
class RefreshMangaCover(
    private val context: Context,
    private val sourceManager: SourceManager,
    private val mangaRepository: MangaRepository,
    private val coverCache: CoverCache,
) {
    suspend operator fun invoke(manga: Manga): Result<Manga> = withIOContext {
        try {
            val source = sourceManager.get(manga.source)
                ?: return@withIOContext Result.failure(IllegalStateException("Source not installed"))

            var freshThumbnailUrl: String? = null

            // 1. Fetch details from source
            try {
                val update = source.getMangaUpdate(
                    manga = manga.toSManga(),
                    chapters = emptyList(),
                    fetchDetails = true,
                    fetchChapters = false,
                )
                val url = update.manga.thumbnail_url
                if (!url.isNullOrBlank()) {
                    freshThumbnailUrl = url
                }
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Failed to get manga details for cover refresh: ${manga.title}" }
            }

            // 2. Fallback: If details didn't have thumbnail, search source catalogue by title
            if (freshThumbnailUrl.isNullOrBlank() && source is CatalogueSource) {
                try {
                    val searchResult = source.getSearchManga(1, manga.title, FilterList())
                    val match = searchResult.mangas.firstOrNull { it.url == manga.url }
                        ?: searchResult.mangas.firstOrNull { it.title.equals(manga.title, ignoreCase = true) }
                    if (!match?.thumbnail_url.isNullOrBlank()) {
                        freshThumbnailUrl = match.thumbnail_url
                    }
                } catch (e: Exception) {
                    logcat(LogPriority.WARN, e) { "Failed to search catalogue for cover refresh: ${manga.title}" }
                }
            }

            val targetThumbnailUrl = freshThumbnailUrl?.takeIf { it.isNotBlank() } ?: manga.thumbnailUrl
                ?: return@withIOContext Result.failure(IllegalStateException("No cover URL available"))

            // 3. Invalidate old cache
            coverCache.deleteFromCache(manga, deleteCustomCover = false)
            manga.thumbnailUrl?.let { context.imageLoader.diskCache?.remove(it) }
            context.imageLoader.diskCache?.remove(targetThumbnailUrl)
            context.imageLoader.memoryCache?.clear()

            // 4. Update database
            val now = Clock.System.now().toEpochMilliseconds()
            mangaRepository.update(
                MangaUpdate(
                    id = manga.id,
                    thumbnailUrl = targetThumbnailUrl,
                    coverLastModified = now,
                ),
            )

            val updatedManga = mangaRepository.getMangaById(manga.id)

            // 5. Preload into cache
            try {
                val request = ImageRequest.Builder(context)
                    .data(updatedManga)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.WRITE_ONLY)
                    .memoryCachePolicy(CachePolicy.WRITE_ONLY)
                    .build()
                context.imageLoader.execute(request)
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Failed to preload cover image for ${manga.title}" }
            }

            Result.success(updatedManga)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Error refreshing cover for ${manga.title}" }
            Result.failure(e)
        }
    }
}
