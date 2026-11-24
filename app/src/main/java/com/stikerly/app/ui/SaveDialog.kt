package com.stikerly.app.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stikerly.app.data.StickerStore

object SaveHelper {
    /** @return pesan error, atau null kalau berhasil. */
    fun save(
        ctx: Context,
        webp: ByteArray,
        thumb: Bitmap,
        animated: Boolean,
        existingId: String?,
        newName: String,
        emoji: String,
    ): String? {
        StickerStore.ensure(ctx)
        val packId = existingId ?: StickerStore.createPack(newName, animated).id
        val ok = StickerStore.addSticker(packId, webp, thumb, emoji, animated)
        return if (ok) null else "Paket penuh, atau jenisnya beda (stiker statis dan animasi tidak boleh dicampur)."
    }
}

@Composable
fun SaveToPackDialog(
    animated: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (existingId: String?, newName: String, emoji: String) -> Unit,
) {
    val rev = StickerStore.revision
    val packs = remember(rev) {
        StickerStore.packs().filter {
            (it.stickers.isEmpty() || it.animated == animated) && it.stickers.size < StickerStore.MAX_STICKERS
        }
    }
    var selected by remember { mutableStateOf(packs.firstOrNull()?.id) }
    var newName by remember { mutableStateOf(if (packs.isEmpty()) "Paket Stickerly" else "") }
    var emoji by remember { mutableStateOf("😀") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Simpan ke paket") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (packs.isNotEmpty()) {
                    Text("Pilih paket:")
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (p in packs) {
                            Chip(
                                label = p.name,
                                selected = selected == p.id && newName.isBlank(),
                                onClick = {
                                    selected = p.id
                                    newName = ""
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(if (packs.isEmpty()) "Nama paket baru" else "Atau buat paket baru") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it.take(8) },
                    label = { Text("Emoji stiker") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected != null || newName.isNotBlank(),
                onClick = {
                    val existing = if (newName.isBlank()) selected else null
                    onConfirm(existing, newName.trim(), emoji.ifBlank { "😀" })
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
