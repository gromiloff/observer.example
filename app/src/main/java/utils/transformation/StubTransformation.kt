package utils.transformation

import android.graphics.Bitmap
import android.graphics.Canvas
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

abstract class StubTransformation : BitmapTransformation() {
    private val id = javaClass.canonicalName
    private val bytes = id!!.toByteArray(Key.CHARSET)

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(bytes)
    }

    override fun hashCode() = this.id!!.hashCode()

    fun createBitmapFromPool(pool: BitmapPool, maybeAlphaSafe: Bitmap?, outWidth: Int, outHeight: Int): Bitmap {
        var argbBitmap = maybeAlphaSafe
        if (maybeAlphaSafe == null || Bitmap.Config.ARGB_8888 != maybeAlphaSafe.config) {
            argbBitmap = pool.get(outWidth, outHeight, Bitmap.Config.ARGB_8888)
            if (maybeAlphaSafe != null) {
                Canvas(argbBitmap).drawBitmap(maybeAlphaSafe, 0f, 0f, null)
            }
        }
        return argbBitmap!!

    }
}
