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

class ScreenCaptureManager(context: Context, private val screenCapturePermissionIntent: Intent, private val screenCaptureListener: ScreenCaptureListener) {
    private val projectionManager: MediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private val workerHandler: Handler
    private val uiHandler: Handler
    private var virtualDisplay: VirtualDisplay? = null
    private val density: Int
    private val width: Int
    private val height: Int
    private val screenshotPath: String

    init {
        val screenshotDirectory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "screenshot+")
        ensureDir(screenshotDirectory)
        screenshotPath = screenshotDirectory.absolutePath

        val metrics = context.resources.displayMetrics
        density = metrics.densityDpi
        width = metrics.widthPixels
        height = metrics.heightPixels

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
        mediaProjection = projectionManager.getMediaProjection(Activity.RESULT_OK, screenCapturePermissionIntent)
        createVirtualDisplay()
        // register media projection stop callback
        mediaProjection?.registerCallback(MediaProjectionStopCallback(), workerHandler)
    }

    private fun stopProjection() {
        workerHandler.post {
            mediaProjection?.stop()
        }
    }

    private fun createVirtualDisplay() {
        // start capture reader
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1)
        virtualDisplay = mediaProjection?.createVirtualDisplay("screen-capture",
                width, height, density, VIRTUAL_DISPLAY_FLAGS, imageReader?.surface, null, workerHandler)
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

            var image: Image? = null
            var fos: FileOutputStream? = null
            var bitmap: Bitmap? = null
            var filePath: String? = null

            try {
                image = reader.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width

                    // create bitmap
                    bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                    bitmap?.copyPixelsFromBuffer(buffer)

                    // write bitmap to a file
                    filePath = screenshotPath + "/my_screenshot_" + System.currentTimeMillis() + ".png"
                    fos = FileOutputStream(filePath)
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

                stopProjection()
            }

            uiHandler.post { screenCaptureListener.onScreenShotTaken(filePath ?: "") }
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

    companion object {
        private const val VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
    }

}
