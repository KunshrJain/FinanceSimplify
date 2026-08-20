package com.example.financesimply.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financesimply.ui.theme.BudgetDanger
import com.example.financesimply.ui.theme.BudgetOver
import com.example.financesimply.ui.theme.BudgetSafe
import com.example.financesimply.ui.theme.BudgetWarning

@Composable
fun BudgetProgressBar(
    spent: Double,
    budget: Long,
    modifier: Modifier = Modifier
) {
    val progress = if (budget > 0) (spent / budget).toFloat().coerceIn(0f, 1.5f) else 0f
    val percentage = (progress * 100).toInt()

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress.coerceAtMost(1f),
            animationSpec = tween(durationMillis = 1200)
        )
    }

    val progressColor = when {
        progress >= 1.0f -> BudgetOver
        progress >= 0.8f -> BudgetDanger
        progress >= 0.6f -> BudgetWarning
        else -> BudgetSafe
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Circular progress
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    val strokeWidth = 16.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val topLeft = Offset(
                        (size.width - radius * 2) / 2,
                        (size.height - radius * 2) / 2
                    )
                    val arcSize = Size(radius * 2, radius * 2)

                    // Track
                    drawArc(
                        color = trackColor,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress
                    drawArc(
                        color = progressColor,
                        startAngle = 135f,
                        sweepAngle = 270f * animatedProgress.value,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp
                        ),
                        color = progressColor
                    )
                    Text(
                        text = "of budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Linear progress bar below
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    // Track
                    drawRoundRect(
                        color = trackColor,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                        size = Size(size.width, size.height)
                    )
                    // Fill
                    drawRoundRect(
                        color = progressColor,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                        size = Size(size.width * animatedProgress.value, size.height)
                    )
                }
            }
        }
    }
}
