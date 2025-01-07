package com.example.ftp.ui

fun Float.format(digits: Int): String {
    return "%.${digits}f".format(this)
}

fun Long.toReadableFileSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(this.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.2f %s", this / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
