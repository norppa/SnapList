package com.ducksoup.snaplist

import android.app.Activity
import android.content.Context.MODE_PRIVATE

import android.content.SharedPreferences

object Prefs {

    private lateinit var sharedPreferences: SharedPreferences
    private var selectedListId: Int = -1

    fun init(activity: Activity) {
        sharedPreferences = activity.getPreferences(MODE_PRIVATE)
    }

    fun getSelectedList(): Int {
        if (selectedListId < 0) {
            selectedListId = sharedPreferences.getInt("selectedList", -1)
        }
        return selectedListId
    }

    fun setSelectedList(selectedListId: Int) {
        with(sharedPreferences.edit()) {
            putInt("selectedList", selectedListId)
            apply()
        }
    }

    fun delSelectedList() {
        with(sharedPreferences.edit()) {
            remove("selectedList")
            apply()
        }
    }
}