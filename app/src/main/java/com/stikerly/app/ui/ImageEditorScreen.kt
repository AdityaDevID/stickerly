package com.stikerly.app.ui

import android.graphics.Bitmap
import android.graphics.Paint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stikerly.app.engine.CropShape
import com.stikerly.app.engine.Render
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImageEditorScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offX by remember { mutableFloatStateOf(0f) }
    var offY by remember { mutableFloatStateOf(0f) }
    var viewSize by remember { mutableFloatStateOf(0f) }
    var shape by remember { mutableStateOf(CropShape.Rounded) }
    var showSave by remember { mutableStateOf(false) }
    val paint = remember { Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val b = withContext(Dispatchers.IO) { Render.loadBitmap(ctx, uri) }
                if (b != null) {
                    bmp = b
                    zoom = 1f
                    offX = 0f
                    offY = 0f
                } else {
                    Toast.makeText(ctx, "Gambar tidak bisa dibuka.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    val pick = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopBar("Stiker Gambar", onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val frame = RoundedCornerShape(22.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(frame)
                    .border(2.dp, Brand.Ink, frame)
                    .onSizeChanged { viewSize = it.width.toFloat() }
            ) {
                Checkerboard(Modifier.fillMaxSize())
                val b = bmp
                if (b == null) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .clickable { pick() },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("🖼️", fontSize = 56.sp)
                        Text("Ketuk untuk pilih gambar", fontWeight = FontWeight.Bold, color = Brand.GreenDark)
                    }
                } else {
                    Canvas(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(b) {
                                detectTransformGestures { _, pan, gestureZoom, _ ->
                                    zoom = (zoom * gestureZoom).coerceIn(1f, 6f)
                                    offX = Render.clampOffset(b.width, b.height, viewSize, zoom, offX + pan.x, true)
                                    offY = Render.clampOffset(b.width, b.height, viewSize, zoom, offY + pan.y, false)
                                }
                            }
                    ) {
                        val m = Render.cropMatrix(b.width, b.height, size.width, zoom, offX, offY)
                        drawIntoCanvas { it.nativeCanvas.drawBitmap(b, m, paint) }
                        val guide = Color.White
                        when (shape) {
                            CropShape.Circle -> drawCircle(
                                guide, radius = size.minDimension / 2f - 3f, style = Stroke(width = 5f),
                            )
                            CropShape.Rounded -> drawRoundRect(
                                guide,
                                topLeft = Offset(3f, 3f),
                                size = androidx.compose.ui.geometry.Size(size.width - 6f, size.height - 6f),
                                cornerRadius = CornerRadius(size.width * 0.1875f),
                                style = Stroke(width = 5f),
                            )
                            CropShape.Square -> Unit
                        }
                    }
                }
            }

            if (bmp != null) {
                Text("Geser dan cubit untuk mengatur posisi.", fontSize = 13.sp, color = Brand.GreenDark)
            }

            SectionTitle("Bentuk")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Kotak", shape == CropShape.Square) { shape = CropShape.Square }
                Chip("Membulat", shape == CropShape.Rounded) { shape = CropShape.Rounded }
                Chip("Lingkaran", shape == CropShape.Circle) { shape = CropShape.Circle }
            }

            ChunkyButton(
                if (bmp == null) "Pilih Gambar" else "Ganti Gambar",
                onClick = { pick() },
                container = Color.White,
                contentColor = Brand.Ink,
            )
            ChunkyButton("Simpan ke Paket", onClick = { showSave = true }, enabled = bmp != null)
            Box(Modifier.size(12.dp))
        }
    }

    if (showSave) {
        SaveToPackDialog(
            animated = false,
            onDismiss = { showSave = false },
            onConfirm = { id, name, emoji ->
                val b = bmp
                if (b != null && viewSize > 0f) {
                    val out = Render.renderCrop(b, viewSize, zoom, offX, offY, shape)
                    val webp = Render.staticWebp(out)
                    if (webp == null) {
                        Toast.makeText(ctx, "Gagal membuat stiker.", Toast.LENGTH_LONG).show()
                    } else {
                        val err = SaveHelper.save(ctx, webp, out, false, id, name, emoji)
                        Toast.makeText(ctx, err ?: "Tersimpan ke paket ✓", Toast.LENGTH_LONG).show()
                        if (err == null) showSave = false
                    }
                }
            },
        )
    }
}
