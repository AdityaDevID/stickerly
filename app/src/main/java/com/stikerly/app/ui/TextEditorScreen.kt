package com.stikerly.app.ui

import android.graphics.Typeface
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stikerly.app.engine.Render

@Composable
fun TextEditorScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("Halo!") }
    val fonts = remember {
        listOf(
            "Tebal" to Typeface.create("sans-serif-black", Typeface.NORMAL),
            "Santai" to Typeface.create("cursive", Typeface.BOLD),
            "Klasik" to Typeface.create(Typeface.SERIF, Typeface.BOLD),
            "Kode" to Typeface.create(Typeface.MONOSPACE, Typeface.BOLD),
        )
    }
    val palette = remember {
        listOf(
            Color.White, Color(0xFF0B1A10), Color(0xFF02E560), Color(0xFFFFC21A),
            Color(0xFFFF4D6D), Color(0xFF3B82F6), Color(0xFFA855F7), Color(0xFFFF8A00),
        )
    }
    var fontIdx by remember { mutableIntStateOf(0) }
    var colorIdx by remember { mutableIntStateOf(0) }
    var outline by remember { mutableStateOf(true) }
    var scale by remember { mutableFloatStateOf(1f) }
    var showSave by remember { mutableStateOf(false) }

    val fill = palette[colorIdx]
    val outlineColor = if (fill.luminance() > 0.6f) Brand.Ink else Color.White
    val bmp = remember(text, fontIdx, colorIdx, outline, scale) {
        Render.renderText(text, fill.toArgb(), outlineColor.toArgb(), outline, fonts[fontIdx].second, scale)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopBar("Stiker Teks", onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val shape = RoundedCornerShape(22.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(shape)
                    .border(2.dp, Brand.Ink, shape)
            ) {
                Checkerboard(Modifier.fillMaxSize())
                Image(bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(120) },
                label = { Text("Tulis teks") },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("Gaya huruf")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fonts.forEachIndexed { i, f ->
                    Chip(f.first, selected = i == fontIdx, onClick = { fontIdx = i })
                }
            }

            SectionTitle("Warna teks")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                palette.forEachIndexed { i, c ->
                    Box(
                        Modifier
                            .size(38.dp)
                            .background(c, CircleShape)
                            .border(if (i == colorIdx) 4.dp else 2.dp, Brand.Ink, CircleShape)
                            .clip(CircleShape)
                            .clickable { colorIdx = i }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Switch(checked = outline, onCheckedChange = { outline = it })
                Text("Outline tebal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            SectionTitle("Ukuran")
            Slider(value = scale, onValueChange = { scale = it }, valueRange = 0.5f..1.4f)

            ChunkyButton("Simpan ke Paket", onClick = { showSave = true }, enabled = text.isNotBlank())
            Box(Modifier.size(12.dp))
        }
    }

    if (showSave) {
        SaveToPackDialog(
            animated = false,
            onDismiss = { showSave = false },
            onConfirm = { id, name, emoji ->
                val webp = Render.staticWebp(bmp)
                if (webp == null) {
                    Toast.makeText(ctx, "Gagal membuat stiker.", Toast.LENGTH_LONG).show()
                } else {
                    val err = SaveHelper.save(ctx, webp, bmp, false, id, name, emoji)
                    Toast.makeText(ctx, err ?: "Tersimpan ke paket ✓", Toast.LENGTH_LONG).show()
                    if (err == null) showSave = false
                }
            },
        )
    }
}
