pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://repo.eclipse.org/content/repositories/paho-snapshots/")
        maven("https://jitpack.io")
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://repo.eclipse.org/content/repositories/paho-snapshots/")
        maven("https://jitpack.io")
    }
}

rootProject.name = "CardioAlert"
include(":app")
