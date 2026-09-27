plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("jvmLibrary") {
            id = "cc.jvm.library"
            implementationClass = "dev.ccandroid.convention.JvmLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "cc.android.library"
            implementationClass = "dev.ccandroid.convention.AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "cc.android.application"
            implementationClass = "dev.ccandroid.convention.AndroidApplicationConventionPlugin"
        }
        register("compose") {
            id = "cc.compose"
            implementationClass = "dev.ccandroid.convention.ComposeConventionPlugin"
        }
        register("kotlinTest") {
            id = "cc.kotlin.test"
            implementationClass = "dev.ccandroid.convention.KotlinTestConventionPlugin"
        }
    }
}

dependencies {
    compileOnly("com.android.tools.build:gradle:8.9.1")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")
}
