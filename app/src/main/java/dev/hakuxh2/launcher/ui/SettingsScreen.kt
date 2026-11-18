package dev.hakuxh2.launcher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hakuxh2.launcher.core.GameProfile
import dev.hakuxh2.services.driverloader.VulkanDriver

/** Per-game profile editor (Layer 1, items 2 & 4). Phase 1 wires persistence per title. */
@Composable
fun SettingsScreen() {
    var profile by remember { mutableStateOf(GameProfile()) }
    var driver by remember { mutableStateOf(VulkanDriver.STOCK_MALI) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Resolution scale: %.2fx".format(profile.resolutionScale))
        Slider(value = profile.resolutionScale, valueRange = 1f..2f,
            onValueChange = { profile = profile.copy(resolutionScale = it) })
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Switch(checked = profile.fsrEnabled, onCheckedChange = { profile = profile.copy(fsrEnabled = it) })
            Spacer(Modifier.width(8.dp)); Text("FSR1 upscale (Phase 2)")
        }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Switch(checked = profile.maliWorkaroundsEnabled, onCheckedChange = { profile = profile.copy(maliWorkaroundsEnabled = it) })
            Spacer(Modifier.width(8.dp)); Text("Mali compatibility layer (Phase 1)")
        }
        Text("Vulkan driver")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = driver == VulkanDriver.STOCK_MALI, onClick = { driver = VulkanDriver.STOCK_MALI },
                label = { Text("Stock Mali") })
            FilterChip(selected = driver == VulkanDriver.PANVK_KBASE, onClick = { driver = VulkanDriver.PANVK_KBASE },
                label = { Text("PanVK-kbase (experimental)") })
        }
    }
}
