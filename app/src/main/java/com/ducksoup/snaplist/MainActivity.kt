package com.ducksoup.snaplist

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.menu_settings -> {
                navigateTo("settings")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun navigateTo(target: String) {
        val targetClass = when (target) {
            "settings" -> SettingsActivity::class.java
            else -> error("Invalid navigation target")
        }
        val intent = Intent(this, targetClass).apply{

        }
        startActivity(intent)
    }
}

