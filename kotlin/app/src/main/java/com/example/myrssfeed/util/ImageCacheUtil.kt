package com.example.myrssfeed.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object ImageCacheUtil {
    private fun getCacheDir(context: Context): File {
        val dir = File(context.cacheDir, "widget_thumbnails")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getCachedImageFile(context: Context, imageUrl: String): File {
        val fileName = imageUrl.hashCode().toString() + ".jpg"
        return File(getCacheDir(context), fileName)
    }

    fun getCachedBitmap(context: Context, imageUrl: String): Bitmap? {
        val file = getCachedImageFile(context, imageUrl)
        return if (file.exists()) {
            android.util.Log.d("ImageCache", "Found cached image: $imageUrl")
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            android.util.Log.d("ImageCache", "No cached image found: $imageUrl")
            null
        }
    }

    fun cacheImageFromUrl(context: Context, imageUrl: String): Bitmap? {
        return try {
            android.util.Log.d("ImageCache", "Downloading image: $imageUrl")
            val url = URL(imageUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            
            val bmp = BitmapFactory.decodeStream(connection.getInputStream())
            if (bmp != null) {
                val file = getCachedImageFile(context, imageUrl)
                FileOutputStream(file).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                android.util.Log.d("ImageCache", "Successfully cached image: $imageUrl")
                bmp
            } else {
                android.util.Log.w("ImageCache", "Failed to decode image: $imageUrl")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageCache", "Error downloading image: $imageUrl", e)
            null
        }
    }
} 