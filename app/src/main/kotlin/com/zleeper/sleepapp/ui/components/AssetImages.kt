package com.zleeper.sleepapp.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object AssetImageCache {
    private val images = object : LruCache<String, ImageBitmap>(48 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4 / 1024
    }

    @Synchronized fun get(path: String): ImageBitmap? = images.get(path)
    @Synchronized fun put(path: String, image: ImageBitmap) { images.put(path, image) }
}

@Composable
fun rememberAssetImage(path: String): ImageBitmap? {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = AssetImageCache.get(path), key1 = path) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                AssetImageCache.get(path) ?: context.assets.open(path).use { stream ->
                    requireNotNull(BitmapFactory.decodeStream(stream)) { "Unable to decode asset: $path" }.asImageBitmap()
                        .also { AssetImageCache.put(path, it) }
                }
            }
        }
    }.value
}
