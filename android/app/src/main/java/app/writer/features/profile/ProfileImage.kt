package app.writer.features.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

/** The optional profile photo: one small square JPEG in app-private storage, never uploaded anywhere. */
object ProfileImage {
    private const val SIZE = 512

    fun file(context: Context) = File(context.filesDir, "profile.jpg")

    suspend fun load(context: Context): Bitmap? = withContext(Dispatchers.IO) {
        val f = file(context)
        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
    }

    /** Reads the picked image, crops it to a square, shrinks it and stores it. Returns false on any failure. */
    suspend fun save(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val source = decode(context, uri) ?: return@withContext false
            val side = min(source.width, source.height)
            val square = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
            val scaled = if (side > SIZE) Bitmap.createScaledBitmap(square, SIZE, SIZE, true) else square
            val target = file(context)
            val temp = File(target.parentFile, "profile.jpg.part")
            temp.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            if (target.exists()) target.delete()
            temp.renameTo(target)
        } catch (e: Exception) {
            false
        }
    }

    fun delete(context: Context) {
        runCatching { file(context).delete() }
    }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= 28) {
            // ImageDecoder also applies the photo's rotation.
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = max(info.size.width, info.size.height)
                decoder.setTargetSampleSize(max(1, longest / (SIZE * 2)))
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sample = max(1, max(bounds.outWidth, bounds.outHeight) / (SIZE * 2))
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }
}
