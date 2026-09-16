package com.familia.monitor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ChatLogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rv_chat_logs)
        rv.layoutManager = LinearLayoutManager(this)

        var messages = MessageLogHelper.getMessages(this)
        rv.adapter = ChatAdapter(messages)
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        val clearItem = menu.add(0, 100, 0, "Limpiar")
        clearItem.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == 100) {
            MessageLogHelper.clearMessages(this)
            val rv = findViewById<RecyclerView>(R.id.rv_chat_logs)
            rv.adapter = ChatAdapter(emptyList())
            android.widget.Toast.makeText(this, "Registro de mensajes limpio", android.widget.Toast.LENGTH_SHORT).show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    class ChatAdapter(private val items: List<MessageLogHelper.CapturedMessage>) : 
        RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvApp: TextView = view.findViewById(android.R.id.text1)
            val tvText: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val date = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(item.timestamp))
            
            holder.tvApp.text = "${item.appName} - $date"
            holder.tvApp.textSize = 12f
            holder.tvApp.setTextColor(0xFF6B7280.toInt())
            
            holder.tvText.text = "${item.sender}: ${item.text}"
            holder.tvText.textSize = 16f
            holder.tvText.setTextColor(0xFF111827.toInt())
        }

        override fun getItemCount() = items.size
    }
}
