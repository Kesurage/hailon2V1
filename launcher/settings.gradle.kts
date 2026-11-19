pluginManagement {
    repositories {
        google()
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://maven.aliyun.com/repository/public")
        mavenCentral()
    }
}
rootProject.name = "hailon2v1"
include(":app")
include(":core-hakux")
include(":native-glue")
include(":services:modloader")
include(":services:fsr")
include(":services:malicompat")
include(":services:driverloader")
