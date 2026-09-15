package eu.kanade.presentation.history.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object HistoryIcons {
    val Fire: ImageVector by lazy {
        ImageVector.Builder(
            name = "Fire",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(12f, 23f)
                curveTo(7.03f, 23f, 3f, 18.97f, 3f, 14f)
                curveTo(3f, 9.87f, 5.79f, 6.4f, 9.6f, 5.31f)
                lineTo(10.5f, 5.06f)
                lineTo(10.22f, 5.96f)
                curveTo(9.74f, 7.48f, 9.77f, 8.86f, 10.3f, 9.84f)
                curveTo(10.74f, 10.65f, 11.53f, 11.23f, 12.5f, 11.48f)
                lineTo(13.5f, 11.74f)
                lineTo(13.12f, 10.79f)
                curveTo(12.2f, 8.48f, 12.55f, 5.76f, 14.07f, 3.73f)
                lineTo(14.7f, 2.9f)
                lineTo(15.11f, 3.86f)
                curveTo(17.48f, 9.38f, 21f, 11.08f, 21f, 14f)
                curveTo(21f, 18.97f, 16.97f, 23f, 12f, 23f)
                close()
                moveTo(12f, 18.5f)
                curveTo(13.93f, 18.5f, 15.5f, 16.93f, 15.5f, 15f)
                curveTo(15.5f, 13.62f, 14.7f, 12.33f, 13.5f, 11.75f)
                curveTo(12.7f, 12.87f, 11.5f, 13.5f, 10.19f, 13.5f)
                curveTo(9.53f, 13.5f, 8.89f, 13.34f, 8.5f, 13.06f)
                curveTo(8.5f, 16.07f, 10.07f, 18.5f, 12f, 18.5f)
                close()
            }
        }.build()
    }

    val Sparkle: ImageVector by lazy {
        ImageVector.Builder(
            name = "Sparkle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2f)
                curveTo(12f, 7.52f, 16.48f, 12f, 22f, 12f)
                curveTo(16.48f, 12f, 12f, 16.48f, 12f, 22f)
                curveTo(12f, 16.48f, 7.52f, 12f, 2f, 12f)
                curveTo(7.52f, 12f, 12f, 7.52f, 12f, 2f)
                close()
            }
        }.build()
    }

    val Lightning: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lightning",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(7f, 2f)
                verticalLineTo(13f)
                horizontalLineTo(10f)
                verticalLineTo(22f)
                lineTo(17f, 10f)
                horizontalLineTo(13f)
                lineTo(16f, 2f)
                horizontalLineTo(7f)
                close()
            }
        }.build()
    }

    val BookPages: ImageVector by lazy {
        ImageVector.Builder(
            name = "BookPages",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(21f, 5f)
                curveToRelative(-1.11f, -0.35f, -2.33f, -0.5f, -3.5f, -0.5f)
                curveToRelative(-1.95f, 0f, -4.05f, 0.4f, -5.5f, 1.5f)
                curveToRelative(-1.45f, -1.1f, -3.55f, -1.5f, -5.5f, -1.5f)
                curveToRelative(-1.17f, 0f, -2.39f, 0.15f, -3.5f, 0.5f)
                curveTo(2.42f, 5.17f, 2f, 5.75f, 2f, 6.44f)
                verticalLineTo(19.5f)
                curveTo(2f, 20.33f, 2.67f, 21f, 3.5f, 21f)
                curveToRelative(0.18f, 0f, 0.35f, -0.03f, 0.5f, -0.08f)
                curveTo(5.11f, 20.57f, 6.33f, 20.42f, 7.5f, 20.42f)
                curveToRelative(1.95f, 0f, 4.05f, 0.4f, 5.5f, 1.5f)
                curveToRelative(1.35f, -0.85f, 3.8f, -1.5f, 5.5f, -1.5f)
                curveToRelative(1.28f, 0f, 2.45f, 0.17f, 3.5f, 0.5f)
                curveToRelative(0.17f, 0.05f, 0.33f, 0.08f, 0.5f, 0.08f)
                curveToRelative(0.83f, 0f, 1.5f, -0.67f, 1.5f, -1.5f)
                verticalLineTo(6.44f)
                curveToRelative(0f, -0.69f, -0.42f, -1.27f, -1f, -1.44f)
                close()
                moveTo(19f, 18.5f)
                curveToRelative(-1f, -0.3f, -2.19f, -0.5f, -3.5f, -0.5f)
                curveToRelative(-1.45f, 0f, -3.55f, 0.55f, -4.5f, 1.5f)
                verticalLineTo(7.5f)
                curveToRelative(0.95f, -0.95f, 3.05f, -1.5f, 4.5f, -1.5f)
                curveToRelative(1.31f, 0f, 2.5f, 0.2f, 3.5f, 0.5f)
                verticalLineTo(18.5f)
                close()
            }
        }.build()
    }
}
