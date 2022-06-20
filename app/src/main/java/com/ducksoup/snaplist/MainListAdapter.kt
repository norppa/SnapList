package com.ducksoup.snaplist

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ducksoup.snaplist.model.SItem
import kotlinx.coroutines.runBlocking

class MainListAdapter(
    private var items: List<SItem>,
    private val dao: SnapListDao,
    context: Context
) : RecyclerView.Adapter<MainListAdapter.ViewHolder>() {

    private val colorGray = ContextCompat.getColor(context, R.color.lightgray)
    private val colorBlack = ContextCompat.getColor(context, R.color.black)

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