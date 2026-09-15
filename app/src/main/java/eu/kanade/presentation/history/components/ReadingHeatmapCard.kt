package eu.kanade.presentation.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
fun ReadingHeatmapCard(
    activityByDate: Map<LocalDate, Long>,
    modifier: Modifier = Modifier,
) {
    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    // Prepare grid of 7 rows x 16 columns (weeks)
    val numWeeks = 16
    val daysData = remember(activityByDate, today) {
        val todayIsoDay = today.dayOfWeek.isoDayNumber // 1..7 (Mon..Sun)
        val startOfCurrentWeek = today.minus(todayIsoDay - 1, DateTimeUnit.DAY)
        val startDate = startOfCurrentWeek.minus(numWeeks - 1, DateTimeUnit.WEEK)

        // Generate columns of weeks
        (0 until numWeeks).map { weekIndex ->
            val weekStart = startDate.plus(weekIndex, DateTimeUnit.WEEK)
            (0 until 7).map { dayOffset ->
                val date = weekStart.plus(dayOffset, DateTimeUnit.DAY)
                val durationMs = activityByDate[date] ?: 0L
                val isFuture = date > today
                HeatmapCell(date = date, durationMs = durationMs, isFuture = isFuture)
            }
        }
    }

    val scrollState = rememberScrollState()

    // Auto-scroll to the end (today)
    LaunchedEffect(Unit) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Day of week labels (M, W, F)
                Column(
                    modifier = Modifier.padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val labelStyle = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Text("M", style = labelStyle, modifier = Modifier.height(11.dp))
                    Box(modifier = Modifier.size(11.dp)) // Tue placeholder
                    Text("W", style = labelStyle, modifier = Modifier.height(11.dp))
                    Box(modifier = Modifier.size(11.dp)) // Thu placeholder
                    Text("F", style = labelStyle, modifier = Modifier.height(11.dp))
                    Box(modifier = Modifier.size(11.dp)) // Sat placeholder
                    Box(modifier = Modifier.size(11.dp)) // Sun placeholder
                }

                // Heatmap Grid
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    daysData.forEach { week ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            week.forEach { cell ->
                                val cellColor = when {
                                    cell.isFuture -> Color.Transparent
                                    cell.durationMs == 0L -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f)
                                    cell.durationMs < 300_000L -> Color(0xFF6E1825) // < 5m
                                    cell.durationMs < 900_000L -> Color(0xFFB3243B) // 5m - 15m
                                    cell.durationMs < 1_800_000L -> Color(0xFFE5394B) // 15m - 30m
                                    else -> Color(0xFFFF4D67) // > 30m
                                }

                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(RoundedCornerShape(2.5.dp))
                                        .background(cellColor),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend at bottom right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val legendColors = listOf(
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f),
                    Color(0xFF6E1825),
                    Color(0xFFB3243B),
                    Color(0xFFE5394B),
                    Color(0xFFFF4D67),
                )
                legendColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(9.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color),
                    )
                }
            }
        }
    }
}

private data class HeatmapCell(
    val date: LocalDate,
    val durationMs: Long,
    val isFuture: Boolean,
)
