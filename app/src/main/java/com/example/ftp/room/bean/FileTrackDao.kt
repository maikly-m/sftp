package com.example.ftp.room.bean

import androidx.room.*

@Dao
interface FileTrackDao {
    @Insert
    suspend fun insert(fileTrack: FileTrack): Long

    @Query("SELECT * FROM filetrack")
    suspend fun getAll(): List<FileTrack>

    @Query("SELECT * FROM filetrack WHERE id = :id")
    suspend fun getById(id: Long): FileTrack?

    @Query("SELECT * FROM filetrack WHERE type = :type")
    suspend fun getByType(type: String): List<FileTrack>?

    @Update
    suspend fun update(fileTrack: FileTrack)

    @Delete
    suspend fun delete(fileTrack: FileTrack)

    @Query("DELETE FROM filetrack")
    suspend fun deleteAll()
}
