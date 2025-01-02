package com.example.ftp.ui

fun Float.format(digits: Int): String {
    return "%.${digits}f".format(this)
}