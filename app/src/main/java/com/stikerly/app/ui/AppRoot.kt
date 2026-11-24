package com.stikerly.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed interface Route {
    data object Home : Route
    data object Packs : Route
    data object Text : Route
    data object Image : Route
    data class Video(val sound: Boolean) : Route
    data class Pack(val id: String) : Route
}

@Composable
fun AppRoot() {
    var route by remember { mutableStateOf<Route>(Route.Home) }

    BackHandler(enabled = route != Route.Home) {
        route = when (route) {
            is Route.Pack -> Route.Packs
            else -> Route.Home
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brand.Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        when (val r = route) {
            Route.Home -> MainFrame(selected = 0, onSelect = { route = if (it == 0) Route.Home else Route.Packs }) {
                HomeScreen(onOpen = { route = it })
            }
            Route.Packs -> MainFrame(selected = 1, onSelect = { route = if (it == 0) Route.Home else Route.Packs }) {
                PacksScreen(onOpen = { route = Route.Pack(it) })
            }
            Route.Text -> TextEditorScreen(onBack = { route = Route.Home })
            Route.Image -> ImageEditorScreen(onBack = { route = Route.Home })
            is Route.Video -> VideoEditorScreen(initialSound = r.sound, onBack = { route = Route.Home })
            is Route.Pack -> PackDetailScreen(packId = r.id, onBack = { route = Route.Packs })
        }
    }
}

@Composable
private fun MainFrame(selected: Int, onSelect: (Int) -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { content() }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Brand.Ink))
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            NavItem("🏠", "Beranda", selected == 0) { onSelect(0) }
            NavItem("📦", "Paket", selected == 1) { onSelect(1) }
        }
    }
}

@Composable
private fun NavItem(icon: String, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, fontSize = 22.sp)
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.Black else FontWeight.Medium,
            color = if (active) Brand.Green else Brand.Ink,
        )
        Box(
            Modifier
                .padding(top = 2.dp)
                .height(3.dp)
                .fillMaxWidth()
                .background(if (active) Brand.Green else Color.Transparent)
        )
    }
}
