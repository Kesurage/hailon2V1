package dev.hakuxh2.launcher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hakuxh2.launcher.core.HakuxH2Jni

/** Diagnostics (roadmap P0/P4): Vulkan probe, driver identity, checksum dry-run. */
@Composable
fun DiagScreen() {
    var report by remember { mutableStateOf("not run yet") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { report = runCatching { HakuxH2Jni.probeVulkan() }.getOrElse { "probe failed: $it" } }) {
            Text("Run Vulkan probe")
        }
        Text(report, style = MaterialTheme.typography.bodySmall)
    }
}
