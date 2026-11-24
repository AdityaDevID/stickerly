package com.stikerly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stikerly.app.data.StickerStore
import com.stikerly.app.ui.AppRoot
import com.stikerly.app.ui.StickerlyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        StickerStore.ensure(this)
        setContent {
            StickerlyTheme {
                AppRoot()
            }
        }
    }
}
