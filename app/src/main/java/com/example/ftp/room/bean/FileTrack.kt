package com.example.ftp.room.bean

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fileTrack")
data class FileTrack(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val name: String,
    val size: Long,
    val mTime: Long,
    var path: String,
)
