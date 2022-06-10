package com.ducksoup.snaplist

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ducksoup.snaplist.model.SItem
import com.ducksoup.snaplist.model.SList
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
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

        adapter = SnapListAdapter(items)
        recyclerView.layoutManager = LinearLayoutManager(applicationContext)
        recyclerView.adapter = adapter

//        generateBogusData()
        runBlocking {
            selectedListId = database.dao().getSelectedList()
            dao.getLists().forEach {
                tabLayout.addTab(tabLayout.newTab().setId(it.id).setText(it.name))
            }
            (0 until tabLayout.tabCount).forEach {
                val tab = tabLayout.getTabAt(it)
                if (tab?.id == selectedListId) tab.select()
            }
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
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.remove_items -> {
                coroutineScope.launch { dao.deleteItems(selectedListId) }
                val c = items.size
                items.clear()
                adapter.notifyItemRangeRemoved(0, c)
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

    private fun generateBogusData() {
        runBlocking {
            dao.deleteAllLists()
            dao.insertList(SList("todo"))
            dao.insertList(SList("shop"))
            val lists = dao.getLists()
            dao.deleteAllChoices()
            dao.initChoices(lists[0].id)
        }
    }

    private fun initializeTabLayout() {


    }
}

class SnapListAdapter(private val items: List<SItem>) :
    RecyclerView.Adapter<SnapListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView = view.findViewById<TextView>(R.id.textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.main_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = items[position].label
    }

    override fun getItemCount(): Int = items.size


}





