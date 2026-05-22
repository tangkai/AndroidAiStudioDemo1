package com.example

import com.example.data.model.SourceType
import com.example.data.repository.PagingRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robust structural architecture unit test verifying the primary
 * repository and list manipulation paradigms of Paging 3.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    private lateinit var repository: PagingRepository

    @Before
    fun setUp() {
        repository = PagingRepository()
    }

    @Test
    fun testInitialCount_isThreeHundred() {
        // Assert initial repository size matches 300 entries
        assertEquals(300, repository.getTotalCount())
    }

    @Test
    fun testFetchPage_returnsCorrectRecords() {
        // Fetch 10 items starting from index 120
        val page = repository.fetchPage(offset = 120, limit = 10)
        
        assertEquals(10, page.size)
        assertEquals(120, page.first().id)
        assertEquals(129, page.last().id)
        assertEquals("Database Record #120", page.first().title)
    }

    @Test
    fun testEditItem_updatesInPlace_andMarksAsModified() {
        // Edit item ID 42
        repository.editItem(
            id = 42,
            newTitle = "Polished Test Title",
            newSubtitle = "Details updated"
        )

        // Read item ID 42 back to check state
        val page = repository.fetchPage(offset = 42, limit = 1)
        val item = page.first()

        assertEquals("Polished Test Title", item.title)
        assertEquals("Details updated", item.subtitle)
        assertEquals(SourceType.MODIFIED, item.type)
    }

    @Test
    fun testDeleteItem_removesItem_andReducesTotalCount() {
        val initialSize = repository.getTotalCount()
        
        // Delete item ID 10
        repository.deleteItem(10)

        // Count should reduce by 1
        assertEquals(initialSize - 1, repository.getTotalCount())

        // Fetch around index 10 to ensure ID 10 is no longer present
        val page = repository.fetchPage(offset = 9, limit = 3)
        val ids = page.map { it.id }
        
        assertFalse("ID 10 should not exist in the active collection", ids.contains(10))
    }

    @Test
    fun testPrependItem_insertsAtFirstPosition() {
        // Prepend custom record with ID 888
        repository.prependItem(
            id = 888,
            title = "Prepended Title",
            subtitle = "Prepended Subtitle"
        )

        // Fetch the very first item
        val firstPage = repository.fetchPage(offset = 0, limit = 1)
        val firstItem = firstPage.first()

        assertEquals(888, firstItem.id)
        assertEquals("Prepended Title", firstItem.title)
        assertEquals(SourceType.INSERTED, firstItem.type)
        assertEquals(301, repository.getTotalCount())
    }

    @Test
    fun testAppendItem_insertsAtLastPosition() {
        val lastIdx = repository.getTotalCount()

        // Append custom record with ID 999
        repository.appendItem(
            id = 999,
            title = "Appended Title",
            subtitle = "Appended Subtitle"
        )

        // Fetch the last item
        val lastItemPage = repository.fetchPage(offset = lastIdx, limit = 1)
        val lastItem = lastItemPage.first()

        assertEquals(999, lastItem.id)
        assertEquals("Appended Title", lastItem.title)
        assertEquals(301, repository.getTotalCount())
    }

    @Test
    fun testInsertItemAtIndex_placesAtCorrectPosition() {
        // Insert custom record with ID 777 at database position 55
        repository.insertItemAt(
            index = 55,
            id = 777,
            title = "Custom Splice Row",
            subtitle = "At Index 55"
        )

        val page = repository.fetchPage(offset = 55, limit = 1)
        val item = page.first()

        assertEquals(777, item.id)
        assertEquals("Custom Splice Row", item.title)
    }
}
