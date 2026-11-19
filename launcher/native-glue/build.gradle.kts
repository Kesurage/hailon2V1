plugins { id("com.android.library") }

android {
    namespace = "dev.hakuxh2.nativeglue"
    compileSdk = 35
    defaultConfig { minSdk = 29 }
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt") } }
}
