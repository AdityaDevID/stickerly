package com.stikerly.app.engine

import java.io.ByteArrayOutputStream

/**
 * Merakit WebP animasi dari beberapa WebP statis (tanpa FFmpeg).
 * Format: RIFF > VP8X + ANIM + ANMF(frame)... — sudah diuji decode di Pillow.
 */
object AnimatedWebp {

    private fun le32(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte())

    private fun le24(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte())

    private fun readLe32(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xff) or
            ((b[o + 1].toInt() and 0xff) shl 8) or
            ((b[o + 2].toInt() and 0xff) shl 16) or
            ((b[o + 3].toInt() and 0xff) shl 24)

    private fun ascii(b: ByteArray, o: Int, n: Int): String = String(b, o, n, Charsets.ISO_8859_1)

    /** Ambil chunk gambar (ALPH / VP8 / VP8L) dari WebP statis. */
    private fun frameChunks(webp: ByteArray): ByteArray? {
        if (webp.size < 20 || ascii(webp, 0, 4) != "RIFF" || ascii(webp, 8, 4) != "WEBP") return null
        val out = ByteArrayOutputStream()
        var pos = 12
        while (pos + 8 <= webp.size) {
            val tag = ascii(webp, pos, 4)
            val len = readLe32(webp, pos + 4)
            if (len < 0) break
            val total = 8 + len + (len and 1)
            if (tag == "ALPH" || tag == "VP8 " || tag == "VP8L") {
                val avail = minOf(total, webp.size - pos)
                out.write(webp, pos, avail)
                for (k in avail until total) out.write(0)
            }
            pos += total
        }
        return if (out.size() > 0) out.toByteArray() else null
    }

    /** @return bytes WebP animasi, atau null kalau ada frame yang tidak valid. */
    fun build(frames: List<ByteArray>, frameDurationMs: Int, width: Int, height: Int): ByteArray? {
        if (frames.isEmpty()) return null
        val body = ByteArrayOutputStream()
        body.write("WEBP".toByteArray(Charsets.ISO_8859_1))

        // VP8X: flag animasi (0x02) + alpha (0x10)
        body.write("VP8X".toByteArray(Charsets.ISO_8859_1))
        body.write(le32(10))
        body.write(byteArrayOf(0x12, 0, 0, 0))
        body.write(le24(width - 1))
        body.write(le24(height - 1))

        // ANIM: background transparan, loop tanpa batas
        body.write("ANIM".toByteArray(Charsets.ISO_8859_1))
        body.write(le32(6))
        body.write(le32(0))
        body.write(byteArrayOf(0, 0))

        for (f in frames) {
            val data = frameChunks(f) ?: return null
            body.write("ANMF".toByteArray(Charsets.ISO_8859_1))
            body.write(le32(16 + data.size))
            body.write(le24(0)) // x
            body.write(le24(0)) // y
            body.write(le24(width - 1))
            body.write(le24(height - 1))
            body.write(le24(frameDurationMs))
            body.write(byteArrayOf(0x01)) // dispose ke background, alpha-blend
            body.write(data)
        }

        val bodyBytes = body.toByteArray()
        val out = ByteArrayOutputStream()
        out.write("RIFF".toByteArray(Charsets.ISO_8859_1))
        out.write(le32(bodyBytes.size))
        out.write(bodyBytes)
        return out.toByteArray()
    }
}
