package com.example.dailysnapshot.util

import android.graphics.Bitmap

/**
 * Crops [bitmap] to a centered square using the shorter dimension.
 * The source bitmap is recycled if a new bitmap had to be created.
 *
 * TODO DAI-37: callers in EditViewModel will be replaced by the user's explicit crop once
 * the crop screen is implemented; this utility can remain for other use cases.
 */
internal fun centerCrop(bitmap: Bitmap): Bitmap {
    val side = minOf(bitmap.width, bitmap.height)
    if (bitmap.width == side && bitmap.height == side) return bitmap
    val x = (bitmap.width - side) / 2
    val y = (bitmap.height - side) / 2
    val cropped = Bitmap.createBitmap(bitmap, x, y, side, side)
    bitmap.recycle()
    return cropped
}
