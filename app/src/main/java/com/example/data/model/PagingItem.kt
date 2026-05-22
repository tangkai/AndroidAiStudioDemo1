package com.example.data.model

/**
 * Data entity representing paginated items in the feed.
 */
data class PagingItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val timestamp: Long,
    val type: SourceType = SourceType.REMOTE,
    val category: String = "Engineering"
)

enum class SourceType {
    REMOTE,
    LOCAL,
    INSERTED,
    MODIFIED
}
