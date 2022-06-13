package com.ducksoup.snaplist

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ducksoup.snaplist.model.SItem
import com.ducksoup.snaplist.model.SList
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private val database by lazy { SnapListDatabase.getDatabase(this) }
    private val dao by lazy { database.dao() }
    private val coroutineScope = CoroutineScope(SupervisorJob())

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var input: EditText
    private lateinit var submit: FloatingActionButton
    private lateinit var adapter: SnapListAdapter

    private var items = mutableListOf<SItem>()

    private var selectedListId = 0

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.recyclerView)
        input = findViewById(R.id.input)
        submit = findViewById(R.id.submitInput)

        adapter = SnapListAdapter()
        recyclerView.layoutManager = LinearLayoutManager(applicationContext)
        recyclerView.adapter = adapter

//        generateBogusData()
        runBlocking {
            selectedListId = database.dao().getSelectedList()
            dao.getLists().forEach {
                tabLayout.addTab(tabLayout.newTab().setId(it.id).setText(it.name))
            }
            focusSelectedTab()

            items.clear()
            items.addAll(dao.getItems(selectedListId))
            adapter.notifyDataSetChanged()
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedListId = tab?.id ?: error("Missing tab id")
                coroutineScope.launch {
                    dao.setSelectedList(selectedListId)
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
                val view =
                    LayoutInflater.from(this).inflate(R.layout.create_list_dialog, null, false)
                val input = view.findViewById<TextInputEditText>(R.id.createListDialogInput)
                MaterialAlertDialogBuilder(this)
                    .setView(view)
                    .setNegativeButton("Cancel") { _, _ ->
                        input.setText("")
                    }
                    .setPositiveButton("Add") { _, _ ->
                        val listName = input.text.toString()
                        val list = SList(input.text.toString())
                        runBlocking {
                            selectedListId = dao.insertList(list).toInt()
                            dao.setSelectedList(selectedListId)

                        }
                        tabLayout.addTab(tabLayout.newTab().setId(selectedListId).setText(listName))
                        focusSelectedTab()
                        items.clear()
                        adapter.notifyDataSetChanged()
                        input.setText("")
                    }
                    .show()
                true
            }
            R.id.del_list -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Confirm List Deletion")
                    .setMessage("Are you sure?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        coroutineScope.launch { dao.deleteList(selectedListId) }
                        removeSelectedTab()
                    }
                    .show()
                true
            }
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
                runBlocking { dao.setItemChecked(!item.checked, item.id) }
                items[position].checked = !item.checked
                this.notifyItemChanged(position)
            }
        }

        override fun getItemCount(): Int = items.size

    }
}





