package dev.hakuxh2.launcher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hakuxh2.services.modloader.ModEntry

/** Mod manager shell (Layer 1, item 3). Format spec: docs/MOD_FORMAT.md (Phase 3). */
@Composable
fun ModManagerScreen() {
    var mods by remember { mutableStateOf(listOf<ModEntry>()) } // TODO(P3): import + checksums
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = { /* TODO(P3): import mod zip, verify manifest + checksums */ }) {
            Text("Install mod package (.zip)")
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn {
            items(mods.sortedBy { it.loadOrder }) { m ->
                ListItem(headlineContent = { Text(m.name) },
                    supportingContent = { Text("order ${m.loadOrder} - ${m.id}") },
                    trailingContent = { Checkbox(checked = m.enabled, onCheckedChange = { /* TODO(P3) */ }) })
            }
        }
        if (mods.isEmpty()) Text("No mods installed. The pristine dump always stays untouched.",
            style = MaterialTheme.typography.bodySmall)
    }
}
