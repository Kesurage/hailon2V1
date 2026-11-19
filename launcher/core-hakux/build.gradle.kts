plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "dev.hakuxh2.core"
    compileSdk = 35
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Phase 0: replace this placeholder module with the upstream hakuX core artifact.
// The upstream build output is consumed as an AAR dependency here; keep this module's
// API surface identical to what native-glue expects (launch request in, frame loop out).
dependencies {
    // implementation(files("libs/hakux-core-release.aar"))  // wired in Phase 0
}
