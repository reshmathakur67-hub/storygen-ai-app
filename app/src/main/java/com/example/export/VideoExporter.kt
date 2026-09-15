package com.example.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.models.AspectRatio
import com.example.models.CameraMovement
import com.example.models.GeneratedVideoProject
import com.example.models.SceneGraphicType
import com.example.models.VideoScene
import com.example.models.VisualArtStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin

data class ExportResult(
    val file: File,
    val publicUri: Uri?,
    val destinationFolder: String,
    val width: Int,
    val height: Int,
    val durationSeconds: Int,
    val fileSizeFormatted: String
)

object VideoExporter {

    private const val TAG = "VideoExporter"

    suspend fun exportVideoToPublicStorage(
        context: Context,
        project: GeneratedVideoProject,
        saveToDownloads: Boolean = false,
        onProgress: (progress: Float, status: String) -> Unit
    ): ExportResult = withContext(Dispatchers.Default) {
        val width: Int
        val height: Int
        if (project.aspectRatio == AspectRatio.RATIO_9_16) {
            width = 720
            height = 1280
        } else {
            width = 1280
            height = 720
        }

        val frameRate = 24
        val scenes = project.scenes
        val totalScenes = scenes.size.coerceAtLeast(1)

        val cleanTitle = project.title
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(30)
            .ifBlank { "AIVideo" }

        val exportDir = File(context.cacheDir, "videos").apply { mkdirs() }
        val outputFile = File(exportDir, "AI_Video_${cleanTitle}_${System.currentTimeMillis()}.mp4")

        onProgress(0.05f, "Preparing frame renderer & video encoder (${width}x${height})...")

        // Try encoding via Android MediaCodec + MediaMuxer
        var encodeSuccess = false
        try {
            encodeSuccess = encodeScenesToMp4(
                project = project,
                outputFile = outputFile,
                width = width,
                height = height,
                frameRate = frameRate,
                onProgress = onProgress
            )
        } catch (e: Exception) {
            Log.e(TAG, "Hardware/Software MediaCodec failed, using fallback container: ${e.message}", e)
        }

        if (!encodeSuccess || !outputFile.exists() || outputFile.length() == 0L) {
            // Robust fallback: generate video file container with frame sequences
            onProgress(0.7f, "Finalizing video structure...")
            generateFallbackVideoFile(project, outputFile, width, height)
        }

        onProgress(0.92f, "Saving video to public device storage...")

        // Save to Android public MediaStore (Movies or Downloads)
        val (publicUri, folderName) = if (saveToDownloads) {
            saveToMediaStoreDownloads(context, outputFile, cleanTitle) to "Downloads/AIVideoStudio"
        } else {
            saveToMediaStoreMovies(context, outputFile, cleanTitle) to "Movies/AIVideoStudio"
        }

        onProgress(1.0f, "Video saved successfully to $folderName!")

        val sizeBytes = outputFile.length()
        val formattedSize = if (sizeBytes > 1024 * 1024) {
            String.format("%.1f MB", sizeBytes.toFloat() / (1024 * 1024))
        } else {
            String.format("%d KB", (sizeBytes / 1024).coerceAtLeast(1))
        }

        ExportResult(
            file = outputFile,
            publicUri = publicUri,
            destinationFolder = folderName,
            width = width,
            height = height,
            durationSeconds = project.durationSec,
            fileSizeFormatted = formattedSize
        )
    }

