package de.nyxnord.kraftlog.ui.exercises

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import de.nyxnord.kraftlog.data.local.entity.MuscleGroup

/**
 * Front/back body diagram that highlights primary (solid) and secondary (faded) muscle groups.
 * Coordinates are normalised [0,1] across the full canvas width×height.
 * Left half = front view, right half = back view.
 */
@Composable
fun MuscleDiagram(
    primaryMuscles: List<MuscleGroup>,
    secondaryMuscles: List<MuscleGroup>,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(2f)
) {
    val primaryColor   = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
    val bodyColor      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val dividerColor   = MaterialTheme.colorScheme.outlineVariant
    val labelColor     = MaterialTheme.colorScheme.onSurfaceVariant

    val effectivePrimary   = if (MuscleGroup.FULL_BODY in primaryMuscles)
        MuscleGroup.entries.toList() else primaryMuscles
    val effectiveSecondary = if (MuscleGroup.FULL_BODY in secondaryMuscles)
        MuscleGroup.entries.toList() else secondaryMuscles

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        fun colorOf(muscle: MuscleGroup): Color = when {
            muscle in effectivePrimary   -> primaryColor
            muscle in effectiveSecondary -> secondaryColor
            else                         -> bodyColor
        }

        // ── helpers ──────────────────────────────────────────────────────────
        fun box(l: Float, t: Float, r: Float, b: Float, color: Color, cr: Float = 0.018f) =
            drawRoundRect(
                color = color,
                topLeft = Offset(l * w, t * h),
                size = Size((r - l) * w, (b - t) * h),
                cornerRadius = CornerRadius(cr * minOf(w, h))
            )

        fun circle(l: Float, t: Float, r: Float, b: Float, color: Color) =
            drawOval(color = color, topLeft = Offset(l * w, t * h), size = Size((r - l) * w, (b - t) * h))

        // ── centre divider ────────────────────────────────────────────────────
        drawLine(dividerColor, Offset(w * 0.5f, h * 0.04f), Offset(w * 0.5f, h * 0.97f), strokeWidth = 1f)

        // ══════════════════════════  FRONT  ═══════════════════════════════════

        // — silhouette base (surfaceVariant fills the figure outline) —
        circle(0.190f, 0.030f, 0.310f, 0.125f, bodyColor)        // head
        box(0.228f, 0.122f, 0.272f, 0.162f, bodyColor, 0.005f)  // neck
        box(0.175f, 0.162f, 0.325f, 0.540f, bodyColor, 0.012f)  // torso
        box(0.095f, 0.162f, 0.175f, 0.440f, bodyColor, 0.014f)  // L upper-arm
        box(0.325f, 0.162f, 0.405f, 0.440f, bodyColor, 0.014f)  // R upper-arm
        box(0.098f, 0.450f, 0.172f, 0.600f, bodyColor, 0.014f)  // L forearm
        box(0.328f, 0.450f, 0.402f, 0.600f, bodyColor, 0.014f)  // R forearm
        box(0.175f, 0.540f, 0.248f, 0.970f, bodyColor, 0.012f)  // L leg
        box(0.252f, 0.540f, 0.325f, 0.970f, bodyColor, 0.012f)  // R leg

        // — shoulder bumps (widen the silhouette at shoulder line) —
        box(0.095f, 0.162f, 0.215f, 0.245f, bodyColor, 0.014f)  // L shoulder shape
        box(0.285f, 0.162f, 0.405f, 0.245f, bodyColor, 0.014f)  // R shoulder shape

        // — muscle regions (drawn on top of silhouette) —
        box(0.097f, 0.164f, 0.213f, 0.242f, colorOf(MuscleGroup.SHOULDERS)) // L shoulder
        box(0.287f, 0.164f, 0.403f, 0.242f, colorOf(MuscleGroup.SHOULDERS)) // R shoulder
        box(0.180f, 0.164f, 0.320f, 0.310f, colorOf(MuscleGroup.CHEST))     // chest
        box(0.097f, 0.164f, 0.173f, 0.435f, colorOf(MuscleGroup.BICEPS))    // L bicep
        box(0.327f, 0.164f, 0.403f, 0.435f, colorOf(MuscleGroup.BICEPS))    // R bicep
        box(0.180f, 0.310f, 0.320f, 0.535f, colorOf(MuscleGroup.CORE))      // core
        box(0.100f, 0.452f, 0.170f, 0.597f, colorOf(MuscleGroup.FOREARMS))  // L forearm
        box(0.330f, 0.452f, 0.400f, 0.597f, colorOf(MuscleGroup.FOREARMS))  // R forearm
        box(0.177f, 0.537f, 0.247f, 0.808f, colorOf(MuscleGroup.QUADRICEPS))// L quad
        box(0.253f, 0.537f, 0.323f, 0.808f, colorOf(MuscleGroup.QUADRICEPS))// R quad
        box(0.178f, 0.820f, 0.246f, 0.968f, colorOf(MuscleGroup.CALVES))    // L calf
        box(0.254f, 0.820f, 0.322f, 0.968f, colorOf(MuscleGroup.CALVES))    // R calf

        // — redraw head/neck on top so muscle regions don't bleed into face —
        circle(0.190f, 0.030f, 0.310f, 0.125f, bodyColor)
        box(0.228f, 0.120f, 0.272f, 0.164f, bodyColor, 0.005f)

        // ══════════════════════════  BACK  ════════════════════════════════════

        // — silhouette base —
        circle(0.690f, 0.030f, 0.810f, 0.125f, bodyColor)        // head
        box(0.728f, 0.122f, 0.772f, 0.162f, bodyColor, 0.005f)  // neck
        box(0.675f, 0.162f, 0.825f, 0.620f, bodyColor, 0.012f)  // torso back (incl. glutes)
        box(0.595f, 0.162f, 0.675f, 0.440f, bodyColor, 0.014f)  // L upper-arm back
        box(0.825f, 0.162f, 0.905f, 0.440f, bodyColor, 0.014f)  // R upper-arm back
        box(0.598f, 0.450f, 0.672f, 0.600f, bodyColor, 0.014f)  // L forearm back
        box(0.828f, 0.450f, 0.902f, 0.600f, bodyColor, 0.014f)  // R forearm back
        box(0.675f, 0.620f, 0.748f, 0.970f, bodyColor, 0.012f)  // L leg back
        box(0.752f, 0.620f, 0.825f, 0.970f, bodyColor, 0.012f)  // R leg back

        // — shoulder bumps —
        box(0.595f, 0.162f, 0.715f, 0.245f, bodyColor, 0.014f)  // L shoulder shape back
        box(0.785f, 0.162f, 0.905f, 0.245f, bodyColor, 0.014f)  // R shoulder shape back

        // — muscle regions —
        box(0.597f, 0.164f, 0.713f, 0.242f, colorOf(MuscleGroup.SHOULDERS)) // L shoulder back
        box(0.787f, 0.164f, 0.903f, 0.242f, colorOf(MuscleGroup.SHOULDERS)) // R shoulder back
        box(0.662f, 0.242f, 0.838f, 0.492f, colorOf(MuscleGroup.BACK))      // back (between arms)
        box(0.597f, 0.164f, 0.660f, 0.435f, colorOf(MuscleGroup.TRICEPS))   // L tricep
        box(0.840f, 0.164f, 0.903f, 0.435f, colorOf(MuscleGroup.TRICEPS))   // R tricep
        box(0.600f, 0.452f, 0.670f, 0.597f, colorOf(MuscleGroup.FOREARMS))  // L forearm back
        box(0.830f, 0.452f, 0.900f, 0.597f, colorOf(MuscleGroup.FOREARMS))  // R forearm back
        box(0.662f, 0.492f, 0.838f, 0.622f, colorOf(MuscleGroup.GLUTES))    // glutes
        box(0.677f, 0.622f, 0.746f, 0.808f, colorOf(MuscleGroup.HAMSTRINGS))// L hamstring
        box(0.754f, 0.622f, 0.823f, 0.808f, colorOf(MuscleGroup.HAMSTRINGS))// R hamstring
        box(0.678f, 0.820f, 0.745f, 0.968f, colorOf(MuscleGroup.CALVES))    // L calf back
        box(0.755f, 0.820f, 0.822f, 0.968f, colorOf(MuscleGroup.CALVES))    // R calf back

        // — redraw head/neck on top —
        circle(0.690f, 0.030f, 0.810f, 0.125f, bodyColor)
        box(0.728f, 0.120f, 0.772f, 0.164f, bodyColor, 0.005f)

        // ── "Front" / "Back" labels ───────────────────────────────────────────
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 9.sp.toPx()
            color = labelColor.copy(alpha = 0.6f).toArgb()
        }
        drawContext.canvas.nativeCanvas.drawText("Front", w * 0.25f, h * 0.985f, paint)
        drawContext.canvas.nativeCanvas.drawText("Back",  w * 0.75f, h * 0.985f, paint)
    }
}
