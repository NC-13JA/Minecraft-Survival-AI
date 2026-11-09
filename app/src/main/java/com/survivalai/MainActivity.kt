package com.survivalai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MiniMapApp() }
    }
}

@Composable
private fun MiniMapApp() {
    var zoom by remember { mutableFloatStateOf(1f) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF80D66B), background = Color(0xFF101A18), surface = Color(0xFF1B2925))) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF101A18)) {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("WORLD MAP", color = Color(0xFF91D77B), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("خريطة العالم المصغّرة", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Surface(color = Color(0xFF493D20), shape = RoundedCornerShape(20.dp)) {
                        Text("عرض تجريبي", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = Color(0xFFFFD778), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFF263B32))) {
                    Canvas(Modifier.fillMaxSize()) {
                        val tile = 23f * zoom
                        val columns = (size.width / tile).toInt() + 3
                        val rows = (size.height / tile).toInt() + 3
                        val startX = -((columns * tile - size.width) / 2f)
                        val startY = -((rows * tile - size.height) / 2f)
                        for (row in 0 until rows) {
                            for (column in 0 until columns) {
                                val worldX = column - columns / 2
                                val worldZ = row - rows / 2
                                drawRect(terrainColor(worldX, worldZ), Offset(startX + column * tile, startY + row * tile), Size(tile + 1f, tile + 1f))
                            }
                        }
                        val gridColor = Color(0x220B1711)
                        for (x in 0..columns) {
                            val px = startX + x * tile
                            drawLine(gridColor, Offset(px, 0f), Offset(px, size.height), 1f)
                        }
                        for (y in 0..rows) {
                            val py = startY + y * tile
                            drawLine(gridColor, Offset(0f, py), Offset(size.width, py), 1f)
                        }
                        val center = Offset(size.width / 2f, size.height / 2f)
                        drawCircle(Color(0x55000000), 17f, center + Offset(0f, 2f))
                        drawCircle(Color.White, 11f, center)
                        drawCircle(Color(0xFF35B8FF), 7f, center)
                        drawCircle(Color.White, 2.5f, center)
                    }
                    Surface(Modifier.align(Alignment.TopStart).padding(12.dp), color = Color(0xCC14231D), shape = RoundedCornerShape(12.dp)) {
                        Text("N  ↑", Modifier.padding(horizontal = 11.dp, vertical = 8.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Surface(Modifier.align(Alignment.BottomStart).padding(12.dp), color = Color(0xCC14231D), shape = RoundedCornerShape(12.dp)) {
                        Text("● موقع اللاعب التجريبي", Modifier.padding(horizontal = 10.dp, vertical = 8.dp), color = Color.White, fontSize = 11.sp)
                    }
                    Column(Modifier.align(Alignment.BottomEnd).padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        MapControl("+") { zoom = (zoom + 0.15f).coerceAtMost(2.2f) }
                        MapControl("−") { zoom = (zoom - 0.15f).coerceAtLeast(0.65f) }
                        MapControl("↺") { zoom = 1f }
                    }
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("X: —     Z: —", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("بانتظار ربط بيانات عالم Minecraft", color = Color(0xFF9BB2A6), fontSize = 11.sp)
                    }
                    Text("مستوى التكبير: " + String.format("%.1f", zoom), color = Color(0xFF91D77B), fontWeight = FontWeight.Bold)
                }
                Text("هذه خريطة واجهة تجريبية وليست خريطة عالمك الحقيقية بعد.", color = Color(0xFF71877C), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun MapControl(label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.size(44.dp), color = Color(0xE6192922), shape = CircleShape) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun terrainColor(x: Int, z: Int): Color {
    val value = sin(x * 0.19) * 0.42 + cos(z * 0.16) * 0.34 + sin((x + z) * 0.075) * 0.24
    return when {
        value < -0.48 -> Color(0xFF285C70)
        value < -0.28 -> Color(0xFFC5B77A)
        value > 0.62 -> Color(0xFF777C69)
        value > 0.30 -> Color(0xFF356044)
        else -> Color(0xFF64864B)
    }
}
