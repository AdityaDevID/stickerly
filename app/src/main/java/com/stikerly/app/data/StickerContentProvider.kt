package com.stikerly.app.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

/** ContentProvider resmi WhatsApp Sticker API: WhatsApp membaca paket stiker lewat sini. */
class StickerContentProvider : ContentProvider() {

    private val matcher = UriMatcher(UriMatcher.NO_MATCH)

    private fun authority(): String = "${context!!.packageName}.stickercontentprovider"

    override fun onCreate(): Boolean {
        StickerStore.ensure(context!!)
        val auth = authority()
        matcher.addURI(auth, "metadata", METADATA)
        matcher.addURI(auth, "metadata/*", METADATA_SINGLE)
        matcher.addURI(auth, "stickers/*", STICKERS)
        matcher.addURI(auth, "stickers_asset/*/*", ASSET)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        StickerStore.ensure(context!!)
        return when (matcher.match(uri)) {
            METADATA -> metadataCursor(uri, StickerStore.packs())
            METADATA_SINGLE -> {
                val id = uri.lastPathSegment ?: ""
                metadataCursor(uri, listOfNotNull(StickerStore.pack(id)))
            }
            STICKERS -> {
                val id = uri.lastPathSegment ?: ""
                stickersCursor(uri, StickerStore.pack(id))
            }
            else -> throw IllegalArgumentException("URI tidak dikenal: $uri")
        }
    }

    private fun metadataCursor(uri: Uri, packs: List<StickerPack>): Cursor {
        val cursor = MatrixCursor(
            arrayOf(
                "sticker_pack_identifier",
                "sticker_pack_name",
                "sticker_pack_publisher",
                "sticker_pack_icon",
                "android_play_store_link",
                "ios_app_download_link",
                "sticker_pack_publisher_email",
                "sticker_pack_publisher_website",
                "sticker_pack_privacy_policy_website",
                "sticker_pack_license_agreement_website",
                "image_data_version",
                "whatsapp_will_not_cache_stickers",
                "animated_sticker_pack",
            )
        )
        for (p in packs) {
            cursor.addRow(
                arrayOf<Any>(
                    p.id,
                    p.name,
                    p.publisher,
                    StickerStore.TRAY_FILE,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    p.version.toString(),
                    0,
                    if (p.animated) 1 else 0,
                )
            )
        }
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    private fun stickersCursor(uri: Uri, pack: StickerPack?): Cursor {
        val cursor = MatrixCursor(arrayOf("sticker_file_name", "sticker_emoji", "sticker_accessibility_text"))
        if (pack != null) {
            for (s in pack.stickers) {
                cursor.addRow(arrayOf<Any>(s.file, s.emoji, "Stiker"))
            }
        }
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        StickerStore.ensure(context!!)
        if (matcher.match(uri) != ASSET) return null
        val segments = uri.pathSegments
        if (segments.size < 3) return null
        val packId = segments[segments.size - 2]
        val name = segments[segments.size - 1]
        val dir = StickerStore.packDir(packId)
        val file = File(dir, name)
        val inside = try {
            file.canonicalPath.startsWith(dir.canonicalPath + File.separator)
        } catch (e: Exception) {
            false
        }
        if (!inside || !file.exists()) throw FileNotFoundException("File tidak ada: $name")
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, file.length())
    }

    override fun getType(uri: Uri): String? {
        val auth = authority()
        return when (matcher.match(uri)) {
            METADATA -> "vnd.android.cursor.dir/vnd.$auth.metadata"
            METADATA_SINGLE -> "vnd.android.cursor.item/vnd.$auth.metadata"
            STICKERS -> "vnd.android.cursor.dir/vnd.$auth.stickers"
            ASSET -> if ((uri.lastPathSegment ?: "").endsWith(".png")) "image/png" else "image/webp"
            else -> throw IllegalArgumentException("URI tidak dikenal: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        private const val METADATA = 1
        private const val METADATA_SINGLE = 2
        private const val STICKERS = 3
        private const val ASSET = 4
    }
}
