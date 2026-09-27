plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.detekt) apply false
}

tasks.register("check") {
    group = "verification"
    description = "Runs all checks and tests across all subprojects"
    dependsOn(subprojects.map { it.tasks.matching { task -> task.name == "check" || task.name == "test" } })
}
