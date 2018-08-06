package org.mozilla.scryer.filemonitor

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import org.mozilla.scryer.persistence.CollectionModel
import org.mozilla.scryer.persistence.ScreenshotModel
import java.io.File
import java.util.concurrent.Executors

class ScreenshotFetcher {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun fetchScreenshots(context: Context, callback: (List<ScreenshotModel>) -> Unit) {
        executor.execute {
            val folders = getFolders(context)
            val screenshots = mutableListOf<ScreenshotModel>()
            folders.forEach { folderPath ->
                screenshots.addAll(fetchScreenshots(folderPath))
            }
            mainHandler.post {
                callback(screenshots)
            }
        }
    }

    private fun getFolders(context: Context): List<String> {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val columns = arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)
        val selection = "${MediaStore.Images.ImageColumns.BUCKET_ID} IS NOT NULL) GROUP BY (${MediaStore.Images.ImageColumns.BUCKET_ID}"
        val results = mutableListOf<String>()

        val cursor = context.contentResolver.query(uri, columns, selection, null, null)
        cursor.use {
            while (cursor.moveToNext()) {
                val path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)).trim()
                if (path.contains("screenshot", true)) {
                    val folder = File(path).parent?.trimEnd(File.separatorChar) ?: continue
                    results.add(folder)
                }
            }
        }

        return results
    }

    private fun fetchScreenshots(dirPath: String): List<ScreenshotModel> {
        val files = File(dirPath).listFiles()
        val results = mutableListOf<ScreenshotModel>()

        for (file in files) {
            val model = ScreenshotModel(null, file.absolutePath, file.lastModified(), CollectionModel.CATEGORY_NONE)
            results.add(model)
        }
        return results
    }
}