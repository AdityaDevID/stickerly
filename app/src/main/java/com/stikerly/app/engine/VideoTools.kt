package com.stikerly.app.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.roundToInt

data class VideoInfo(val durationMs: Long, val hasAudio: Boolean)

class PipelineResult(
    val webp: ByteArray?,
    val thumb: Bitmap?,
    val frames: Int,
    val fps: Int,
    val mp4: File?,
    val error: String?,
)

object VideoTools {

    fun probe(ctx: Context, uri: Uri): VideoInfo? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(ctx, uri)
            val dur = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val audio = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
            if (dur <= 0L) null else VideoInfo(dur, audio)
        } catch (e: Exception) {
            null
        } finally {
            try {
                r.release()
            } catch (e: Exception) {
            }
        }
    }

    fun frameAt(ctx: Context, uri: Uri, ms: Long): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(ctx, uri)
            r.getFrameAtTime(ms * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            null
        } finally {
            try {
                r.release()
            } catch (e: Exception) {
            }
        }
    }

    private fun frameName(i: Int) = "f_%04d.png".format(i)

    /** Ambil frame dari video, simpan sebagai PNG 512x512 di [dir]. @return jumlah frame. */
    suspend fun extractFrames(
        ctx: Context,
        uri: Uri,
        startMs: Long,
        endMs: Long,
        fps: Int,
        fit: Boolean,
        dir: File,
        onProgress: (Float) -> Unit,
    ): Int {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(ctx, uri)
            val total = (((endMs - startMs) * fps) / 1000L).toInt().coerceIn(1, 140)
            var count = 0
            for (i in 0 until total) {
                currentCoroutineContext().ensureActive()
                val tUs = startMs * 1000L + (i * 1_000_000L / fps)
                val src = r.getFrameAtTime(tUs, MediaMetadataRetriever.OPTION_CLOSEST)
                if (src != null) {
                    val out = Render.frameToCanvas(src, fit)
                    src.recycle()
                    FileOutputStream(File(dir, frameName(count))).use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    out.recycle()
                    count++
                }
                onProgress((i + 1f) / total)
            }
            return count
        } finally {
            try {
                r.release()
            } catch (e: Exception) {
            }
        }
    }

    /** Potong video (beserta suaranya) tanpa re-encode. */
    fun trimToMp4(ctx: Context, uri: Uri, startMs: Long, endMs: Long, out: File): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var started = false
        try {
            extractor.setDataSource(ctx, uri, null)
            out.parentFile?.mkdirs()
            if (out.exists()) out.delete()
            val mx = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = mx

            val rotation = try {
                val r = MediaMetadataRetriever()
                r.setDataSource(ctx, uri)
                val v = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                r.release()
                v
            } catch (e: Exception) {
                0
            }
            mx.setOrientationHint(rotation)

            val trackMap = HashMap<Int, Int>()
            var maxSize = 1 shl 20
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    try {
                        trackMap[i] = mx.addTrack(f)
                        extractor.selectTrack(i)
                        if (f.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                            maxSize = maxOf(maxSize, f.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                        }
                    } catch (e: Exception) {
                        // format audio yang tidak didukung muxer MP4 dilewati
                    }
                }
            }
            if (trackMap.isEmpty()) return false

            mx.start()
            started = true
            val endUs = endMs * 1000L
            extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val buf = ByteBuffer.allocate(maxSize)
            val info = MediaCodec.BufferInfo()
            var baseUs = -1L
            while (true) {
                val track = extractor.sampleTrackIndex
                if (track < 0) break
                val t = extractor.sampleTime
                if (t < 0 || t > endUs) break
                buf.clear()
                val size = extractor.readSampleData(buf, 0)
                if (size < 0) break
                if (baseUs < 0) baseUs = t
                val dst = trackMap[track]
                if (dst != null) {
                    info.set(0, size, (t - baseUs).coerceAtLeast(0L), extractor.sampleFlags)
                    mx.writeSampleData(dst, buf, info)
                }
                extractor.advance()
            }
            return true
        } catch (e: Exception) {
            return false
        } finally {
            try {
                if (started) muxer?.stop()
            } catch (e: Exception) {
            }
            try {
                muxer?.release()
            } catch (e: Exception) {
            }
            extractor.release()
        }
    }
}

/** Alur lengkap: video -> frame -> WebP animasi <= 500 KB (+ opsional MP4 bersuara). */
object VideoPipeline {
    private const val MAX_BYTES = 500_000
    private const val BASE_FPS = 12

    suspend fun run(
        ctx: Context,
        uri: Uri,
        startMs: Long,
        endMs: Long,
        fit: Boolean,
        withSound: Boolean,
        report: (Float, String) -> Unit,
    ): PipelineResult {
        val dir = File(ctx.cacheDir, "frames")
        dir.deleteRecursively()
        dir.mkdirs()
        try {
            report(0f, "Mengambil frame…")
            val n = VideoTools.extractFrames(ctx, uri, startMs, endMs, BASE_FPS, fit, dir) {
                report(it * 0.6f, "Mengambil frame…")
            }
            if (n == 0) return PipelineResult(null, null, 0, 0, null, "Frame video tidak bisa dibaca.")

            val attempts = listOf(
                12 to 70, 12 to 55, 10 to 45, 8 to 40, 8 to 30, 6 to 25, 6 to 15,
            )
            var best: ByteArray? = null
            var usedFps = BASE_FPS
            var usedFrames = n
            for ((i, a) in attempts.withIndex()) {
                currentCoroutineContext().ensureActive()
                val fps = a.first
                val q = a.second
                report(0.6f + 0.35f * i / attempts.size, "Mengompres… ($fps fps, kualitas $q)")
                val count = (n * fps / BASE_FPS).coerceAtLeast(1)
                val frames = ArrayList<ByteArray>(count)
                for (k in 0 until count) {
                    val idx = (k * BASE_FPS / fps.toFloat()).roundToInt().coerceAtMost(n - 1)
                    val bmp = BitmapFactory.decodeFile(File(dir, "f_%04d.png".format(idx)).absolutePath) ?: continue
                    frames.add(Render.webpBytes(bmp, q))
                    bmp.recycle()
                }
                val data = AnimatedWebp.build(frames, (1000f / fps).roundToInt(), Render.SIZE, Render.SIZE)
                if (data != null && data.size <= MAX_BYTES) {
                    best = data
                    usedFps = fps
                    usedFrames = frames.size
                    break
                }
            }

            val thumb = BitmapFactory.decodeFile(File(dir, "f_0000.png").absolutePath)

            var mp4: File? = null
            if (withSound) {
                report(0.97f, "Menyiapkan video bersuara…")
                val f = File(ctx.cacheDir, "shared/Stickerly_${System.currentTimeMillis()}.mp4")
                if (VideoTools.trimToMp4(ctx, uri, startMs, endMs, f)) mp4 = f
            }

            return if (best == null) {
                PipelineResult(null, thumb, n, 0, mp4, "Hasil masih di atas 500 KB. Coba perpendek durasinya.")
            } else {
                PipelineResult(best, thumb, usedFrames, usedFps, mp4, null)
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
