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
    // The composition root owns the Keystore and the DataStore file, so the
    // Room and DataStore artifacts are declared here rather than leaked
    // through shared:data's implementation scope.
    implementation(libs.room.runtime)
    implementation(libs.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // The store composition tests run on the bundled driver, like shared:data.
    testImplementation("androidx.sqlite:sqlite-jvm:${libs.versions.sqlite.get()}")
    testImplementation("androidx.sqlite:sqlite-bundled-jvm:${libs.versions.sqlite.get()}")
    testImplementation("androidx.room:room-runtime-jvm:${libs.versions.room.get()}")
}
