package com.example.ocr.UI.Utils

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.ocr.R

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
    }
}