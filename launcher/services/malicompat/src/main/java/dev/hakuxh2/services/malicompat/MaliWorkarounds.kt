package dev.hakuxh2.services.malicompat

/**
 * Phase 1 centerpiece: the Mali compatibility layer (roadmap Table 3).
 * Every toggle is per-game, defaults conservative, and included in bug-report bundles.
 */
data class MaliWorkarounds(
    // Stock Arm driver may over-allocate swapchain buffers (14 seen when 8 requested):
    // size the swapchain from driver-reported caps, never from the requested minimum.
    val clampSwapchainFromDriverCaps: Boolean = true,
    // Tile-memory depth fast path is documented to flicker on Mali: gate it off first.
    val disableTileMemoryDepth: Boolean = true,
    // Mali returns NaN for 0 x infinity where desktop GPUs return 0: SPIR-V constant
    // rewrite pass behind a per-game flag.
    val nanConstantRewrite: Boolean = false,
    // Default internal resolution to 1x; scaling is the top flicker hypothesis.
    val forceOneXScaling: Boolean = true,
)
