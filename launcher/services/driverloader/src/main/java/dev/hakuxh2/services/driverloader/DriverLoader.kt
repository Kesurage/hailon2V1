package dev.hakuxh2.services.driverloader

/** Opt-in experimental renderer (roadmap D2/P4). Never the default. */
enum class VulkanDriver { STOCK_MALI, PANVK_KBASE }

/**
 * Phase 4: adrenotools-style per-app Vulkan ICD injection, no root. Loads a
 * panvk-kbase driver package (Valhall v9 / Mali-G57 is that ecosystem's declared main
 * target) for the current process only. The watchdog must auto-fallback to
 * STOCK_MALI on any VK_ERROR_DEVICE_LOST within the first minutes of a session.
 */
interface DriverLoader {
    fun install(driver: VulkanDriver, packageZipPath: String): Boolean
    fun activeDriver(): VulkanDriver
    fun onDeviceLost(): VulkanDriver // returns the fallback driver
}
