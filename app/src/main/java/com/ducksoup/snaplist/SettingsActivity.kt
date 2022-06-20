package com.ducksoup.snaplist

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {
    private lateinit var delChkSwitch: SwitchMaterial
    private lateinit var delAllSwitch: SwitchMaterial
    private lateinit var delListSwitch: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        delChkSwitch = findViewById(R.id.whenDeletingCheckedItemsSwitch)
        delAllSwitch = findViewById(R.id.whenDeletingAllItemsSwitch)
        delListSwitch = findViewById(R.id.whenDeletingListSwitch)

        delChkSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setConfirmDelChkItems(isChecked)
        }
        delAllSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setConfirmDelAllItems(isChecked)
        }
        delListSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setConfirmDelList(isChecked)
        }

        delChkSwitch.isChecked = Prefs.confirmDelChkItems()
        delAllSwitch.isChecked = Prefs.confirmDelAllItems()
        delListSwitch.isChecked = Prefs.confirmDelList()
        println("setting switches ${Prefs.confirmDelList()}")
    }
}