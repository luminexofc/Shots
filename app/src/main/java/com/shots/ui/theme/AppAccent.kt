package com.shots.ui.theme

import androidx.compose.ui.graphics.Color

data class AppAccent(
    val bg: Color,
    val ink: Color
)

object AppAccents {
    val options = listOf(
        AppAccent(Color(0xFFA7F3D0), Color(0xFF0F1115)),
        AppAccent(Color(0xFF7DD3FC), Color(0xFF0F1115)),
        AppAccent(Color(0xFFC4B5FD), Color(0xFF0F1115)),
        AppAccent(Color(0xFFFCD34D), Color(0xFF0F1115)),
        AppAccent(Color(0xFFFDA4AF), Color(0xFF0F1115))
    )

    fun get(index: Int): AppAccent = options[index.coerceIn(options.indices)]
}
