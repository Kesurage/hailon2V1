package dev.hakuxh2.services.fsr

/**
 * Phase 2: FSR1 (EASU upscale + RCAS sharpen) as a Vulkan compute presentation pass,
 * porting Xenia's proven presentation-layer pattern. fp16 math, simplified EASU;
 * budget on Mali-G57 MC2: 2-4 ms added at 720p-to-1080p-class upscale. RCAS optional;
 * fall back to CAS when not upscaling. Off by default.
 */
data class FsrConfig(
    val enabled: Boolean = false,
    val sharpness: Float = 0.2f,          // maps to sharpness-reduction tuning knob
    val useFp16: Boolean = true,
    val rcasEnabled: Boolean = true,
)
