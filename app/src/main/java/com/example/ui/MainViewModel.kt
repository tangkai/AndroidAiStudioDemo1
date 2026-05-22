package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import com.example.data.model.PagingItem
import com.example.data.model.SourceType
import com.example.data.paging.CustomPagingSource
import com.example.data.repository.PagingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MainViewModel administering the MVVM design architecture.
 * Exposes a flow of PagingData that reacts dynamically to mutations.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: PagingRepository
) : ViewModel() {

    // Tracks starting page selection (supports loading from arbitrary indices)
    private val _initialOffset = MutableStateFlow(120)
    val initialOffset: StateFlow<Int> = _initialOffset.asStateFlow()

    // Temporary ViewModel-level filters to showcase client-side immediate intercept transformations
    private val _tempDeletedIds = MutableStateFlow<Set<Int>>(emptySet())
    private val _tempModifiedItems = MutableStateFlow<Map<Int, PagingItem>>(emptyMap())

    // Tracks the current active PagingSource instance in order to trigger invalidation updates
    private var currentPagingSource: CustomPagingSource? = null

    // Fetch the action logs from the repository to stream in our styled Logger widget
    val logs: StateFlow<List<String>> = repository.mutationLog

    // High performance PagingData stream combining repository invalidation + client-side Flow transformation
    val pagingItemsFlow: Flow<PagingData<PagingItem>> = _initialOffset
        .flatMapLatest { offset ->
            Pager(
                config = PagingConfig(
                    pageSize = 15,
                    enablePlaceholders = false,
                    prefetchDistance = 4,
                    initialLoadSize = 25
                ),
                pagingSourceFactory = {
                    CustomPagingSource(repository, initialOffset = offset).also {
                        currentPagingSource = it
                    }
                }
            ).flow
        }
        .cachedIn(viewModelScope)
        // Combine with client-side filters for instant local flow interceptions
        .combine(_tempDeletedIds) { pagingData, deletedIds ->
            pagingData.filter { it.id !in deletedIds }
        }
        .combine(_tempModifiedItems) { pagingData, modifiedMap ->
            pagingData.map { item ->
                modifiedMap[item.id] ?: item
            }
        }
        .cachedIn(viewModelScope)

    init {
        // Collect database invalidation events safely in ViewModel scope
        viewModelScope.launch {
            repository.invalidationEvents.collect {
                currentPagingSource?.invalidate()
            }
        }
        // Collect simulated error mode changes safely in ViewModel scope
        viewModelScope.launch {
            com.example.data.api.NetworkSimulationManager.errorModeFlow.collect {
                currentPagingSource?.invalidate()
            }
        }
    }

    /**
     * Set a new starting position index (from the middle)
     */
    fun setMiddleOffset(offset: Int) {
        _initialOffset.value = offset
        // Clear local UI filters when re-partitioning
        _tempDeletedIds.value = emptySet()
        _tempModifiedItems.value = emptyMap()
    }

    /**
     * DEMO ACTION: Insert item directly AFTER a specific target item ID
     */
    fun insertItemAfter(targetId: Int, id: Int, title: String, subtitle: String) {
        val targetIndex = repository.getIndexById(targetId)
        if (targetIndex != -1) {
            repository.insertItemAt(targetIndex + 1, id, title, subtitle)
        } else {
            // Fallback prepend if not found
            repository.prependItem(id, title, subtitle)
        }
    }

    /**
     * DEMO ACTION 1: Edit an item in the Repo Source of Truth
     */
    fun editItemInDatabase(id: Int, newTitle: String, newSubtitle: String) {
        repository.editItem(id, newTitle, newSubtitle)
    }

    /**
     * DEMO ACTION 2: Edit an item via Flow Interception (Client-side, Instant)
     */
    fun editItemInFlow(id: Int, newTitle: String, newSubtitle: String) {
        val currentMap = _tempModifiedItems.value.toMutableMap()
        val existingItem = currentMap[id] ?: PagingItem(
            id = id,
            title = newTitle,
            subtitle = newSubtitle,
            timestamp = System.currentTimeMillis(),
            type = SourceType.MODIFIED
        )
        currentMap[id] = existingItem.copy(
            title = newTitle,
            subtitle = newSubtitle,
            type = SourceType.MODIFIED
        )
        _tempModifiedItems.value = currentMap
    }

    /**
     * DEMO ACTION 3: Delete an item in the Repo Source of Truth (Invalidates source)
     */
    fun deleteItemFromDatabase(id: Int) {
        repository.deleteItem(id)
    }

    /**
     * DEMO ACTION 4: Delete an item via Flow Interception (Client-side, Instant)
     */
    fun deleteItemFromFlow(id: Int) {
        _tempDeletedIds.value = _tempDeletedIds.value + id
    }

    /**
     * DEMO ACTION 5: Prepend record (At Database Index 0)
     */
    fun prependItemToDatabase(id: Int, title: String, subtitle: String) {
        repository.prependItem(id, title, subtitle)
    }

    /**
     * DEMO ACTION 6: Append record (At Database End Index)
     */
    fun appendItemToDatabase(id: Int, title: String, subtitle: String) {
        repository.appendItem(id, title, subtitle)
    }

    /**
     * DEMO ACTION 7: Insert item at a specific custom index in the database
     */
    fun insertItemAtDatabaseIndex(index: Int, id: Int, title: String, subtitle: String) {
        repository.insertItemAt(index, id, title, subtitle)
    }

    /**
     * Clears all temporary client-side flow interceptors and resets DB source
     */
    fun resetFilters() {
        _tempDeletedIds.value = emptySet()
        _tempModifiedItems.value = emptyMap()
        repository.onDataInvalidated?.invoke()
    }
}
