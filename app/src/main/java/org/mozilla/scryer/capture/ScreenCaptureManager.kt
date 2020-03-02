package org.mozilla.scryer.capture

import android.app.Activity
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import java.io.File
import java.io.FileOutputStream

class ScreenCaptureManager(context: Context, private val screenCapturePermissionIntent: Intent, private val screenCaptureListener: ScreenCaptureListener) {
    companion object {
        const val SCREENSHOT_DIR = "ScreenshotGo"
    }

    private val projectionManager: MediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private val workerHandler: Handler
    private val uiHandler: Handler
    private var defaultDisplay: Display
    private var virtualDisplay: VirtualDisplay? = null
    private val metrics: DisplayMetrics = DisplayMetrics()
    private val density: Int
    private var width = 0
    private var height = 0
    private val screenshotPath: String

    init {
        val screenshotDirectory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), SCREENSHOT_DIR)
        ensureDir(screenshotDirectory)
        screenshotPath = screenshotDirectory.absolutePath

        val windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
        defaultDisplay = windowManager.defaultDisplay
        defaultDisplay.getMetrics(metrics)
        density = metrics.densityDpi

        val handlerThread = HandlerThread("ScreenCaptureThread")
        handlerThread.start()
        val looper = handlerThread.looper
        workerHandler = Handler(looper)

        uiHandler = Handler()
    }

    fun captureScreen() {
        startProjection()
    }

    private fun startProjection() {
        uiHandler.post {
            try {
                mediaProjection = projectionManager.getMediaProjection(Activity.RESULT_OK, screenCapturePermissionIntent)
            } catch (exception: IllegalStateException) {
                // There is no hint from MediaProjectionManager to know if there is already a
                // MediaProjection instance running. So, just catch the exception and skip the capture.
                return@post
            }
            createVirtualDisplay()
            // register media projection stop callback
            mediaProjection?.registerCallback(MediaProjectionStopCallback(), workerHandler)
        }
    }

    private fun stopProjection() {
        workerHandler.post {
            mediaProjection?.stop()
        }
    }

    private fun createVirtualDisplay() {
        val size = Point()
        defaultDisplay.getRealSize(size)
        width = size.x
        height = size.y

        // start capture reader
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay("screen-capture",
                width, height, density, DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION, imageReader?.surface, null, workerHandler)
        imageReader?.setOnImageAvailableListener(ImageAvailableListener(), workerHandler)
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
            imageReader?.setOnImageAvailableListener(null, null)

            val filePath: String = screenshotPath + "/Screenshot_" + System.currentTimeMillis() + ".jpg"
            var bitmap: Bitmap? = null
            var croppedBitmap: Bitmap? = null

            try {
                reader.acquireLatestImage()?.use { image ->
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width

                    // create bitmap
                    bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                    bitmap?.let {
                        it.copyPixelsFromBuffer(buffer)
                        // trim the screenshot to the correct size.
                        croppedBitmap = Bitmap.createBitmap(it, 0, 0, width, height)
                    }

                    croppedBitmap?.let {
                        // write bitmap to a file
                        FileOutputStream(filePath).use { fileOutputStream ->
                            it.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                croppedBitmap?.recycle()
                bitmap?.recycle()

                stopProjection()
            }

            uiHandler.post { screenCaptureListener.onScreenShotTaken(filePath) }
        }
    }

    private inner class MediaProjectionStopCallback : MediaProjection.Callback() {
        override fun onStop() {
            workerHandler.post {
                virtualDisplay?.release()
                imageReader?.setOnImageAvailableListener(null, null)
                mediaProjection?.unregisterCallback(this@MediaProjectionStopCallback)
            }
        }
    }
}
