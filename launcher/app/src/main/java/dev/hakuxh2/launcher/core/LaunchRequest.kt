package dev.hakuxh2.launcher.core

import dev.hakuxh2.services.driverloader.VulkanDriver
import dev.hakuxh2.services.modloader.ModEntry

/** Per-game profile (roadmap 5.1). Defaults are the Phase 1 anti-flicker defaults. */
data class GameProfile(
    val resolutionScale: Float = 1.0f,      // 1x default: scaling is the top flicker cause
    val fsrEnabled: Boolean = false,        // Phase 2: FSR1 EASU+RCAS, off by default
    val fsrSharpness: Float = 0.2f,
    val targetFps: Int = 30,                // original-console ceiling first
    val maliWorkaroundsEnabled: Boolean = true,
)

/** The single object the UI hands to the services layer and the core (Phase 0 contract). */
data class LaunchRequest(
    val titleId: String,
    val gameImagePath: String,
    val profile: GameProfile,
    val driver: VulkanDriver = VulkanDriver.STOCK_MALI,
    val modStack: List<ModEntry> = emptyList(), // ordered, enabled mods only
)
