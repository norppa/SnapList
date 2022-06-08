package com.ducksoup.snaplist

data class SItem(
    val label: String,
    val checked: Boolean
)

data class SList(
    val name: String,
    val items: List<SItem>
)