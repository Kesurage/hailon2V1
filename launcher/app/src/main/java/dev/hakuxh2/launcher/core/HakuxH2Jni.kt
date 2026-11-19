package dev.hakuxh2.launcher.core

/** JNI surface implemented by native-glue/hakux_h2_jni.cpp. Phase 0 wires the real core. */
object HakuxH2Jni {
    init { System.loadLibrary("hakux_h2") }

    /** Serializes [LaunchRequest]; returns false until the core AAR is wired. */
    external fun launchGame(requestJson: String): Boolean

    /** Vulkan probe for DiagScreen: driver name, Vulkan version, swapchain caps. */
    external fun probeVulkan(): String
}
