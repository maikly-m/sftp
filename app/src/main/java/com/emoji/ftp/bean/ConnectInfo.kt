package com.emoji.ftp.bean

data class ConnectInfo(
    val ip: String,
    val port: Int,
    val name: String,
    val pw: String
)