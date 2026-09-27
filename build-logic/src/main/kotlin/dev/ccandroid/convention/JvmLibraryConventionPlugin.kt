package dev.ccandroid.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")

            extensions.configure(JavaPluginExtension::class.java) {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(21))
                }
            }

            extensions.configure(KotlinJvmProjectExtension::class.java) {
                jvmToolchain(21)
                compilerOptions {
                    allWarningsAsErrors.set(true)
                }
            }
        }
    }
}
