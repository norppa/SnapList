package com.ducksoup.snaplist

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.MenuCompat
import androidx.core.view.isEmpty
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
    private lateinit var adapter: SnapListAdapter
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

        adapter = SnapListAdapter()
        recyclerView.layoutManager = LinearLayoutManager(applicationContext)
        recyclerView.adapter = adapter

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                println("onTabSelected")
                selectedListId = tab?.id ?: error("Missing tab id")
                println("selectedListId $selectedListId")
                coroutineScope.launch {
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
        println("onStop $selectedListId")
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
                MaterialAlertDialogBuilder(this)
                    .setTitle("Confirm Item Deletion")
                    .setMessage("Are you sure?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        coroutineScope.launch { dao.deleteItems(selectedListId) }
                        items.clear()
                        adapter.notifyDataSetChanged()
                    }
                    .show()
                true
            }
            R.id.del_chk_items -> {
                coroutineScope.launch { dao.deleteCheckedItems(selectedListId) }
                items.removeAll { it.checked }
                adapter.notifyDataSetChanged()
                true
            }
            R.id.add_list -> {
                openAddListDialog()
                true
            }
            R.id.del_list -> {
                openDelListDialog()
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
        (0 until tabLayout.tabCount).forEach {
            val tab = tabLayout.getTabAt(it)
            if (tab?.id == selectedListId) tab.select()
        }
    }

    private fun removeSelectedTab() {
        var i = 0
        var tab: TabLayout.Tab?
        while (i < tabLayout.tabCount) {
            tab = tabLayout.getTabAt(i)
            if (tab?.id == selectedListId) {
                tabLayout.removeTabAt(i)
                break
            }
            i++
        }
        if (tabLayout.tabCount == 0) loadInitialView()
    }

    private fun loadInitialView() {
        findViewById<ConstraintLayout>(R.id.mainView).visibility = View.GONE
        findViewById<LinearLayout>(R.id.initialView).visibility = View.VISIBLE
    }

    private fun loadMainView() {
        findViewById<ConstraintLayout>(R.id.mainView).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.initialView).visibility = View.GONE
    }

    inner class SnapListAdapter() : RecyclerView.Adapter<SnapListAdapter.ViewHolder>() {

        private val colorGray = ContextCompat.getColor(applicationContext, R.color.lightgray)
        private val colorBlack = ContextCompat.getColor(applicationContext, R.color.black)

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(R.id.textView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.main_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val textView = holder.textView
            val item = items[position]
            textView.text = item.label
            if (item.checked) {
                textView.setTextColor(colorGray)
                textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                textView.setTextColor(colorBlack)
                textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            textView.setOnClickListener {
                item.checked = !item.checked
                runBlocking { dao.updateItem(item) }
                this.notifyItemChanged(position)
            }
        }

        override fun getItemCount(): Int = items.size

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
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm List Deletion")
            .setMessage("Are you sure?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                runBlocking { dao.delList(selectedListId) }
                removeSelectedTab()
            }
            .show()
    }

}





