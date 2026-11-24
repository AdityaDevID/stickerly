package com.stikerly.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.stikerly.app.engine.PipelineResult
import com.stikerly.app.engine.VideoInfo
import com.stikerly.app.engine.VideoPipeline
import com.stikerly.app.engine.VideoTools
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_SECONDS = 10f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(initialSound: Boolean, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var uri by remember { mutableStateOf<Uri?>(null) }
    var info by remember { mutableStateOf<VideoInfo?>(null) }
    var range by remember { mutableStateOf(0f..1f) }
    var fit by remember { mutableStateOf(false) }
    var sound by remember { mutableStateOf(initialSound) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var label by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<PipelineResult?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    var showSave by remember { mutableStateOf(false) }

    val durationSec = (info?.durationMs ?: 0L) / 1000f

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
        if (picked != null) {
            scope.launch {
                val i = withContext(Dispatchers.IO) { VideoTools.probe(ctx, picked) }
                if (i == null) {
                    Toast.makeText(ctx, "Video tidak bisa dibuka.", Toast.LENGTH_LONG).show()
                } else {
                    uri = picked
                    info = i
                    result = null
                    val d = i.durationMs / 1000f
                    range = 0f..minOf(d, 6f)
                }
            }
        }
    }

    LaunchedEffect(uri, range.start) {
        val u = uri
        if (u != null) {
            delay(200)
            preview = withContext(Dispatchers.IO) { VideoTools.frameAt(ctx, u, (range.start * 1000f).toLong()) }
        }
    }

    fun generate() {
        val u = uri ?: return
        busy = true
        progress = 0f
        label = "Memulai…"
        result = null
        val startMs = (range.start * 1000f).toLong()
        val endMs = (range.endInclusive * 1000f).toLong()
        job = scope.launch {
            try {
                val res = withContext(Dispatchers.Default) {
                    VideoPipeline.run(ctx, u, startMs, endMs, fit, sound) { p, l ->
                        progress = p
                        label = l
                    }
                }
                result = res
                if (res.error != null) Toast.makeText(ctx, res.error, Toast.LENGTH_LONG).show()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(ctx, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                busy = false
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopBar(if (initialSound) "Stiker + Suara" else "Stiker Video", onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val frame = RoundedCornerShape(22.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(frame)
                    .border(2.dp, Brand.Ink, frame)
            ) {
                Checkerboard(Modifier.fillMaxSize())
                val pv = preview
                if (uri == null || pv == null) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .clickable(enabled = !busy) {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("🎬", fontSize = 56.sp)
                        Text("Ketuk untuk pilih video", fontWeight = FontWeight.Bold, color = Brand.GreenDark)
                    }
                } else {
                    Image(
                        pv.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = if (fit) androidx.compose.ui.layout.ContentScale.Fit
                        else androidx.compose.ui.layout.ContentScale.Crop,
                    )
                }
            }

            if (uri != null && durationSec > 0f) {
                SectionTitle("Potong durasi (maks. 10 detik)")
                RangeSlider(
                    value = range,
                    onValueChange = { r ->
                        var s = r.start
                        var e = r.endInclusive
                        if (e - s > MAX_SECONDS) {
                            if (s != range.start) e = s + MAX_SECONDS else s = e - MAX_SECONDS
                        }
                        if (e - s >= 0.5f) range = s..e
                    },
                    valueRange = 0f..durationSec,
                    enabled = !busy,
                )
                Text(
                    "%.1f dtk — %.1f dtk  (panjang %.1f dtk)".format(range.start, range.endInclusive, range.endInclusive - range.start),
                    fontSize = 13.sp,
                    color = Brand.GreenDark,
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Switch(checked = fit, onCheckedChange = { fit = it }, enabled = !busy)
                    Text("Muat semua (tanpa potong sisi)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Switch(checked = sound, onCheckedChange = { sound = it }, enabled = !busy)
                    Column {
                        Text("Sertakan video bersuara", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (info?.hasAudio == false) {
                            Text("Video ini tidak punya suara.", fontSize = 12.sp, color = Brand.Danger)
                        }
                    }
                }
                Text(
                    "Stiker WhatsApp tidak menyimpan suara di dalam file. Suara diekspor sebagai video MP4 terpisah.",
                    fontSize = 12.sp,
                    color = Brand.GreenDark,
                )
            }

            if (busy) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                ChunkyButton(
                    "Batal",
                    onClick = {
                        job?.cancel()
                        busy = false
                    },
                    container = Color.White,
                    contentColor = Brand.Ink,
                )
            } else {
                ChunkyButton(
                    if (uri == null) "Pilih Video" else "Buat Stiker",
                    onClick = {
                        if (uri == null) {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        } else {
                            generate()
                        }
                    },
                )
                if (uri != null) {
                    ChunkyButton(
                        "Ganti Video",
                        onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        },
                        container = Color.White,
                        contentColor = Brand.Ink,
                    )
                }
            }

            val res = result
            if (res != null && res.error == null && res.webp != null && res.thumb != null) {
                ChunkyCard(Modifier.fillMaxWidth(), container = Brand.Mint) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Stiker siap ✓", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text(
                            "Animasi • ${res.frames} frame • ${res.fps} fps • ${res.webp.size / 1024} KB",
                            fontSize = 14.sp,
                        )
                        ChunkyButton("Simpan ke Paket", onClick = { showSave = true })
                        val mp4 = res.mp4
                        if (mp4 != null) {
                            ChunkyButton(
                                "Bagikan Video Bersuara",
                                onClick = {
                                    try {
                                        val shared = FileProvider.getUriForFile(
                                            ctx, "${ctx.packageName}.fileprovider", mp4,
                                        )
                                        val send = Intent(Intent.ACTION_SEND).apply {
                                            type = "video/mp4"
                                            putExtra(Intent.EXTRA_STREAM, shared)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        ctx.startActivity(Intent.createChooser(send, "Bagikan video"))
                                    } catch (e: Exception) {
                                        Toast.makeText(ctx, "Tidak bisa membagikan video.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                container = Brand.Yellow,
                                contentColor = Brand.Ink,
                            )
                        } else if (sound) {
                            Text(
                                "Video bersuara tidak bisa dibuat dari file ini.",
                                fontSize = 12.sp,
                                color = Brand.Danger,
                            )
                        }
                    }
                }
            }
            Box(Modifier.size(12.dp))
        }
    }

    val res = result
    if (showSave && res != null && res.webp != null && res.thumb != null) {
        SaveToPackDialog(
            animated = true,
            onDismiss = { showSave = false },
            onConfirm = { id, name, emoji ->
                val err = SaveHelper.save(ctx, res.webp, res.thumb, true, id, name, emoji)
                Toast.makeText(ctx, err ?: "Tersimpan ke paket ✓", Toast.LENGTH_LONG).show()
                if (err == null) showSave = false
            },
        )
    }
}
