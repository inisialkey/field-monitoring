package com.inisialkey.fieldmonitoring.report.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class PhotoStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val authority = "${context.packageName}.files"

    fun newCameraTarget(): Uri {
        val file = File(context.cacheDir, "camera/${UUID.randomUUID()}.jpg").apply { parentFile?.mkdirs() }
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun discard(uri: Uri) {
        if (uri.authority == authority) context.contentResolver.delete(uri, null, null)
    }

    suspend fun import(source: Uri, ownerId: String, attachmentId: String): File = withContext(Dispatchers.IO) {
        val bitmap = decode(source)
        val bytes = try {
            compress(bitmap)
        } finally {
            bitmap.recycle()
        }
        val target = File(context.filesDir, "attachments/$ownerId/$attachmentId.jpg").apply { parentFile?.mkdirs() }
        val temp = File(target.path + ".tmp")
        temp.writeBytes(bytes)
        if (!temp.renameTo(target)) throw IOException("Cannot store photo")
        discard(source)
        target
    }

    suspend fun delete(paths: List<String>) = withContext(Dispatchers.IO) {
        paths.forEach { File(it).delete() }
    }

    private fun decode(source: Uri): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Unreadable image")
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_EDGE_PX) sampleSize *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Unreadable image")
        val rotation = resolver.openInputStream(source)?.use { ExifInterface(it).rotationDegrees } ?: 0
        val scale = minOf(1f, MAX_EDGE_PX.toFloat() / maxOf(decoded.width, decoded.height))
        if (scale == 1f && rotation == 0) return decoded
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            .also { if (it !== decoded) decoded.recycle() }
    }

    private fun compress(bitmap: Bitmap): ByteArray {
        var quality = JPEG_QUALITY
        while (true) {
            val bytes = ByteArrayOutputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                output.toByteArray()
            }
            if (bytes.size <= MAX_BYTES || quality <= MIN_JPEG_QUALITY) return bytes
            quality -= 10
        }
    }

    private companion object {
        const val MAX_EDGE_PX = 1920
        const val JPEG_QUALITY = 80
        const val MIN_JPEG_QUALITY = 40
        const val MAX_BYTES = 2 * 1024 * 1024
    }
}
