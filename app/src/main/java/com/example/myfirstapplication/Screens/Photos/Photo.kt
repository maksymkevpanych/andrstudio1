package com.example.myfirstapplication.Screens.Photos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photo_table")
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uri: String
)
