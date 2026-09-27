plugins {
    id("cc.android.library")
    id("cc.compose")
    id("cc.kotlin.test")
}

android {
    namespace = "dev.ccandroid.ui"
}

dependencies {
    api(project(":shared:domain"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    testImplementation(libs.junit)
}
