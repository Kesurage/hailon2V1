package dev.hakuxh2.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.hakuxh2.launcher.ui.DiagScreen
import dev.hakuxh2.launcher.ui.LibraryScreen
import dev.hakuxh2.launcher.ui.ModManagerScreen
import dev.hakuxh2.launcher.ui.SettingsScreen

/**
 * Launcher shell (roadmap Layer 1). Bottom switcher only — navigation stays trivial
 * until the screens need routing, per the skeleton's job: fix boundaries, not polish.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var screen by remember { mutableStateOf("Library") }
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        when (screen) {
                            "Library" -> LibraryScreen()
                            "Settings" -> SettingsScreen()
                            "Mods" -> ModManagerScreen()
                            "Diag" -> DiagScreen()
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("Library", "Settings", "Mods", "Diag").forEach { name ->
                            FilterChip(selected = screen == name, onClick = { screen = name }, label = { Text(name) })
                        }
                    }
                }
            }
        }
    }
}
