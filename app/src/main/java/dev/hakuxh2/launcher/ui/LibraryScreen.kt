package dev.hakuxh2.launcher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class GameEntry(val titleId: String, val name: String, val path: String)

/** Game list + dump import (Layer 1, item 1). Import uses SAF; scanning is Phase 0 work. */
@Composable
fun LibraryScreen() {
    var games by remember { mutableStateOf(listOf<GameEntry>()) } // TODO(P0): scan dumps dir
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = { /* TODO(P0): ACTION_OPEN_DOCUMENT for ISO/XISO, copy into library */ }) {
            Text("Import game image (ISO/XISO)")
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn {
            items(games) { g ->
                ListItem(headlineContent = { Text(g.name) }, supportingContent = { Text(g.titleId) })
            }
        }
        if (games.isEmpty()) Text("No games yet — import a user-dumped image.", style = MaterialTheme.typography.bodySmall)
    }
}