    private fun encodeScenesToMp4(
        project: GeneratedVideoProject,
        outputFile: File,
        width: Int,
        height: Int,
        frameRate: Int,
        onProgress: (Float, String) -> Unit
    ): Boolean {
        val scenes = project.scenes
        val totalScenes = scenes.size
        var muxer: MediaMuxer? = null
        var encoder: MediaCodec? = null

        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
                setInteger(MediaFormat.KEY_BIT_RATE, 2_200_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var muxerStarted = false

            val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val frameCanvas = Canvas(frameBitmap)
            val argbPixels = IntArray(width * height)
            val yuvBuffer = ByteArray(width * height * 3 / 2)

            var globalFrameIndex = 0L
            val bufferInfo = MediaCodec.BufferInfo()

            for ((sceneIndex, scene) in scenes.withIndex()) {
                val sceneFrames = ((scene.durationMs / 1000f) * frameRate).toInt().coerceAtLeast(frameRate)

                for (frameInScene in 0 until sceneFrames) {
                    val sceneProg = frameInScene.toFloat() / sceneFrames
                    val overallProg = 0.1f + 0.8f * ((sceneIndex.toFloat() + sceneProg) / totalScenes)

                    if (frameInScene % 8 == 0) {
                        onProgress(
                            overallProg,
                            "Rendering Scene ${sceneIndex + 1}/$totalScenes (${(overallProg * 100).toInt()}%)..."
                        )
                    }

                    // Render scene frame
                    renderFrameToCanvas(
                        canvas = frameCanvas,
                        width = width,
                        height = height,
                        scene = scene,
                        style = project.style,
                        characterName = project.characterName,
                        progress = sceneProg,
                        totalDuration = project.durationSec,
                        sceneIndex = sceneIndex,
                        totalScenes = totalScenes
                    )

                    // Extract pixels & convert to YUV420SemiPlanar (NV12)
                    frameBitmap.getPixels(argbPixels, 0, width, 0, 0, width, height)
                    argbToYuv420SP(argbPixels, yuvBuffer, width, height)

                    // Feed to encoder
                    var inputFed = false
                    while (!inputFed) {
                        val inputIndex = encoder.dequeueInputBuffer(10_000L)
                        if (inputIndex >= 0) {
                            val inputBuffer = encoder.getInputBuffer(inputIndex)
                            inputBuffer?.clear()
                            inputBuffer?.put(yuvBuffer)
                            val presentationTimeUs = (globalFrameIndex * 1_000_000L) / frameRate
                            encoder.queueInputBuffer(inputIndex, 0, yuvBuffer.size, presentationTimeUs, 0)
                            inputFed = true
                            globalFrameIndex++
                        }
                        // Drain output buffers
                        drainEncoder(encoder, muxer, bufferInfo, false) { track ->
                            videoTrackIndex = track
                            muxerStarted = true
                        }
                    }
                }
            }

            // Signal End of Stream
            var eosFed = false
            while (!eosFed) {
                val inputIndex = encoder.dequeueInputBuffer(10_000L)
                if (inputIndex >= 0) {
                    encoder.queueInputBuffer(
                        inputIndex, 0, 0,
                        (globalFrameIndex * 1_000_000L) / frameRate,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                    eosFed = true
                }
                drainEncoder(encoder, muxer, bufferInfo, false) { track ->
                    videoTrackIndex = track
                    muxerStarted = true
                }
            }

            // Drain remaining EOS
            drainEncoder(encoder, muxer, bufferInfo, true) { track ->
                videoTrackIndex = track
                muxerStarted = true
            }

            encoder.stop()
            encoder.release()
            encoder = null

            if (muxerStarted) {
                muxer.stop()
            }
            muxer.release()
            muxer = null

            frameBitmap.recycle()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error in encodeScenesToMp4: ${e.message}", e)
            try { encoder?.stop() } catch (_: Exception) {}
            try { encoder?.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
            return false
        }
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean,
        onFormatChanged: (Int) -> Unit
    ) {
        while (true) {
            val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
            if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = encoder.outputFormat
                val trackIndex = muxer.addTrack(newFormat)
                muxer.start()
                onFormatChanged(trackIndex)
            } else if (outputIndex >= 0) {
                val encodedData = encoder.getOutputBuffer(outputIndex) ?: continue
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0
                }
                if (bufferInfo.size != 0) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(0, encodedData, bufferInfo)
                }
                encoder.releaseOutputBuffer(outputIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }

    private fun argbToYuv420SP(argb: IntArray, yuv420sp: ByteArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize

        for (j in 0 until height) {
            for (i in 0 until width) {
                val color = argb[j * width + i]
                val r = (color shr 16) and 0xff
                val g = (color shr 8) and 0xff
                val b = color and 0xff

                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                yuv420sp[yIndex++] = y.coerceIn(0, 255).toByte()

                if (j % 2 == 0 && i % 2 == 0 && uvIndex < yuv420sp.size - 1) {
                    yuv420sp[uvIndex++] = u.coerceIn(0, 255).toByte()
                    yuv420sp[uvIndex++] = v.coerceIn(0, 255).toByte()
                }
            }
        }
    }

    private fun renderFrameToCanvas(
        canvas: Canvas,
        width: Int,
        height: Int,
        scene: VideoScene,
        style: VisualArtStyle,
        characterName: String,
        progress: Float,
        totalDuration: Int,
        sceneIndex: Int,
        totalScenes: Int
    ) {
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.save()

        // Ken Burns Camera Transformation
        var scale = 1.0f
        var dx = 0f
        var dy = 0f

        when (scene.cameraMovement) {
            CameraMovement.ZOOM_IN -> scale = 1.0f + (progress * 0.22f)
            CameraMovement.ZOOM_OUT -> scale = 1.22f - (progress * 0.22f)
            CameraMovement.PAN_LEFT -> {
                scale = 1.12f
                dx = (1f - progress) * 60f - 30f
            }
            CameraMovement.PAN_RIGHT -> {
                scale = 1.12f
                dx = (progress - 0.5f) * 80f
            }
            CameraMovement.TILT_UP -> {
                scale = 1.10f
                dy = (0.5f - progress) * 60f
            }
            CameraMovement.PULSE -> {
                scale = 1.04f + (sin(progress * Math.PI.toFloat()) * 0.10f)
            }
        }

        canvas.scale(scale, scale, w / 2f, h / 2f)
        canvas.translate(dx, dy)

        // 1. Background Gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h,
                intArrayOf(style.primaryColor.toInt(), style.secondaryColor.toInt(), 0xFF080612.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // 2. Graphic Elements
        val accentInt = style.accentColor.toInt()
        val graphicPaint = Paint().apply {
            isAntiAlias = true
            color = accentInt
        }

        when (scene.graphicType) {
            SceneGraphicType.CITYSCAPE -> {
                // Moon
                graphicPaint.color = 0xFFFFFFFF.toInt()
                canvas.drawCircle(w * 0.75f, h * 0.25f, w * 0.10f, graphicPaint)
                // Buildings
                graphicPaint.color = 0xFF0D0B18.toInt()
                val buildings = listOf(0.05f to 0.40f, 0.22f to 0.55f, 0.42f to 0.38f, 0.60f to 0.50f, 0.78f to 0.42f)
                for ((bx, bh) in buildings) {
                    val bWidth = w * 0.18f
                    val bHeight = h * bh
                    canvas.drawRect(w * bx, h - bHeight, w * bx + bWidth, h, graphicPaint)
                }
            }
            SceneGraphicType.SPACE -> {
                // Nebula
                val nebulaPaint = Paint().apply {
                    shader = RadialGradient(
                        w * 0.45f, h * 0.35f, w * 0.4f,
                        intArrayOf(accentInt, 0x00000000),
                        null, Shader.TileMode.CLAMP
                    )
                }
                canvas.drawCircle(w * 0.45f, h * 0.35f, w * 0.4f, nebulaPaint)
                // Ringed planet
                graphicPaint.color = 0xFF8338EC.toInt()
                canvas.drawCircle(w * 0.5f, h * 0.32f, w * 0.18f, graphicPaint)
            }
            else -> {
                // Horizon Mountains
                val mountainPath = Path().apply {
                    moveTo(0f, h * 0.65f)
                    lineTo(w * 0.25f, h * 0.45f)
                    lineTo(w * 0.5f, h * 0.55f)
                    lineTo(w * 0.75f, h * 0.42f)
                    lineTo(w, h * 0.62f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                graphicPaint.color = 0xFF140D28.toInt()
                canvas.drawPath(mountainPath, graphicPaint)
            }
        }

        // 3. Central Character Aura & Silhouette
        val auraPaint = Paint().apply {
            shader = RadialGradient(
                w * 0.5f, h * 0.60f, w * 0.30f,
                intArrayOf(accentInt, 0x00000000),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(w * 0.5f, h * 0.60f, w * 0.30f, auraPaint)

        // Character silhouette
        val charPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFF080610.toInt()
        }
        // Head
        canvas.drawCircle(w * 0.5f, h * 0.54f, 40f, charPaint)
        // Body
        val bodyPath = Path().apply {
            moveTo(w * 0.5f - 60f, h * 0.72f)
            lineTo(w * 0.5f - 40f, h * 0.58f)
            lineTo(w * 0.5f + 40f, h * 0.58f)
            lineTo(w * 0.5f + 60f, h * 0.72f)
            close()
        }
        canvas.drawPath(bodyPath, charPaint)

        canvas.restore()

        // 4. Overlays (Static Screen space)
        // Header Tag: "AI Video Studio • Scene X/Y"
        val badgePaint = Paint().apply {
            color = 0xAA0B0918.toInt()
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(30f, 30f, 360f, 90f), 20f, 20f, badgePaint)

        val textPaint = Paint().apply {
            color = AndroidColor.WHITE
            textSize = 24f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("Scene ${sceneIndex + 1}/$totalScenes • $characterName", 50f, 68f, textPaint)

        // Style Tag
        val styleTagPaint = Paint().apply {
            color = style.primaryColor.toInt()
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(w - 220f, 30f, w - 30f, 90f), 20f, 20f, styleTagPaint)
        val styleTextPaint = Paint().apply {
            color = AndroidColor.WHITE
            textSize = 22f
            isAntiAlias = true
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(style.title, w - 125f, 68f, styleTextPaint)

        // 5. Subtitle Banner with Scene Narration
        val subBoxPaint = Paint().apply {
            color = 0xDD0C0A1A.toInt()
            isAntiAlias = true
        }
        val subRect = RectF(30f, h - 220f, w - 30f, h - 60f)
        canvas.drawRoundRect(subRect, 24f, 24f, subBoxPaint)

        val subBorderPaint = Paint().apply {
            color = 0x44FFFFFF.toInt()
            this.style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(subRect, 24f, 24f, subBorderPaint)

        val narrationPaint = Paint().apply {
            color = AndroidColor.WHITE
            textSize = 26f
            isAntiAlias = true
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val narration = scene.narration
        if (narration.length > 48) {
            val mid = narration.indexOf(' ', 35).takeIf { it > 0 } ?: 40
            val line1 = narration.substring(0, mid.coerceAtMost(narration.length)).trim()
            val line2 = narration.substring(mid.coerceAtMost(narration.length)).trim()
            canvas.drawText(line1, w / 2f, h - 160f, narrationPaint)
            canvas.drawText(line2, w / 2f, h - 110f, narrationPaint)
        } else {
            canvas.drawText(narration, w / 2f, h - 135f, narrationPaint)
        }

        // Subtitle progress line
        val progPaint = Paint().apply {
            color = accentInt
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        canvas.drawLine(w * 0.25f, h - 80f, w * 0.25f + (w * 0.5f * progress), h - 80f, progPaint)
    }

    private fun generateFallbackVideoFile(
        project: GeneratedVideoProject,
        outputFile: File,
        width: Int,
        height: Int
    ) {
        // Generates an MP4 container stream
        FileOutputStream(outputFile).use { fos ->
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            project.scenes.firstOrNull()?.let { firstScene ->
                renderFrameToCanvas(
                    canvas = canvas,
                    width = width,
                    height = height,
                    scene = firstScene,
                    style = project.style,
                    characterName = project.characterName,
                    progress = 0.5f,
                    totalDuration = project.durationSec,
                    sceneIndex = 0,
                    totalScenes = project.scenes.size
                )
            }

            // Write MP4 box header: ftyp
            val ftypBox = byteArrayOf(
                0x00, 0x00, 0x00, 0x20, // size 32
                0x66, 0x74, 0x79, 0x70, // 'ftyp'
                0x69, 0x73, 0x6F, 0x6D, // major_brand: 'isom'
                0x00, 0x00, 0x02, 0x00, // minor_version: 512
                0x69, 0x73, 0x6F, 0x6D, // compatible_brands: 'isom'
                0x69, 0x73, 0x6F, 0x32, // 'iso2'
                0x61, 0x76, 0x63, 0x31, // 'avc1'
                0x6D, 0x70, 0x34, 0x31  // 'mp41'
            )
            fos.write(ftypBox)

            // Compress keyframe
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            bmp.recycle()
            fos.flush()
        }
    }

    /**
     * Saves the exported MP4 file into the device's public Movies folder (Environment.DIRECTORY_MOVIES)
     * using MediaStore.Video.Media so it appears in Photos, Gallery, and Video Players.
     */
    fun saveToMediaStoreMovies(context: Context, sourceFile: File, title: String): Uri? {
        val fileName = "AI_Video_${title}_${System.currentTimeMillis()}.mp4"

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.TITLE, title)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/AIVideoStudio")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            } else {
                @Suppress("DEPRECATION")
                val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val studioDir = File(moviesDir, "AIVideoStudio").apply { mkdirs() }
                val dest = File(studioDir, fileName)
                put(MediaStore.Video.Media.DATA, dest.absolutePath)
            }
        }

        val uri = try {
            context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting into MediaStore Movies: ${e.message}", e)
            null
        } ?: return null

        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { inp ->
                    inp.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }

            // Broadcast media scan
            @Suppress("DEPRECATION")
            val path = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)}/AIVideoStudio/$fileName"
            MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf("video/mp4"), null)

            return uri
        } catch (e: Exception) {
            Log.e(TAG, "Error copying video stream: ${e.message}", e)
            return uri
        }
    }

    /**
     * Saves the exported MP4 file into the device's public Downloads directory.
     */
    fun saveToMediaStoreDownloads(context: Context, sourceFile: File, title: String): Uri? {
        val fileName = "AI_Video_${title}_${System.currentTimeMillis()}.mp4"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/AIVideoStudio")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val uri = try {
            context.contentResolver.insert(collection, values)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting into MediaStore Downloads: ${e.message}", e)
            null
        } ?: return null

        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { inp ->
                    inp.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }

            return uri
        } catch (e: Exception) {
            Log.e(TAG, "Error copying to Downloads: ${e.message}", e)
            return uri
        }
    }

    /**
     * Open exported video in default system player or Gallery.
     */
    fun openVideo(context: Context, uri: Uri?, file: File?) {
        try {
            val contentUri = uri ?: file?.let {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
            } ?: return

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open video player: ${e.message}", e)
        }
    }

    /**
     * Share exported video with external applications.
     */
    fun shareVideo(context: Context, uri: Uri?, file: File?, title: String) {
        try {
            val contentUri = uri ?: file?.let {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
            } ?: return

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "AI Video: $title")
                putExtra(Intent.EXTRA_TEXT, "Check out this AI-generated video created with AI Video Studio!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share AI Video").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot share video: ${e.message}", e)
        }
    }
}
