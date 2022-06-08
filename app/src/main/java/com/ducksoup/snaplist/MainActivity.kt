package com.ducksoup.snaplist

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var mainListAdapter: MainListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val list = listOf("one", "two", "three")

        val recyclerView = this.findViewById<RecyclerView>(R.id.recyclerView)
        linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager
        mainListAdapter = MainListAdapter(list)
        recyclerView.adapter = mainListAdapter

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
}

class MainListAdapter(private val list: List<String>) :
    RecyclerView.Adapter<MainListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView

        init {
            textView = view.findViewById(R.id.textView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.main_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = list[position]
    }

    override fun getItemCount(): Int = list.size
}

