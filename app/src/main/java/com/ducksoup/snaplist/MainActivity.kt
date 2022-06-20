package com.ducksoup.snaplist

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.MenuCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ducksoup.snaplist.model.SItem
import com.ducksoup.snaplist.model.SList
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class MainActivity : AppCompatActivity() {

    private val database by lazy { SnapListDatabase.getDatabase(this) }
    private val dao by lazy { database.dao() }
    private val coroutineScope = CoroutineScope(SupervisorJob())

    private lateinit var initialView: LinearLayout
    private lateinit var mainView: ConstraintLayout
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var input: EditText
    private lateinit var submit: FloatingActionButton
    private lateinit var adapter: MainListAdapter
    private lateinit var initialListButton: Button

    private var items = mutableListOf<SItem>()
    private var selectedListId = -1

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Prefs.init(this)

        mainView = findViewById(R.id.mainView)
        initialView = findViewById(R.id.initialView)

        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.recyclerView)
        input = findViewById(R.id.input)
        submit = findViewById(R.id.submitInput)

        initialListButton = findViewById(R.id.initialListButton)

        adapter = MainListAdapter(items, dao, applicationContext)
        recyclerView.layoutManager = LinearLayoutManager(applicationContext)
        recyclerView.adapter = adapter

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedListId = tab?.id ?: error("Missing tab id")
                runBlocking {
                    items.clear()
                    items.addAll(dao.getItems(selectedListId))
                    runOnUiThread { adapter.notifyDataSetChanged() }
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
        })

        submit.setOnClickListener {
            val item = SItem(input.text.toString(), false, selectedListId)
            coroutineScope.launch { dao.insertItem(item) }
            items.add(item)
            adapter.notifyItemInserted(items.size - 1)
            input.setText("")
        }

        initialListButton.setOnClickListener { openAddListDialog(true) }

        val lists = runBlocking { dao.getLists() }

        if (lists.isEmpty()) {
            loadInitialView()
        } else {
            lists.forEach {
                tabLayout.addTab(tabLayout.newTab().setId(it.id).setText(it.name))
            }
            selectedListId = Prefs.getSelectedList()
            if (selectedListId < 0 || lists.none { it.id == selectedListId }) {
                Prefs.setSelectedList(lists[0].id)
            }

            focusSelectedTab()
            val listItems = runBlocking { dao.getItems(selectedListId) }
            items.clear()
            items.addAll(listItems)
            adapter.notifyDataSetChanged()
        }
    }

    override fun onStop() {
        super.onStop()
        if (tabLayout.tabCount == 0) {
            Prefs.delSelectedList()
        } else {
            Prefs.setSelectedList(selectedListId)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        if (menu != null) MenuCompat.setGroupDividerEnabled(menu, true)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.del_all_items -> {
                if (Prefs.confirmDelAllItems()) openDelAllItemsDialog() else delAllItems()
                true
            }
            R.id.del_chk_items -> {
                if (Prefs.confirmDelChkItems()) openDelChkItemsDialog() else delChkItems()
                true
            }
            R.id.add_list -> {
                openAddListDialog()
                true
            }
            R.id.del_list -> {
                if (Prefs.confirmDelList()) openDelListDialog() else delList()
                true
            }
            R.id.menu_settings -> {
                val intent = Intent(this, SettingsActivity::class.java).apply {}
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun focusSelectedTab() {
        selectedTab()?.select()
    }

    private fun selectedTab(): TabLayout.Tab? {
        var i = 0
        var tab: TabLayout.Tab?
        while (i < tabLayout.tabCount) {
            tab = tabLayout.getTabAt(i)
            if (tab?.id == selectedListId) return tab
            i++
        }
        return null
    }

    private fun loadInitialView() {
        findViewById<ConstraintLayout>(R.id.mainView).visibility = View.GONE
        findViewById<LinearLayout>(R.id.initialView).visibility = View.VISIBLE
    }

    private fun loadMainView() {
        findViewById<ConstraintLayout>(R.id.mainView).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.initialView).visibility = View.GONE
    }

    private fun openDelChkItemsDialog() {
        val nOfCheckedItems = items.filter { it.checked }.size
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm Checked Item Deletion")
            .setMessage("Are you sure you want to delete all ($nOfCheckedItems) checked items?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                delChkItems()
            }
            .show()
    }

    private fun delChkItems() {
        coroutineScope.launch { dao.deleteCheckedItems(selectedListId) }
        items.removeAll { it.checked }
        adapter.notifyDataSetChanged()
    }

    private fun openDelAllItemsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm Item Deletion")
            .setMessage("Are you sure you want to delete all (${items.size}) items?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                delAllItems()
            }
            .show()
    }

    private fun delAllItems() {
        coroutineScope.launch { dao.deleteItems(selectedListId) }
        items.clear()
        adapter.notifyDataSetChanged()
    }

    private fun openAddListDialog(isFirst: Boolean = false) {
        val view = LayoutInflater.from(this).inflate(R.layout.create_list_dialog, null, false)
        val input = view.findViewById<TextInputEditText>(R.id.createListDialogInput)
        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton("Cancel") { _, _ ->
                input.setText("")
            }
            .setPositiveButton("Add") { _, _ ->
                val listName = input.text.toString()
                addList(listName, isFirst)
            }
            .show()
    }

    private fun addList(name: String, isFirst: Boolean = false) {
        val list = SList(name)
        selectedListId = runBlocking { dao.insertList(list).toInt() }
        if (isFirst) {
            Prefs.setSelectedList(selectedListId)
            tabLayout.addTab(tabLayout.newTab().setId(selectedListId).setText(name))
            focusSelectedTab()
            items.clear()
            adapter.notifyDataSetChanged()
            loadMainView()
        } else {
            tabLayout.addTab(tabLayout.newTab().setId(selectedListId).setText(name))
            focusSelectedTab()
            items.clear()
            adapter.notifyDataSetChanged()
            input.setText("")
        }
    }

    private fun openDelListDialog() {
        val selectedListName = selectedTab()?.text
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm List Deletion")
            .setMessage("Are you sure you want to permanently delete the list \"${selectedListName}\" and all its items?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ -> delList() }
            .show()
    }

    private fun delList() {
        runBlocking { dao.delList(selectedListId) }
        tabLayout.removeTab(selectedTab() ?: error("invalid selected tab"))
        if (tabLayout.tabCount == 0) loadInitialView()
    }

}





