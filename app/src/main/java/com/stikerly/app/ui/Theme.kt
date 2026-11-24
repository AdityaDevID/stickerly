package com.stikerly.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Warna diambil dari logo & banner Stickerly: hijau WhatsApp, putih, outline hitam tebal, aksen kuning. */
object Brand {
    val GreenBright = Color(0xFF02E560)
    val Green = Color(0xFF0BB54F)
    val GreenDeep = Color(0xFF03963B)
    val GreenDark = Color(0xFF025B29)
    val Mint = Color(0xFFEAFDF1)
    val MintDeep = Color(0xFFCFF7DE)
    val Ink = Color(0xFF0B1A10)
    val Yellow = Color(0xFFFFC21A)
    val Bg = Color(0xFFF7FFFA)
    val Danger = Color(0xFFD93025)
}

@Composable
fun StickerlyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Brand.Green,
            onPrimary = Color.White,
            primaryContainer = Brand.Mint,
            onPrimaryContainer = Brand.Ink,
            secondary = Brand.GreenDeep,
            tertiary = Brand.Yellow,
            background = Brand.Bg,
            onBackground = Brand.Ink,
            surface = Color.White,
            onSurface = Brand.Ink,
            surfaceVariant = Brand.Mint,
            onSurfaceVariant = Brand.Ink,
            outline = Brand.Ink,
        ),
        content = content,
    )
}
