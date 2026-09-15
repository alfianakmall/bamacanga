package eu.kanade.presentation.history.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.history.HistoryUiModel
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.util.formatChapterNumber

@Composable
fun ReadingTimelineCard(
    history: HistoryUiModel.Item,
    onClickCover: () -> Unit,
    onClickResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subtitle = remember(history.chapters, history.totalDuration) {
        formatHistorySubtitle(history.chapters, history.totalDuration)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClickResume)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MangaCover.Book(
                modifier = Modifier
                    .width(46.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
                data = history.item.coverData,
                onClick = onClickCover,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 8.dp),
            ) {
                Text(
                    text = history.item.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

private fun formatHistorySubtitle(chapters: List<Double>, durationMs: Long): String {
    val chapterText = when {
        chapters.isEmpty() -> ""
        chapters.size == 1 -> {
            val num = chapters.first()
            if (num >= 0) "Chapter ${formatChapterNumber(num)}" else ""
        }
        else -> {
            "Chapters ${chapters.joinToString(", ") { formatChapterNumber(it) }}"
        }
    }

    val minutes = durationMs / 1000 / 60
    val durationText = when {
        minutes > 0 -> "${minutes}m read"
        durationMs > 10_000 -> "1m read"
        else -> null
    }

    return when {
        chapterText.isNotEmpty() && durationText != null -> "$chapterText · $durationText"
        chapterText.isNotEmpty() -> chapterText
        durationText != null -> durationText
        else -> ""
    }
}
