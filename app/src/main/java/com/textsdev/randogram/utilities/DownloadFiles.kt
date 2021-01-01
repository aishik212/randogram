package com.textsdev.randogram.utilities

import android.app.DownloadManager
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.ParcelFileDescriptor
import android.util.Log
import com.textsdev.randogram.adapters.photoViewerActivity
import java.io.File
import java.io.FileOutputStream


class DownloadFiles {
    companion object {
        fun downloadToDownload(filename: String?, activity: photoViewerActivity, cacheDir: File) {
/*
            FirebaseStorage.getInstance()
                .getReference(filename.toString()).downloadUrl.addOnSuccessListener {
                    normalDownload(it, f, activity)
                }
*/

            val file = File("${cacheDir}/${filename}")
            var externName: File
            val s = "${activity.getExternalFilesDir(DIRECTORY_DOWNLOADS)}/Randogram_image"
            externName = File(("$s${file.name}"))
            var i = 0
            Log.d(
                "texts",
                "downloadToDownload: " + externName.absolutePath + "\n" + externName.exists()
            )
            while (externName.exists()) {
                externName =
                    File(("$s${i}${file.name}"))
                i++
                Log.d("texts", "downloadToDownload: $externName")
            }
            createFile(activity, externName)
        }

        private fun normalDownload(
            it: Uri?,
            f: String,
            activity: photoViewerActivity
        ) {
            if (it != null) {
                try {
                    val r: DownloadManager.Request = DownloadManager.Request(it)
                    r.setDestinationInExternalPublicDir(
                        DIRECTORY_DOWNLOADS,
                        f
                    )
                    r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    val downloadManager =
                        activity.getSystemService(DOWNLOAD_SERVICE) as DownloadManager?
                    downloadManager?.enqueue(r)
                } catch (e: Exception) {
                    val i = Intent(Intent.ACTION_VIEW, Uri.parse(it.toString()))
                    activity.startActivity(i)
                }
            }

        }

        const val CREATE_FILE = 1
        private fun createFile(photoViewer: photoViewerActivity, externName: File) {
            val name = externName.name.replace(
                "Randogram_image",
                "Randogram_image${System.currentTimeMillis()}"
            )
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/" + externName.extension
                putExtra(Intent.EXTRA_TITLE, name)
            }
            photoViewer.startActivityForResult(intent, CREATE_FILE)
        }

        fun writeFileContent(uri: Uri, activity: photoViewerActivity, file: File?) {
            if (file != null) {
                try {
                    val pfd: ParcelFileDescriptor? =
                        activity.contentResolver.openFileDescriptor(uri, "w")
                    val fileOutputStream = FileOutputStream(
                        pfd?.fileDescriptor
                    )
                    fileOutputStream.write(file.readBytes())
                    fileOutputStream.close()
                    pfd?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }
    }
}