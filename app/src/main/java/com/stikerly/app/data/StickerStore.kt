package com.stikerly.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class StickerItem(val file: String, val emoji: String)

data class StickerPack(
    val id: String,
    val name: String,
    val publisher: String,
    val animated: Boolean,
    val version: Int,
    val stickers: List<StickerItem>,
)

/** Penyimpanan paket stiker (JSON + file WebP di filesDir). Dipakai UI dan ContentProvider WhatsApp. */
object StickerStore {
    const val MAX_STICKERS = 30
    const val MIN_STICKERS = 3
    const val TRAY_FILE = "tray.png"

    private var appContext: Context? = null
    private val lock = Any()

    /** Naik setiap ada perubahan, supaya UI Compose ikut refresh. */
    var revision by mutableIntStateOf(0)
        private set

    fun ensure(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    private fun ctx(): Context = appContext ?: throw IllegalStateException("StickerStore belum diinisialisasi")

    private fun dataFile(): File = File(ctx().filesDir, "packs.json")

    fun packDir(id: String): File = File(ctx().filesDir, "packs/$id")

    fun thumbFile(id: String, file: String): File = File(packDir(id), "thumb_$file.png")

    private fun load(): MutableList<StickerPack> {
        val f = dataFile()
        if (!f.exists()) return mutableListOf()
        return try {
            val arr = JSONObject(f.readText()).getJSONArray("packs")
            val out = mutableListOf<StickerPack>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val sa = o.getJSONArray("stickers")
                val items = mutableListOf<StickerItem>()
                for (j in 0 until sa.length()) {
                    val s = sa.getJSONObject(j)
                    items.add(StickerItem(s.getString("file"), s.optString("emoji", "😀")))
                }
                out.add(
                    StickerPack(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        publisher = o.optString("publisher", "Stickerly"),
                        animated = o.optBoolean("animated", false),
                        version = o.optInt("version", 1),
                        stickers = items,
                    )
                )
            }
            out
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun save(list: List<StickerPack>) {
        val packs = JSONArray()
        for (p in list) {
            val st = JSONArray()
            for (s in p.stickers) {
                st.put(JSONObject().put("file", s.file).put("emoji", s.emoji))
            }
            packs.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("publisher", p.publisher)
                    .put("animated", p.animated)
                    .put("version", p.version)
                    .put("stickers", st)
            )
        }
        val tmp = File(ctx().filesDir, "packs.json.tmp")
        tmp.writeText(JSONObject().put("packs", packs).toString())
        tmp.renameTo(dataFile())
    }

    private fun bump() {
        revision += 1
    }

    fun packs(): List<StickerPack> = synchronized(lock) { load() }

    fun pack(id: String): StickerPack? = synchronized(lock) { load().firstOrNull { it.id == id } }

    fun createPack(name: String, animated: Boolean): StickerPack {
        val created = synchronized(lock) {
            val list = load()
            val p = StickerPack(
                id = UUID.randomUUID().toString().replace("-", "").take(20),
                name = name.trim().take(60).ifBlank { "Paket Stickerly" },
                publisher = "Stickerly",
                animated = animated,
                version = 1,
                stickers = emptyList(),
            )
            list.add(p)
            save(list)
            p
        }
        bump()
        return created
    }

    fun addSticker(packId: String, webp: ByteArray, thumb: Bitmap, emoji: String, animated: Boolean): Boolean {
        val ok = synchronized(lock) {
            val list = load()
            val idx = list.indexOfFirst { it.id == packId }
            if (idx < 0) {
                false
            } else {
                val p = list[idx]
                val compatible = p.stickers.isEmpty() || p.animated == animated
                if (!compatible || p.stickers.size >= MAX_STICKERS) {
                    false
                } else {
                    val dir = packDir(packId)
                    dir.mkdirs()
                    val file = UUID.randomUUID().toString().replace("-", "").take(16) + ".webp"
                    File(dir, file).writeBytes(webp)
                    val small = Bitmap.createScaledBitmap(thumb, 192, 192, true)
                    FileOutputStream(thumbFile(packId, file)).use { small.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    val updated = p.copy(
                        animated = animated,
                        version = p.version + 1,
                        stickers = p.stickers + StickerItem(file, emoji.ifBlank { "😀" }),
                    )
                    list[idx] = updated
                    save(list)
                    refreshTray(updated)
                    true
                }
            }
        }
        if (ok) bump()
        return ok
    }

    fun removeSticker(packId: String, file: String) {
        synchronized(lock) {
            val list = load()
            val idx = list.indexOfFirst { it.id == packId }
            if (idx >= 0) {
                val p = list[idx]
                val updated = p.copy(version = p.version + 1, stickers = p.stickers.filter { it.file != file })
                File(packDir(packId), file).delete()
                thumbFile(packId, file).delete()
                list[idx] = updated
                save(list)
                refreshTray(updated)
            }
        }
        bump()
    }

    fun deletePack(id: String) {
        synchronized(lock) {
            val list = load()
            list.removeAll { it.id == id }
            save(list)
            packDir(id).deleteRecursively()
        }
        bump()
    }

    private fun refreshTray(pack: StickerPack) {
        val trayFile = File(packDir(pack.id), TRAY_FILE)
        val first = pack.stickers.firstOrNull()
        if (first == null) {
            trayFile.delete()
            return
        }
        val src = BitmapFactory.decodeFile(thumbFile(pack.id, first.file).absolutePath) ?: return
        val tray = Bitmap.createScaledBitmap(src, 96, 96, true)
        FileOutputStream(trayFile).use { tray.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
