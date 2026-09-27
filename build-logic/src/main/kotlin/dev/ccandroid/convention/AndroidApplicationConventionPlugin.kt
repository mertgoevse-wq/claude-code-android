package dev.ccandroid.convention

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.android")

            extensions.configure(ApplicationExtension::class.java) {
                compileSdk = 35

                defaultConfig {
                    applicationId = "dev.ccandroid"
                    minSdk = 26
                    targetSdk = 35
                    versionCode = 1
                    versionName = "0.1.0"
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                signingConfigs {
                    loadReleaseSigning(project.rootDir)?.let { material ->
                        create("release") {
                            storeFile = material.storeFile
                            storePassword = material.storePassword
                            keyAlias = material.keyAlias
                            keyPassword = material.keyPassword
                        }
                    }
                }

                buildTypes {
                    debug {
                        applicationIdSuffix = ".debug"
                        isMinifyEnabled = false
                    }
                    release {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                        // Null when no keystore.properties is present, which is
                        // the normal case for a local build. The result is an
                        // unsigned APK rather than a failed build.
                        signingConfig = signingConfigs.findByName("release")
                    }
                }

                packaging {
                    resources {
                        excludes += listOf(
                            "/META-INF/{AL2.0,LGPL2.1}",
                            "META-INF/*.version",
                            "kotlin/**",
                            "DebugProbesKt.bin"
                        )
                    }
                }
            }

            extensions.configure(KotlinAndroidProjectExtension::class.java) {
                jvmToolchain(21)
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
    }
}
