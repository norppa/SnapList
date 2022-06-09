package com.ducksoup.snaplist

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.viewModels
import com.ducksoup.snaplist.model.SList
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val database by lazy { SnapListDatabase.getDatabase(this) }

    private val listViewModel: ListViewModel by viewModels {
        ListViewModelFactory(database.listDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        generateBogusData()

        listViewModel.allLists.observe(this) { lists ->
            lists?.let { setTabs(tabLayout, it.map { list -> list.name }) }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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
        val intent = Intent(this, targetClass).apply {

        }
        startActivity(intent)
    }

    private fun setTabs(tabLayout: TabLayout, tabList: List<String>) {
        tabList.forEach { tabLayout.addTab(tabLayout.newTab().setText(it)) }
    }

    private fun generateBogusData() {
        CoroutineScope(SupervisorJob()).launch {
            val dao = database.listDao()
            dao.deleteAll()
            dao.insert(SList("todo"))
            dao.insert(SList("shop"))
        }
    }
}





