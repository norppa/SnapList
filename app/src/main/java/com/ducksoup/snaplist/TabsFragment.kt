package com.ducksoup.snaplist

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class TabsFragment() : Fragment() {

    private val lists = mutableListOf<SList>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tabs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        addMockItems()
        val viewPager = view.findViewById<ViewPager2>(R.id.pager)
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        viewPager.adapter = TabsAdapter(this, lists)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = lists[position].name
        }.attach()
    }

    private fun addMockItems() {
        lists.add(
            SList(
                "todo",
                mutableListOf(
                    SItem("do chores", false),
                    SItem("chop wood", false),
                    SItem("sleep", true)
                )
            ))
        lists.add(
            SList(
                "shop",
                mutableListOf(
                    SItem("milk", false),
                    SItem("eggs", false),
                    SItem("beer", true)
                )
            )
        )
    }

}

class TabsAdapter(fragment: Fragment, private val tabList: List<SList>) :
    FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = tabList.size

    override fun createFragment(position: Int): Fragment {
        return TabFragment(tabList[position].items)
    }

}

class TabFragment(private val items: List<SItem>) : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.list)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ListAdapter(items)
    }
}

class ListAdapter(private val list: List<SItem>) :
    RecyclerView.Adapter<ListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.main_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = list[position].label
    }

    override fun getItemCount(): Int = list.size
}