package com.example.ocr.ui.utils

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.ocr.R
import java.io.File

const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

class UIUtils {
    companion object {
        //Function to set the status bar color to transparent.
        fun setStatusBarTransparent(activity: Activity, view: View) {
            activity.apply {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = ContextCompat.getColor(this, R.color.transparent)
                WindowCompat.setDecorFitsSystemWindows(window, false)
                ViewCompat.setOnApplyWindowInsetsListener(view) { root, windowInset ->
                    val inset = windowInset.getInsets(WindowInsetsCompat.Type.systemBars())
                    root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        leftMargin = inset.left
                        bottomMargin = inset.bottom
                        rightMargin = inset.right
                    }
                    WindowInsetsCompat.CONSUMED
                }
            }
        }

        fun setStatusBarShown(activity: Activity, view: View) {
            activity.apply {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = ContextCompat.getColor(this, R.color.transparent)
                WindowCompat.setDecorFitsSystemWindows(window, false)
                ViewCompat.setOnApplyWindowInsetsListener(view) { root, windowInset ->
                    val inset = windowInset.getInsets(WindowInsetsCompat.Type.systemBars())
                    val inset1 = windowInset.getInsets(WindowInsetsCompat.Type.statusBars())
                    root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        leftMargin = inset.left
                        bottomMargin = inset.bottom
                        topMargin = inset1.top
                        rightMargin = inset.right
                    }
                    WindowInsetsCompat.CONSUMED
                }
            }
        }

        fun getFileFromUri(context: Context, uri: Uri?): File? {
            uri ?: return null
            uri.path ?: return null

            var newUriString = uri.toString()
            newUriString = newUriString.replace(
                "content://com.android.providers.downloads.documents/",
                "content://com.android.providers.media.documents/"
            )
            newUriString = newUriString.replace(
                "/msf%3A", "/image%3A"
            )
            val newUri = Uri.parse(newUriString)

            var realPath = String()
            val databaseUri: Uri
            val selection: String?
            val selectionArgs: Array<String>?
            if (newUri.path?.contains("/document/image:") == true) {
                databaseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                selection = "_id=?"
                selectionArgs = arrayOf(DocumentsContract.getDocumentId(newUri).split(":")[1])
            } else {
                databaseUri = newUri
                selection = null
                selectionArgs = null
            }
            try {
                val column = "_data"
                val projection = arrayOf(column)
                val cursor = context.contentResolver.query(
                    databaseUri,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )
                cursor?.let {
                    if (it.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndexOrThrow(column)
                        realPath = cursor.getString(columnIndex)
                    }
                    cursor.close()
                }
            } catch (e: Exception) {
                Log.i("GetFileUri Exception:", e.message ?: "")
            }
            val path = realPath.ifEmpty {
                when {
                    newUri.path?.contains("/document/raw:") == true -> newUri.path?.replace(
                        "/document/raw:",
                        ""
                    )
                    newUri.path?.contains("/document/primary:") == true -> newUri.path?.replace(
                        "/document/primary:",
                        "/storage/emulated/0/"
                    )
                    else -> return null
                }
            }
            return if (path.isNullOrEmpty()) null else File(path)
        }
    }
}