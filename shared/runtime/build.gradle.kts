plugins {
    id("cc.android.library")
    id("cc.kotlin.test")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.ccandroid.runtime"
}

dependencies {
    api(project(":shared:core"))
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}
