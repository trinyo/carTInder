package hu.zoltanegyhazi.cartinder.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import hu.zoltanegyhazi.cartinder.data.BodyType
import hu.zoltanegyhazi.cartinder.data.Car

/** Az alak arányai a vászon szélességének / magasságának törtrészében. */
private data class Silhouette(
    val bodyStart: Float,
    val bodyEnd: Float,
    val belt: Float,
    val roofY: Float,
    val cabinRear: Float,
    val roofStart: Float,
    val roofEnd: Float,
    val cabinFront: Float,
    val wheelScale: Float = 1f,
)

private fun BodyType.silhouette() = when (this) {
    BodyType.SEDAN -> Silhouette(0.05f, 0.95f, 0.52f, 0.26f, 0.20f, 0.34f, 0.58f, 0.74f)
    BodyType.HATCHBACK -> Silhouette(0.10f, 0.90f, 0.52f, 0.24f, 0.13f, 0.22f, 0.55f, 0.72f)
    BodyType.KOMBI -> Silhouette(0.05f, 0.95f, 0.52f, 0.25f, 0.07f, 0.12f, 0.58f, 0.74f)
    BodyType.SUV -> Silhouette(0.06f, 0.94f, 0.46f, 0.14f, 0.09f, 0.14f, 0.62f, 0.77f, 1.12f)
    BodyType.COUPE -> Silhouette(0.05f, 0.95f, 0.56f, 0.32f, 0.17f, 0.37f, 0.54f, 0.73f)
}

@Composable
fun CarArt(car: Car, modifier: Modifier = Modifier) {
    val bodyColor = Color(car.color)
    val s = car.body.silhouette()
    Canvas(modifier.aspectRatio(1.9f)) {
        drawCar(s, bodyColor)
    }
}

private fun DrawScope.drawCar(s: Silhouette, bodyColor: Color) {
    val w = size.width
    val h = size.height
    val bottom = 0.78f * h
    val wheelR = 0.085f * w * s.wheelScale
    val dark = lerp(bodyColor, Color.Black, 0.35f)
    val light = lerp(bodyColor, Color.White, 0.35f)

    // Árnyék
    drawOval(
        color = Color.Black.copy(alpha = 0.18f),
        topLeft = Offset(0.06f * w, bottom + wheelR - 0.035f * h),
        size = Size(0.88f * w, 0.07f * h),
    )

    // Utastér
    val cabin = Path().apply {
        moveTo(s.cabinRear * w, s.belt * h + 2f)
        lineTo(s.roofStart * w, s.roofY * h)
        lineTo(s.roofEnd * w, s.roofY * h)
        lineTo(s.cabinFront * w, s.belt * h + 2f)
        close()
    }
    drawPath(cabin, bodyColor)
    drawPath(cabin, bodyColor, style = Stroke(width = 0.03f * h, join = StrokeJoin.Round))

    // Ablakok: az utastér a középpontja felé zsugorítva
    val cx = (s.cabinRear + s.cabinFront) / 2 * w
    val cy = (s.belt + s.roofY) / 2 * h
    fun shrink(x: Float, y: Float) = Offset(cx + (x * w - cx) * 0.86f, cy + (y * h - cy) * 0.80f + 0.015f * h)
    val glass = Path().apply {
        val a = shrink(s.cabinRear, s.belt)
        val b = shrink(s.roofStart, s.roofY)
        val c = shrink(s.roofEnd, s.roofY)
        val d = shrink(s.cabinFront, s.belt)
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
    }
    drawPath(
        glass,
        Brush.linearGradient(
            listOf(Color(0xFFDDF1FF), Color(0xFF6E8CA8)),
            start = Offset(cx, s.roofY * h),
            end = Offset(cx + 0.1f * w, s.belt * h),
        ),
    )
    val pillarX = (s.roofStart + s.roofEnd) / 2 * w
    drawRect(bodyColor, Offset(pillarX - 0.012f * w, s.roofY * h), Size(0.024f * w, (s.belt - s.roofY) * h))

    // Karosszéria
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(light, bodyColor, dark), startY = s.belt * h, endY = bottom),
        topLeft = Offset(s.bodyStart * w, s.belt * h),
        size = Size((s.bodyEnd - s.bodyStart) * w, bottom - s.belt * h),
        cornerRadius = CornerRadius(0.07f * h),
    )
    // Övvonal csík és ajtórés
    drawLine(light, Offset(s.bodyStart * w + 0.03f * w, s.belt * h + 0.04f * h), Offset(s.bodyEnd * w - 0.03f * w, s.belt * h + 0.04f * h), strokeWidth = 0.012f * h)
    drawLine(dark, Offset(pillarX, s.belt * h + 0.02f * h), Offset(pillarX, bottom - 0.06f * h), strokeWidth = 0.008f * h)
    drawRoundRect(dark, Offset(pillarX + 0.03f * w, s.belt * h + 0.08f * h), Size(0.05f * w, 0.025f * h), CornerRadius(0.012f * h))
    drawRoundRect(dark, Offset(pillarX - 0.14f * w, s.belt * h + 0.08f * h), Size(0.05f * w, 0.025f * h), CornerRadius(0.012f * h))

    // Lámpák
    drawRoundRect(Color(0xFFFFF59D), Offset(s.bodyEnd * w - 0.045f * w, s.belt * h + 0.06f * h), Size(0.04f * w, 0.06f * h), CornerRadius(0.02f * h))
    drawRoundRect(Color(0xFFE53935), Offset(s.bodyStart * w + 0.005f * w, s.belt * h + 0.06f * h), Size(0.03f * w, 0.07f * h), CornerRadius(0.015f * h))

    // Kerekek
    val wheelInset = 0.17f * w
    listOf(s.bodyStart * w + wheelInset, s.bodyEnd * w - wheelInset).forEach { x ->
        val c = Offset(x, bottom)
        drawCircle(lerp(bodyColor, Color.Black, 0.6f), wheelR * 1.12f, c)
        drawCircle(Color(0xFF212121), wheelR, c)
        drawCircle(Color(0xFFBDBDBD), wheelR * 0.56f, c)
        drawCircle(Color(0xFF757575), wheelR * 0.18f, c)
    }
}
