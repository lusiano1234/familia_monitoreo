package com.familia.monitor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ChatLogActivity : AppCompatActivity() {

    private lateinit var adapter: ChatAdapter
    private var allMessages: List<MessageLogHelper.CapturedMessage> = emptyList()
    private var currentFilterApp: String? = null
    private var currentSearchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rv_chat_logs)
        rv.layoutManager = LinearLayoutManager(this)

        allMessages = MessageLogHelper.getMessages(this)
        adapter = ChatAdapter(allMessages)
        rv.adapter = adapter

        // Búsqueda en tiempo real
        val etSearch = findViewById<EditText>(R.id.et_search)
        etSearch.doAfterTextChanged { 
            currentSearchQuery = it?.toString()?.lowercase() ?: ""
            applyFilters()
        }

        // Filtro por App
        val btnFilter = findViewById<Button>(R.id.btn_filter_app)
        btnFilter.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showFilterDialog() {
        val apps = mutableListOf("Todas")
        val uniqueApps = allMessages.map { it.appName }.distinct().sorted()
        apps.addAll(uniqueApps)
        val options = apps.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Filtrar por Aplicación")
            .setItems(options) { _, which ->
                currentFilterApp = if (which == 0) null else options[which]
                findViewById<Button>(R.id.btn_filter_app).text = currentFilterApp ?: "Filtrar"
                applyFilters()
            }
            .show()
    }

    private fun applyFilters() {
        val filtered = allMessages.filter { msg ->
            val matchesApp = currentFilterApp == null || msg.appName == currentFilterApp
            val matchesSearch = currentSearchQuery.isEmpty() || 
                    msg.text.lowercase().contains(currentSearchQuery) || 
                    msg.sender.lowercase().contains(currentSearchQuery)
            matchesApp && matchesSearch
        }
        adapter.updateItems(filtered)
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        val clearItem = menu.add(0, 100, 0, "Limpiar")
        clearItem.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == 100) {
            MessageLogHelper.clearMessages(this)
            allMessages = emptyList()
            applyFilters()
            Toast.makeText(this, "Registro de mensajes limpio", Toast.LENGTH_SHORT).show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        if (!PinActivity.isSessionUnlocked) {
            val intent = Intent(this, PinActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        PinActivity.isNavigatingInternal = false
    }

    override fun onStop() {
        super.onStop()
        if (!PinActivity.isNavigatingInternal) {
            PinActivity.isSessionUnlocked = false
        }
    }

    override fun finish() {
        PinActivity.isNavigatingInternal = true
        super.finish()
    }

    class ChatAdapter(private var items: List<MessageLogHelper.CapturedMessage>) : 
        RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

        fun updateItems(newItems: List<MessageLogHelper.CapturedMessage>) {
            items = newItems
            notifyDataSetChanged()
        }

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
