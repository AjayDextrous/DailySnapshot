package com.example.dailysnapshot.util

import android.graphics.Bitmap
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class BitmapUtilsTest {

    @Before
    fun setUp() {
        mockkStatic(Bitmap::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Bitmap::class)
    }

    @Test
    fun centerCrop_squareBitmap_returnsSameInstance() {
        val bitmap = mockk<Bitmap>(relaxed = true) {
            every { width } returns 100
            every { height } returns 100
        }

        val result = centerCrop(bitmap)

        assertSame(bitmap, result)
        verify(exactly = 0) { bitmap.recycle() }
    }

    @Test
    fun centerCrop_landscapeBitmap_cropsToSquareAlongHeight() {
        // 200×100 → side = 100, x = (200-100)/2 = 50, y = 0
        val bitmap = mockk<Bitmap>(relaxed = true) {
            every { width } returns 200
            every { height } returns 100
        }
        val cropped = mockk<Bitmap>(relaxed = true)
        every { Bitmap.createBitmap(bitmap, 50, 0, 100, 100) } returns cropped

        val result = centerCrop(bitmap)

        assertSame(cropped, result)
        verify(exactly = 1) { Bitmap.createBitmap(bitmap, 50, 0, 100, 100) }
    }

    @Test
    fun centerCrop_portraitBitmap_cropsToSquareAlongWidth() {
        // 100×200 → side = 100, x = 0, y = (200-100)/2 = 50
        val bitmap = mockk<Bitmap>(relaxed = true) {
            every { width } returns 100
            every { height } returns 200
        }
        val cropped = mockk<Bitmap>(relaxed = true)
        every { Bitmap.createBitmap(bitmap, 0, 50, 100, 100) } returns cropped

        val result = centerCrop(bitmap)

        assertSame(cropped, result)
        verify(exactly = 1) { Bitmap.createBitmap(bitmap, 0, 50, 100, 100) }
    }

    @Test
    fun centerCrop_recyclesSourceBitmap_whenNewBitmapCreated() {
        val bitmap = mockk<Bitmap>(relaxed = true) {
            every { width } returns 300
            every { height } returns 200
        }
        val cropped = mockk<Bitmap>(relaxed = true)
        every { Bitmap.createBitmap(bitmap, 50, 0, 200, 200) } returns cropped

        centerCrop(bitmap)

        verify(exactly = 1) { bitmap.recycle() }
    }
}
