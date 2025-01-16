package com.emoji.ftp.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.emoji.ftp.room.bean.FileTrack
import com.emoji.ftp.room.bean.FileTrackDao

@Database(entities = [FileTrack::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileTrackDao(): FileTrackDao
}
