package dev.hakuxh2.launcher.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.hakuxh2.launcher.R
import dev.hakuxh2.launcher.library.GameEntry
import dev.hakuxh2.launcher.library.LibraryStore
import dev.hakuxh2.launcher.library.XisoProbe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/** Game list + dump import (Layer 1, item 1). SAF import works; the emulator core is Phase 1. */
@Composable
fun LibraryScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var games by remember { mutableStateOf(LibraryStore.load(context)) }

    val pickDump = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            val msg = runCatching {
                val displayName = LibraryStore.queryPickedDisplayName(context, uri)
                val verified = withContext(Dispatchers.IO) { XisoProbe.hasXboxSignature(context, uri) }
                val file = withContext(Dispatchers.IO) { LibraryStore.copyIntoDumps(context, uri, displayName) }
                val entry = GameEntry(
                    name = displayName.removeSuffix(".iso").removeSuffix(".xiso").removeSuffix(".bin"),
                    path = file.absolutePath,
                    sizeBytes = file.length(),
                    verified = verified,
                )
                LibraryStore.save(context, LibraryStore.load(context) + entry)
                games = LibraryStore.load(context)
                if (verified) "Imported ${entry.name} — XISO signature ok"
                else "Imported ${entry.name} — warning: no Xbox signature found"
            }.getOrElse { "Import failed: ${it.message}" }
            snackbar.showSnackbar(msg)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(R.drawable.logo_header),
                    contentDescription = "hailon2V1 logo",
                    modifier = Modifier.size(40.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("hailon2V1", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Library — user dumps, never modified in place", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { pickDump.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text("Import game image (ISO/XISO)")
            }
            Text(
                "Copies the dump into app storage and checks the Xbox volume signature. " +
                    "Launching is stubbed until the core lands in Phase 1.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn {
                items(games, key = { it.path }) { g ->
                    ListItem(
                        headlineContent = { Text(g.name) },
                        supportingContent = {
                            Text("${formatSize(g.sizeBytes)} · ${if (g.verified) "XISO signature ok" else "signature not found"}")
                        },
                        trailingContent = {
                            IconButton(onClick = {
                                File(g.path).delete()
                                LibraryStore.save(context, LibraryStore.load(context).filterNot { it.path == g.path })
                                games = LibraryStore.load(context)
                            }) { Icon(Icons.Filled.Delete, contentDescription = "Remove ${g.name}") }
                        },
                    )
                }
            }
            if (games.isEmpty()) {
                Text("No games yet — import a user-dumped image.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> String.format(Locale.US, "%.1f GB", bytes / 1e9)
    bytes >= 1L shl 20 -> String.format(Locale.US, "%.1f MB", bytes / 1e6)
    bytes > 0 -> "$bytes B"
    else -> "unknown size"
}
