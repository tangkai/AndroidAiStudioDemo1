package com.example.data.repository

import android.util.Log
import com.example.data.model.PagingItem
import com.example.data.model.SourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service repository acting as the central cache/source of truth.
 * Implements standard operations requested by the user:
 * - Loading from a specified key (middle paging, forward, backward)
 * - Editing item values
 * - Deleting item values
 * - Prepending / Appending items
 * - Inserting items at arbitrary positions
 */
@Singleton
class PagingRepository @Inject constructor() {

    private val TAG = "PagingRepository"
    private val _itemsList = ArrayList<PagingItem>()
    private val _mutationLog = MutableStateFlow<List<String>>(emptyList())
    val mutationLog: StateFlow<List<String>> = _mutationLog.asStateFlow()

    // Flag to trigger invalidation under Hilt-guided flow
    var onDataInvalidated: (() -> Unit)? = null

    init {
        // Populate the primary mock database list of 300 sequential entries
        // Let's index them from 0 to 299
        for (i in 0 until 300) {
            _itemsList.add(
                PagingItem(
                    id = i,
                    title = "Database Record #$i",
                    subtitle = "Latency: ${(15..45).random()}ms | Source: Remote DB Server",
                    timestamp = System.currentTimeMillis() - (300 - i) * 60000L,
                    type = SourceType.REMOTE,
                    category = when (i % 4) {
                        0 -> "Network"
                        1 -> "Hilt DI"
                        2 -> "Paging3"
                        else -> "Compose UI"
                    }
                )
            )
        }
        addLog("Database initialized with 300 sequential mock records (IDs 0 to 299).")
    }

    fun addLog(message: String) {
        val current = _mutationLog.value.toMutableList()
        current.add(0, "[${System.currentTimeMillis() % 100000}] $message")
        _mutationLog.value = current.take(20) // Keep latest 20 logs
        Log.d(TAG, message)
    }

    fun getIndexById(id: Int): Int {
        return _itemsList.indexOfFirst { it.id == id }
    }

    /**
     * Simulates fetching a chunk of data.
     * Supports middle partitioning (starting loading from a middle key / offset)
     * loads both forward and backward relative to that offset.
     */
    fun fetchPage(offset: Int, limit: Int): List<PagingItem> {
        if (_itemsList.isEmpty()) return emptyList()
        
        // Boundaries safety
        val start = Math.max(0, Math.min(offset, _itemsList.size - 1))
        val end = Math.min(start + limit, _itemsList.size)
        
        if (start >= end) return emptyList()
        
        // Simulating Network/DB read delay
        Thread.sleep(150) 
        return _itemsList.subList(start, end).toList()
    }

    fun getTotalCount(): Int = _itemsList.size

    /**
     * OPERATION 1: Edit an item's details dynamically
     */
    fun editItem(id: Int, newTitle: String, newSubtitle: String) {
        val index = _itemsList.indexOfFirst { it.id == id }
        if (index != -1) {
            val oldItem = _itemsList[index]
            _itemsList[index] = oldItem.copy(
                title = newTitle,
                subtitle = newSubtitle,
                type = SourceType.MODIFIED
            )
            addLog("Edited item ID $id to title: \"$newTitle\"")
            notifyDataChanged()
        }
    }

    /**
     * OPERATION 2: Delete an item dynamically
     */
    fun deleteItem(id: Int) {
        val index = _itemsList.indexOfFirst { it.id == id }
        if (index != -1) {
            _itemsList.removeAt(index)
            addLog("Deleted item ID $id from database.")
            notifyDataChanged()
        }
    }

    /**
     * OPERATION 3: Insert item at a specific index
     */
    fun insertItemAt(index: Int, id: Int, title: String, subtitle: String) {
        val safeIndex = Math.max(0, Math.min(index, _itemsList.size))
        val newItem = PagingItem(
            id = id,
            title = title,
            subtitle = subtitle,
            timestamp = System.currentTimeMillis(),
            type = SourceType.INSERTED,
            category = "Dynamic"
        )
        _itemsList.add(safeIndex, newItem)
        addLog("Inserted custom item ID $id at position $safeIndex")
        notifyDataChanged()
    }

    /**
     * OPERATION 4: Prepend item (insert at start of DB)
     */
    fun prependItem(id: Int, title: String, subtitle: String) {
        insertItemAt(0, id, title, subtitle)
    }

    /**
     * OPERATION 5: Append item (insert at end of DB)
     */
    fun appendItem(id: Int, title: String, subtitle: String) {
        insertItemAt(_itemsList.size, id, title, subtitle)
    }

    private fun notifyDataChanged() {
        onDataInvalidated?.invoke()
    }
}
