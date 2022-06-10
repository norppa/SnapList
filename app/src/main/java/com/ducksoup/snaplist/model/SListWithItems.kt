package com.ducksoup.snaplist.model

import androidx.room.Embedded
import androidx.room.Relation

data class SListWithItems(
    @Embedded val list: SList,

    @Relation(parentColumn = "id", entityColumn = "listId")
    val items: List<SItem>
)