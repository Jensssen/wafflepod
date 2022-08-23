package com.jensssen.wafflepod.classes

data class Message(
    val date: String? = null,
    val message: String = "",
    val author: String = "",
    val position: Int = 0,
    val uri: String? = null
)