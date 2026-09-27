package dev.ccandroid.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

class KotlinTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            tasks.withType(Test::class.java).configureEach {
                useJUnit()
            }
        }
    }
}
