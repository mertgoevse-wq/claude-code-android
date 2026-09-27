plugins {
    id("cc.jvm.library")
    id("cc.kotlin.test")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":shared:core"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}
