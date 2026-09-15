package eu.kanade.presentation.history.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.manga.components.MangaCover
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CalendarMonth
import mihon.icons.materialsymbols.rounded.CollectionsBookmark
import mihon.icons.materialsymbols.rounded.KeyboardArrowRight
import mihon.icons.materialsymbols.rounded.Schedule
import java.util.Locale

@Composable
fun HistoryStatsPage(
    stats: HistoryStatsData,
    period: HistoryPeriod,
    onPeriodChange: (HistoryPeriod) -> Unit,
    onClickManga: (Long) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Large Title "History"
        Text(
            text = "History",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
        )

        // Streak Card (e.g. 1-day reading streak)
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF381216)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = HistoryIcons.Fire,
                        contentDescription = null,
                        tint = Color(0xFFFF3B5C),
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "${stats.currentStreak}-day reading streak",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Filter Header: "SHOWING THIS MONTH" with toggle icons
        var showPeriodMenu by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val periodLabel = when (period) {
                HistoryPeriod.THIS_MONTH -> "SHOWING THIS MONTH"
                HistoryPeriod.THIS_WEEK -> "SHOWING THIS WEEK"
                HistoryPeriod.ALL_TIME -> "SHOWING ALL TIME"
            }

            Text(
                text = periodLabel,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                letterSpacing = 0.6.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Box {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { showPeriodMenu = true },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.CalendarMonth,
                            contentDescription = "Period",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Icon(
                            imageVector = HistoryIcons.BookPages,
                            contentDescription = "Period",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                DropdownMenu(
                    expanded = showPeriodMenu,
                    onDismissRequest = { showPeriodMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("This Month") },
                        onClick = {
                            onPeriodChange(HistoryPeriod.THIS_MONTH)
                            showPeriodMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("This Week") },
                        onClick = {
                            onPeriodChange(HistoryPeriod.THIS_WEEK)
                            showPeriodMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("All Time") },
                        onClick = {
                            onPeriodChange(HistoryPeriod.ALL_TIME)
                            showPeriodMenu = false
                        },
                    )
                }
            }
        }

        // Metrics Row 1: TIME READ (18m) & Pages (84)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Time Read Card
            Card(
                modifier = Modifier.weight(1f).height(105.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "TIME READ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            imageVector = MaterialSymbols.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = formatTimeRead(stats.timeReadMs),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        // Mini bar graph visual in corner
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.height(20.dp),
                        ) {
                            Box(modifier = Modifier.width(3.dp).height(6.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(1.dp)))
                            Box(modifier = Modifier.width(3.dp).height(10.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(1.dp)))
                            Box(modifier = Modifier.width(3.dp).height(14.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(1.dp)))
                            Box(modifier = Modifier.width(3.dp).height(18.dp).background(Color(0xFFFF3B5C), RoundedCornerShape(1.dp)))
                        }
                    }
                }
            }

            // Pages Card
            Card(
                modifier = Modifier.weight(1f).height(105.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = HistoryIcons.BookPages,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "Pages",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Text(
                        text = "${stats.pagesRead}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Metrics Row 2: Favorite manga & Days read
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Favorite Title Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(115.dp)
                    .clickable(enabled = stats.favoriteMangaId != null) {
                        stats.favoriteMangaId?.let(onClickManga)
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = HistoryIcons.Sparkle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = "Favorite... >",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Text(
                            text = stats.favoriteMangaTitle ?: "None yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    if (stats.favoriteMangaCover != null) {
                        MangaCover.Book(
                            data = stats.favoriteMangaCover,
                            modifier = Modifier
                                .width(46.dp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(6.dp)),
                        )
                    }
                }
            }

            // Days read Card
            Card(
                modifier = Modifier.weight(1f).height(115.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "Days read",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Text(
                        text = "${stats.daysRead}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Metrics Row 3: Titles added | Avg ch/day | Longest str.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Titles added
            Card(
                modifier = Modifier.weight(1f).height(85.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.CollectionsBookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "Titles added",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "${stats.titlesAdded}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Avg ch/day
            Card(
                modifier = Modifier.weight(1f).height(85.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = HistoryIcons.Lightning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "Avg ch/day",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.1f", stats.avgChaptersPerDay),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Longest str.
            Card(
                modifier = Modifier.weight(1f).height(85.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = HistoryIcons.Fire,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "Longest str.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "${stats.longestStreak}d",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Fun facts / Trivia banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B2844)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = HistoryIcons.Sparkle,
                        contentDescription = null,
                        tint = Color(0xFF6B9BFF),
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Formatted trivia text with italicized book title
                val annotatedString = remember(stats.triviaText) {
                    val bookTitle = "Fahrenheit 451"
                    val parts = stats.triviaText.split(bookTitle)
                    buildAnnotatedString {
                        if (parts.size == 2) {
                            append(parts[0])
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)) {
                                append(bookTitle)
                            }
                            append(parts[1])
                        } else {
                            append(stats.triviaText)
                        }
                    }
                }

                Text(
                    text = annotatedString,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Trend Chart Card: Pages, this month
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Pages, this month",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${stats.totalPagesThisMonth}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Canvas Line Chart
                PagesLineChart(
                    points = stats.trendChartPoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(75.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Month start",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                    Text(
                        text = "Today",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun PagesLineChart(
    points: List<Pair<Int, Int>>,
    modifier: Modifier = Modifier,
) {
    val accentColor = Color(0xFFFF3B5C)
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingBottom = 4.dp.toPx()
        val paddingTop = 8.dp.toPx()
        val chartHeight = height - paddingTop - paddingBottom

        if (points.size < 2) {
            // Draw a subtle baseline
            drawLine(
                color = accentColor.copy(alpha = 0.4f),
                start = Offset(0f, height - paddingBottom),
                end = Offset(width, height - paddingBottom),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round,
            )
            return@Canvas
        }

        val maxVal = maxOf(points.maxOf { it.second }.toFloat(), 1f)
        val numPoints = points.size

        val offsets = points.mapIndexed { index, pair ->
            val x = (index.toFloat() / (numPoints - 1)) * width
            val y = paddingTop + (1f - (pair.second.toFloat() / maxVal)) * chartHeight
            Offset(x, y)
        }

        // Draw line path
        val path = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            for (i in 1 until offsets.size) {
                lineTo(offsets[i].x, offsets[i].y)
            }
        }

        // Fill area under path
        val fillPath = Path().apply {
            addPath(path)
            lineTo(offsets.last().x, height - paddingBottom)
            lineTo(offsets.first().x, height - paddingBottom)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.25f),
                    Color.Transparent,
                ),
                startY = paddingTop,
                endY = height,
            ),
        )

        drawPath(
            path = path,
            color = accentColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )

        // Draw active dot at the last point (Today)
        val lastPoint = offsets.last()
        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = lastPoint,
        )
        drawCircle(
            color = accentColor,
            radius = 3.dp.toPx(),
            center = lastPoint,
        )
    }
}

private fun formatTimeRead(timeMs: Long): String {
    val minutes = timeMs / 1000 / 60
    return when {
        minutes < 60 -> "${minutes}m"
        else -> {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            if (remainingMinutes > 0) "${hours}h ${remainingMinutes}m" else "${hours}h"
        }
    }
}
