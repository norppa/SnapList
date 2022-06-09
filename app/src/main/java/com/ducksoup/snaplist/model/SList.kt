package com.ducksoup.snaplist.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lists")
class SList (
    @PrimaryKey val name: String
)
