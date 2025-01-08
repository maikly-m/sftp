package com.example.ftp.ui

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider

fun Float.format(digits: Int): String {
    return "%.${digits}f".format(this)
}

fun Long.toReadableFileSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(this.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.2f %s", this / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun View.setRoundedCorners(cornerRadius: Float) {
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
        }
    }
    clipToOutline = true
}
