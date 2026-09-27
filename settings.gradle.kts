pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "claude-code-android"

include(":shared:core")
include(":shared:domain")
include(":shared:data")
include(":shared:runtime")
include(":shared:orchestration")
include(":shared:skills")
include(":shared:vcs")
include(":shared:ui")
include(":androidApp")
