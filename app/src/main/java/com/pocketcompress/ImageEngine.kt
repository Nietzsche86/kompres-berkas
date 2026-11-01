package com.pocketcompress

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

object ImageEngine {
    suspend fun compress(context: Context, uri: Uri, config: CompressConfig): File = withContext(Dispatchers.IO) {
        val degrees = context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        options.inSampleSize = (1f / config.scale.coerceIn(0.1f, 1f)).roundToInt().coerceAtLeast(1)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565

        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IllegalArgumentException("Gagal membuka gambar")

        val finalBitmap = if (degrees != 0f) {
            val matrix = Matrix().apply { postRotate(degrees) }
            val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            decoded.recycle()
            rotated
        } else {
            decoded
        }

        val outFile = File(context.cacheDir, "compressed_img.jpg")
        var quality = config.quality.coerceIn(10, 100)
        val targetBytes = config.targetKb?.let { it * 1024L }

        while (true) {
            outFile.outputStream().use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            if (targetBytes == null || outFile.length() <= targetBytes || quality <= 15) {
                break
            }
            quality = (quality - 10).coerceAtLeast(15)
        }

        finalBitmap.recycle()
        outFile
    }
}
