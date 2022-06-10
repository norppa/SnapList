package com.ducksoup.snaplist.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lists")
class SList (
    val name: String,
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
) {
    override fun toString(): String {
        return "{ id: ${this.id}, name: ${this.name} }"
    }
}