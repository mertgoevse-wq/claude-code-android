plugins {
    id("cc.jvm.library")
    id("cc.kotlin.test")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":shared:domain"))
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}
