plugins {
    id("cc.android.library")
    id("cc.kotlin.test")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.ccandroid.data"
}

dependencies {
    api(project(":shared:domain"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.okhttp)
    implementation(libs.datastore.preferences)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}
