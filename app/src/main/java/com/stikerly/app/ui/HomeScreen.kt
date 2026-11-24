package com.stikerly.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stikerly.app.R
import com.stikerly.app.data.StickerStore

@Composable
fun HomeScreen(onOpen: (Route) -> Unit) {
    var showAbout by remember { mutableStateOf(false) }
    val rev = StickerStore.revision
    val packCount = remember(rev) { StickerStore.packs().size }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Hero(onClick = { showAbout = true })

        Text("Mau bikin apa hari ini?", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Brand.Ink)

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Tile(Modifier.weight(1f), "Aa", "Teks", "Tulis kata jadi stiker") { onOpen(Route.Text) }
            Tile(Modifier.weight(1f), "🖼️", "Gambar", "Potong dari galeri") { onOpen(Route.Image) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Tile(Modifier.weight(1f), "🎬", "Video", "Stiker bergerak") { onOpen(Route.Video(false)) }
            Tile(Modifier.weight(1f), "🔊", "Suara", "Video + suara", badge = "BARU") { onOpen(Route.Video(true)) }
        }

        ChunkyCard(Modifier.fillMaxWidth(), container = Brand.Mint, onClick = { onOpen(Route.Packs) }) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("📦", fontSize = 32.sp)
                Column(Modifier.weight(1f)) {
                    Text("Paket stikermu", fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text(
                        if (packCount == 0) "Simpan minimal 3 stiker, lalu kirim ke WhatsApp."
                        else "$packCount paket tersimpan. Ketuk untuk kirim ke WhatsApp.",
                        fontSize = 13.sp,
                    )
                }
                Text("→", fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            containerColor = Color.White,
            title = { Text("Stickerly") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(
                        painterResource(R.drawable.banner),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.FillWidth,
                    )
                    Text("Generator stiker WhatsApp: teks, gambar, dan video bergerak.", fontSize = 14.sp)
                }
            },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Tutup") } },
        )
    }
}

@Composable
private fun Hero(onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp)) {
        Box(
            Modifier
                .matchParentSize()
                .offset(4.dp, 4.dp)
                .background(Brand.Ink, shape)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Brand.GreenBright, Brand.Green, Brand.GreenDeep)),
                    shape,
                )
                .border(2.dp, Brand.Ink, shape)
                .clip(shape)
                .clickable(onClick = onClick)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painterResource(R.drawable.logo),
                contentDescription = "Logo Stickerly",
                modifier = Modifier.size(88.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Wordmark(34)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Buat Sticker WhatsApp dengan Mudah!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun Tile(
    modifier: Modifier,
    icon: String,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit,
) {
    ChunkyCard(modifier.height(158.dp), onClick = onClick) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                Modifier
                    .size(54.dp)
                    .background(Brand.Mint, CircleShape)
                    .border(2.dp, Brand.Ink, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(icon, fontSize = if (icon == "Aa") 20.sp else 26.sp, fontWeight = FontWeight.Black, color = Brand.GreenDark)
            }
            Column {
                Text(title, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(subtitle, fontSize = 13.sp, color = Brand.GreenDark)
            }
        }
        if (badge != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Brand.Yellow, CircleShape)
                    .border(2.dp, Brand.Ink, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(badge, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Brand.Ink)
            }
        }
    }
}
