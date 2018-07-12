package org.mozilla.scryer.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class ScreenCaptureManager(context: Context, private val screenCapturePermissionIntent: Intent) {
    private val mProjectionManager: MediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private var mMediaProjection: MediaProjection? = null
    private var mImageReader: ImageReader? = null
    private val mHandler: Handler
    private var mVirtualDisplay: VirtualDisplay? = null
    private val mDensity: Int
    private val mWidth: Int
    private val mHeight: Int
    private val screenshotPath: String

    init {
        val screenshotDirectory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "screenshot+")
        ensureDir(screenshotDirectory)
        screenshotPath = screenshotDirectory.absolutePath

        val metrics = context.resources.displayMetrics
        mDensity = metrics.densityDpi
        mWidth = metrics.widthPixels
        mHeight = metrics.heightPixels

        val handlerThread = HandlerThread("ScreenCaptureThread")
        handlerThread.start()
        val looper = handlerThread.looper
        mHandler = Handler(looper)
    }

    fun captureScreen() {
        startProjection()
    }

    private fun startProjection() {
        mMediaProjection = mProjectionManager.getMediaProjection(Activity.RESULT_OK, screenCapturePermissionIntent)
        createVirtualDisplay()
        // register media projection stop callback
        mMediaProjection?.registerCallback(MediaProjectionStopCallback(), mHandler)
    }

    private fun stopProjection() {
        mHandler.post {
            mMediaProjection?.stop()
        }
    }

    private fun createVirtualDisplay() {
        // start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 1)
        mVirtualDisplay = mMediaProjection?.createVirtualDisplay("screen-capture",
                mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader?.surface, null, mHandler)
        mImageReader!!.setOnImageAvailableListener(ImageAvailableListener(), mHandler)
    }

    private fun ensureDir(dir: File): Boolean {
        return if (dir.mkdirs()) {
            true
        } else {
            dir.exists() && dir.isDirectory && dir.canWrite()
        }
    }

    private inner class ImageAvailableListener : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            mImageReader!!.setOnImageAvailableListener(null, null)

            var image: Image? = null
            var fos: FileOutputStream? = null
            var bitmap: Bitmap? = null

            try {
                image = reader.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * mWidth

                    // create bitmap
                    bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888)
                    bitmap?.copyPixelsFromBuffer(buffer)

                    // write bitmap to a file
                    fos = FileOutputStream(screenshotPath + "/my_screenshot_" + Calendar.getInstance().timeInMillis + ".png")
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (fos != null) {
                    try {
                        fos.close()
                    } catch (ioe: IOException) {
                        ioe.printStackTrace()
                    }

                }

                bitmap?.recycle()
                image?.close()
            }

            stopProjection()
        }
    }

    private inner class MediaProjectionStopCallback : MediaProjection.Callback() {
        override fun onStop() {
            mHandler.post {
                mVirtualDisplay?.release()
                mImageReader?.setOnImageAvailableListener(null, null)
                mMediaProjection?.unregisterCallback(this@MediaProjectionStopCallback)
            }
        }
    }

    companion object {
        private const val VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
    }

}
