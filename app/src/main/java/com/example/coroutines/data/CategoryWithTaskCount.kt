package com.example.coroutines.data

data class CategoryWithTaskCount(
    val id: Long,
    val name: String,
    val colorHex: Long,
    val taskCount: Int,
)
