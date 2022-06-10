package com.ducksoup.snaplist.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "choices")
data class SChoice (
    @PrimaryKey val id: Int = 0,
    val selectedList: Int
)