// JNI stubs for the hakuX-H2 fork (roadmap Phase 0 wires these to the real core).
// Contract: see docs/ARCHITECTURE.md — the UI passes one serialized LaunchRequest;
// the services layer has already staged mods and injected the chosen Vulkan ICD
// BEFORE any Vulkan instance is created here.
#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_dev_hakuxh2_launcher_core_HakuxH2Jni_probeVulkan(JNIEnv* env, jobject /*thiz*/) {
    // Phase 0: enumerate vkEnumerateInstanceLayerProperties / instance extensions,
    // report driver name + Vulkan version + swapchain caps for DiagScreen.
    std::string report = "probe: stub (Phase 0) - no Vulkan instance created yet";
    return env->NewStringUTF(report.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_dev_hakuxh2_launcher_core_HakuxH2Jni_launchGame(JNIEnv* /*env*/, jobject /*thiz*/, jstring /*requestJson*/) {
    // Phase 0: hand the request to the upstream core (core-hakux AAR) after the
    // modloader overlay is staged and the driver loader has injected the ICD.
    return JNI_FALSE;
}
