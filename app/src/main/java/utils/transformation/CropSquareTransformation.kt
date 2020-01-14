@file:Suppress("unused")

package utils.transformation

import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool

class CropSquareTransformation : StubTransformation() {
    override fun transform(pool: BitmapPool, inBitmap: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val toTransform = createBitmapFromPool(pool, inBitmap, outWidth, outHeight)
        val size = Math.min(outWidth, outHeight)
        return Bitmap.createBitmap(toTransform, (outWidth - size) / 2, (outHeight - size) / 2, size, size)
    }
}
