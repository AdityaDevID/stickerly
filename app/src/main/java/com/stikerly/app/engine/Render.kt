package com.stikerly.app.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

enum class CropShape { Square, Rounded, Circle }

object Render {
    const val SIZE = 512

    @Suppress("DEPRECATION")
    private fun webpFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP

    fun webpBytes(bmp: Bitmap, quality: Int): ByteArray {
        val bos = ByteArrayOutputStream()
        bmp.compress(webpFormat(), quality, bos)
        return bos.toByteArray()
    }

    /** WebP statis 512x512, maksimal 100 KB (batas WhatsApp). */
    fun staticWebp(bmp: Bitmap, maxBytes: Int = 100_000): ByteArray? {
        for (q in intArrayOf(95, 85, 75, 65, 55, 45, 35, 25, 15)) {
            val b = webpBytes(bmp, q)
            if (b.size <= maxBytes) return b
        }
        return null
    }

    // ---------- Teks ----------

    fun renderText(
        text: String,
        color: Int,
        outlineColor: Int,
        outline: Boolean,
        typeface: Typeface,
        scale: Float,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val clean = text.ifBlank { " " }
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        paint.typeface = typeface
        val pad = 30f
        val maxW = SIZE - pad * 2

        fun build(size: Float): StaticLayout {
            paint.textSize = size
            return StaticLayout.Builder
                .obtain(clean, 0, clean.length, paint, maxW.toInt())
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 0.95f)
                .build()
        }

        fun fits(l: StaticLayout): Boolean {
            var widest = 0f
            for (i in 0 until l.lineCount) widest = max(widest, l.getLineWidth(i))
            return l.height <= maxW && widest <= maxW + 1f
        }

        var size = 190f * scale
        var layout = build(size)
        while (!fits(layout) && size > 20f) {
            size -= 4f
            layout = build(size)
        }

        canvas.save()
        canvas.translate((SIZE - layout.width) / 2f, (SIZE - layout.height) / 2f)
        if (outline) {
            paint.style = Paint.Style.STROKE
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeWidth = size * 0.16f
            paint.color = outlineColor
            layout.draw(canvas)
        }
        paint.style = Paint.Style.FILL
        paint.color = color
        layout.draw(canvas)
        canvas.restore()
        return bmp
    }

    // ---------- Gambar ----------

    fun loadBitmap(ctx: Context, uri: Uri, maxSide: Int = 2048): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            if (bmp == null) {
                null
            } else {
                val orient = ctx.contentResolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
                val m = Matrix()
                var rotate = true
                when (orient) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
                    else -> rotate = false
                }
                if (rotate) Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true) else bmp
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun baseScale(bw: Int, bh: Int, viewSize: Float): Float = max(viewSize / bw, viewSize / bh)

    /** Matriks yang memetakan bitmap ke kotak pratinjau (viewSize x viewSize). */
    fun cropMatrix(bw: Int, bh: Int, viewSize: Float, zoom: Float, offX: Float, offY: Float): Matrix {
        val sc = baseScale(bw, bh, viewSize) * zoom
        val left = viewSize / 2f + offX - bw * sc / 2f
        val top = viewSize / 2f + offY - bh * sc / 2f
        return Matrix().apply {
            setScale(sc, sc)
            postTranslate(left, top)
        }
    }

    fun clampOffset(bw: Int, bh: Int, viewSize: Float, zoom: Float, off: Float, horizontal: Boolean): Float {
        val sc = baseScale(bw, bh, viewSize) * zoom
        val dim = if (horizontal) bw else bh
        val maxOff = max(0f, dim * sc / 2f - viewSize / 2f)
        return off.coerceIn(-maxOff, maxOff)
    }

    fun renderCrop(src: Bitmap, viewSize: Float, zoom: Float, offX: Float, offY: Float, shape: CropShape): Bitmap {
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val f = SIZE / viewSize
        val m = cropMatrix(src.width, src.height, viewSize, zoom, offX, offY)
        m.postScale(f, f)
        c.drawBitmap(src, m, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

        if (shape != CropShape.Square) {
            val mask = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
            val mc = Canvas(mask)
            val mp = Paint(Paint.ANTI_ALIAS_FLAG)
            mp.color = Color.BLACK
            val s = SIZE.toFloat()
            if (shape == CropShape.Circle) {
                mc.drawCircle(s / 2f, s / 2f, s / 2f, mp)
            } else {
                mc.drawRoundRect(RectF(0f, 0f, s, s), 96f, 96f, mp)
            }
            val dp = Paint()
            dp.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            c.drawBitmap(mask, 0f, 0f, dp)
            mask.recycle()
        }
        return out
    }

    // ---------- Video ----------

    /** Taruh frame video ke kanvas 512x512 (isi penuh atau muat tanpa potong). */
    fun frameToCanvas(src: Bitmap, fit: Boolean): Bitmap {
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val w = src.width.toFloat()
        val h = src.height.toFloat()
        val s = if (fit) min(SIZE / w, SIZE / h) else max(SIZE / w, SIZE / h)
        val dw = w * s
        val dh = h * s
        val dx = (SIZE - dw) / 2f
        val dy = (SIZE - dh) / 2f
        c.drawBitmap(src, null, RectF(dx, dy, dx + dw, dy + dh), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        return out
    }
}
