plugins {
    id("cc.android.application")
    id("cc.compose")
    id("cc.kotlin.test")
}

android {
    namespace = "dev.ccandroid"
}

dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:domain"))
    implementation(project(":shared:data"))
    implementation(project(":shared:runtime"))
    implementation(project(":shared:orchestration"))
    implementation(project(":shared:skills"))
    implementation(project(":shared:vcs"))
    implementation(project(":shared:ui"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.junit)
}
