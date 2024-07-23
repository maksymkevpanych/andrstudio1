package com.example.myfirstapplication
import android.os.Parcelable



data class
Todo(
    val id: Int,
    val title: String,
    var isChecked: Boolean
)
