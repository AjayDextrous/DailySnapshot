package com.example.dailysnapshot.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.example.dailysnapshot.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles Polaroid frame compositing for saved snapshot images.
 *
 * Frame proportions (classic Polaroid):
 *  - Left / Right / Top border: [SIDE_MARGIN_RATIO] × photo width
 *  - Bottom margin:             [BOTTOM_MARGIN_RATIO] × photo height
 *
 * All methods are CPU-bound; callers must dispatch to a background thread.
 */
@Singleton
class ImageProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val captionTypeface: Typeface? by lazy {
        ResourcesCompat.getFont(context, R.font.special_elite)
    }

    /**
     * Composites [rawBitmap] into a Polaroid frame and writes the result as a JPEG to [outputFile].
     *
     * @param rawBitmap   Source photo bitmap (must already be correctly oriented — no EXIF applied here).
     * @param caption     Caption text to render in the bottom margin. Empty string = no text.
     * @param filterId    One of "sepia", "faded", "noir", "warm", "cool", or null / "none" for no filter.
     * @param outputFile  Destination file; parent directories are created if missing.
     */
    fun compositePolaroid(
        rawBitmap: Bitmap,
        caption: String,
        filterId: String?,
        outputFile: File
    ) {
        val photoW = rawBitmap.width
        val photoH = rawBitmap.height

        val sideMargin   = (photoW * SIDE_MARGIN_RATIO).toInt()
        val bottomMargin = (photoH * BOTTOM_MARGIN_RATIO).toInt()

        val frameW = photoW + 2 * sideMargin
        val frameH = photoH + sideMargin + bottomMargin   // top margin equals sideMargin

        val frameBitmap = createBitmap(frameW, frameH, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(frameBitmap)

            // Near-white paper background (#FAFAF8) — mimics aged Polaroid paper
            canvas.drawColor(android.graphics.Color.parseColor("#FAFAF8"))

            // Photo with optional ColorMatrix filter
            val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                val matrix = colorMatrix(filterId)
                if (matrix != null) colorFilter = ColorMatrixColorFilter(matrix)
            }
            val srcRect = Rect(0, 0, photoW, photoH)
            val dstRect = Rect(sideMargin, sideMargin, sideMargin + photoW, sideMargin + photoH)
            canvas.drawBitmap(rawBitmap, srcRect, dstRect, photoPaint)

            // Caption text centred in the bottom margin, max 2 lines with ellipsis
            if (caption.isNotEmpty()) {
                val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#333333")
                    textSize = frameW * CAPTION_TEXT_SIZE_RATIO
                    typeface = captionTypeface ?: Typeface.MONOSPACE
                }
                val maxTextWidth = (frameW * 0.85f).toInt()
                val layout = StaticLayout.Builder
                    .obtain(caption, 0, caption.length, textPaint, maxTextWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setMaxLines(2)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()
                val captionAreaTop = (sideMargin + photoH).toFloat()
                val textTop = captionAreaTop + (bottomMargin - layout.height) / 2f
                val textLeft = (frameW - maxTextWidth) / 2f
                canvas.save()
                canvas.translate(textLeft, textTop)
                layout.draw(canvas)
                canvas.restore()
            }

            outputFile.parentFile?.mkdirs()
            outputFile.outputStream().use { frameBitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        } finally {
            frameBitmap.recycle()
        }
    }

    /** Returns the [ColorMatrix] for [filterId], or null for no-op (null / "none"). */
    private fun colorMatrix(filterId: String?): ColorMatrix? = when (filterId) {
        "sepia" -> ColorMatrix(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f,     0f,     0f,     1f, 0f
        ))
        "faded" -> ColorMatrix(floatArrayOf(
            0.7f,  0.1f,  0.1f,  0f, 40f,
            0.1f,  0.7f,  0.1f,  0f, 40f,
            0.1f,  0.1f,  0.7f,  0f, 50f,
            0f,    0f,    0f,    1f, 0f
        ))
        "noir" -> ColorMatrix(floatArrayOf(
            0.419f, 0.822f, 0.160f, 0f, -51f,
            0.419f, 0.822f, 0.160f, 0f, -51f,
            0.419f, 0.822f, 0.160f, 0f, -51f,
            0f,     0f,     0f,     1f, 0f
        ))
        "warm" -> ColorMatrix(floatArrayOf(
            1.2f, 0f,   0f,   0f,  10f,
            0f,   1.0f, 0f,   0f,  5f,
            0f,   0f,   0.8f, 0f, -10f,
            0f,   0f,   0f,   1f,  0f
        ))
        "cool" -> ColorMatrix(floatArrayOf(
            0.8f, 0f,   0f,   0f, -10f,
            0f,   1.0f, 0f,   0f,  5f,
            0f,   0f,   1.2f, 0f,  15f,
            0f,   0f,   0f,   1f,  0f
        ))
        else -> null  // "none" or null → no transformation
    }

    companion object {
        private const val SIDE_MARGIN_RATIO        = 0.06f
        private const val BOTTOM_MARGIN_RATIO      = 0.23f
        private const val CAPTION_TEXT_SIZE_RATIO  = 0.035f
    }
}
