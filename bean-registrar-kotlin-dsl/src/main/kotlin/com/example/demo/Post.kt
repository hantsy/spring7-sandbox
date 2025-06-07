package com.example.demo

import java.util.*

data class Post(
    val id: UUID? = null,
    var title: String?,
    var content: String?
)