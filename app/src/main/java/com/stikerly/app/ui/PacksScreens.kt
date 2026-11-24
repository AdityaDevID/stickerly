package com.stikerly.app.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.stikerly.app.data.StickerPack
import com.stikerly.app.data.StickerStore

@Composable
private fun StickerThumb(packId: String, file: String?, modifier: Modifier) {
    val image = remember(packId, file) {
        if (file == null) null
        else BitmapFactory.decodeFile(StickerStore.thumbFile(packId, file).absolutePath)?.asImageBitmap()
    }
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .clip(shape)
            .background(Brand.Mint, shape)
            .border(2.dp, Brand.Ink, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(image, contentDescription = null, modifier = Modifier.fillMaxSize())
        } else {
            Text("🙂", fontSize = 26.sp)
        }
    }
}

@Composable
fun PacksScreen(onOpen: (String) -> Unit) {
    val rev = StickerStore.revision
    val packs = remember(rev) { StickerStore.packs() }
    var showNew by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar("Paket Stiker", null)
        if (packs.isEmpty()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("📦", fontSize = 64.sp)
                Spacer(Modifier.height(8.dp))
                Text("Belum ada paket", fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(
                    "Buat stiker dari Beranda, lalu simpan ke paket. Paket butuh minimal 3 stiker supaya bisa dikirim ke WhatsApp.",
                    fontSize = 14.sp,
                    color = Brand.GreenDark,
                )
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(packs, key = { it.id }) { p ->
                    PackRow(p) { onOpen(p.id) }
                }
            }
        }
        ChunkyButton("+ Paket baru", onClick = { showNew = true }, modifier = Modifier.padding(16.dp))
    }

    if (showNew) {
        var name by remember { mutableStateOf("") }
        var animated by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showNew = false },
            containerColor = Color.White,
            title = { Text("Paket baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama paket") },
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Statis", !animated) { animated = false }
                        Chip("Animasi", animated) { animated = true }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        StickerStore.createPack(name, animated)
                        showNew = false
                    },
                ) { Text("Buat") }
            },
            dismissButton = { TextButton(onClick = { showNew = false }) { Text("Batal") } },
        )
    }
}

@Composable
private fun PackRow(p: StickerPack, onClick: () -> Unit) {
    ChunkyCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StickerThumb(p.id, p.stickers.firstOrNull()?.file, Modifier.size(64.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, fontWeight = FontWeight.Black, fontSize = 17.sp, maxLines = 1)
                Text(
                    "${p.stickers.size}/${StickerStore.MAX_STICKERS} stiker • ${if (p.animated) "animasi" else "statis"}",
                    fontSize = 13.sp,
                )
            }
            val ready = p.stickers.size >= StickerStore.MIN_STICKERS
            Box(
                Modifier
                    .background(if (ready) Brand.Green else Brand.Yellow, CircleShape)
                    .border(2.dp, Brand.Ink, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    if (ready) "Siap" else "Kurang ${StickerStore.MIN_STICKERS - p.stickers.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (ready) Color.White else Brand.Ink,
                )
            }
        }
    }
}

private fun launchAddToWhatsApp(
    ctx: Context,
    pack: StickerPack,
    launcher: (Intent) -> Unit,
) {
    val authority = "${ctx.packageName}.stickercontentprovider"
    val base = Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK").apply {
        putExtra("sticker_pack_id", pack.id)
        putExtra("sticker_pack_authority", authority)
        putExtra("sticker_pack_name", pack.name)
    }
    val pm = ctx.packageManager
    @Suppress("DEPRECATION")
    val target = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
        pm.queryIntentActivities(Intent(base).setPackage(pkg), 0).isNotEmpty()
    }
    if (target == null) {
        Toast.makeText(ctx, "WhatsApp tidak ditemukan atau versinya terlalu lama.", Toast.LENGTH_LONG).show()
        return
    }
    try {
        launcher(Intent(base).setPackage(target))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(ctx, "WhatsApp tidak bisa membuka paket ini.", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun PackDetailScreen(packId: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val rev = StickerStore.revision
    val pack = remember(rev, packId) { StickerStore.pack(packId) }
    var confirmDelete by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r: ActivityResult ->
        if (r.resultCode == Activity.RESULT_OK) {
            Toast.makeText(ctx, "Paket ditambahkan ke WhatsApp ✓", Toast.LENGTH_LONG).show()
        } else {
            val err = r.data?.getStringExtra("validation_error")
            if (err != null) Toast.makeText(ctx, "WhatsApp menolak: $err", Toast.LENGTH_LONG).show()
        }
    }

    if (pack == null) {
        Column(Modifier.fillMaxSize()) {
            TopBar("Paket tidak ditemukan", onBack)
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(pack.name, onBack)
        Text(
            "${pack.stickers.size}/${StickerStore.MAX_STICKERS} stiker • ${if (pack.animated) "animasi" else "statis"}",
            modifier = Modifier.padding(horizontal = 16.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Brand.GreenDark,
        )
        Spacer(Modifier.height(8.dp))

        if (pack.stickers.isEmpty()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("✨", fontSize = 56.sp)
                Text("Paket masih kosong. Buat stiker dari Beranda.", fontSize = 14.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(pack.stickers, key = { it.file }) { s ->
                    Box(Modifier.aspectRatio(1f)) {
                        StickerThumb(pack.id, s.file, Modifier.fillMaxSize())
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(26.dp)
                                .background(Color.White, CircleShape)
                                .border(2.dp, Brand.Ink, CircleShape)
                                .clip(CircleShape)
                                .clickable { StickerStore.removeSticker(pack.id, s.file) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✕", fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val ready = pack.stickers.size >= StickerStore.MIN_STICKERS
            if (!ready) {
                Text(
                    "Tambahkan ${StickerStore.MIN_STICKERS - pack.stickers.size} stiker lagi untuk bisa dikirim ke WhatsApp.",
                    fontSize = 13.sp,
                    color = Brand.GreenDark,
                )
            }
            ChunkyButton(
                "Kirim ke WhatsApp",
                onClick = { launchAddToWhatsApp(ctx, pack) { launcher.launch(it) } },
                enabled = ready,
            )
            Text(
                "Hapus paket",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { confirmDelete = true }
                    .padding(8.dp),
                color = Brand.Danger,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Color.White,
            title = { Text("Hapus paket?") },
            text = { Text("Paket \"${pack.name}\" dan semua stikernya akan dihapus dari aplikasi ini.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    StickerStore.deletePack(pack.id)
                    onBack()
                }) { Text("Hapus", color = Brand.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Batal") } },
        )
    }
}
