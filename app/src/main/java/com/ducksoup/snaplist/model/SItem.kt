package com.ducksoup.snaplist.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class SItem(
    var label: String,
    var checked: Boolean,
    val listId: Int,
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
)