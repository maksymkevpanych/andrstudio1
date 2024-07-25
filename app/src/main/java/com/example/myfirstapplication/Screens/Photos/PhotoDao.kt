package com.example.myfirstapplication.Screens.Photos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PhotoDao {
    @Insert
    suspend fun insert(photo: Photo)

    @Query("SELECT * FROM photo_table ORDER BY id DESC LIMIT 1")
    suspend fun getLastPhoto(): Photo?
}
