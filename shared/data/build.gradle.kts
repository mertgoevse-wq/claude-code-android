plugins {
    id("cc.android.library")
    id("cc.kotlin.test")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "dev.ccandroid.data"
    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    api(project(":shared:domain"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)

    // The provider client. Ktor rather than raw OkHttp because a turn is
    // server-sent events that must be parsed incrementally: a response is never
    // buffered whole, because a streaming turn can be megabytes of deltas.
    // OkHttp is the Android engine; the engine is injected so the tests can
    // replace it with a MockEngine and never open a socket.
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)

    implementation(libs.datastore.preferences)

    // The DAO tests run as plain JVM tests on Room's KMP path with the
    // BundledSQLiteDriver. Robolectric cannot host these tests on this repo's
    // ARM64 build host — its native runtime ships no linux/aarch64 build
    // (robolectric/robolectric#9166; progress log, P2-6 entry).
    //
    // Test deps use explicit -jvm coordinates so the JVM actuals win on the
    // test classpath: the android artifacts ship a NativeLibraryLoader that
    // only knows System.loadLibrary, and a plain type=jar selector still
    // resolves the AAR (Gradle module metadata types AARs as "jar").
    testImplementation("androidx.sqlite:sqlite-jvm:${libs.versions.sqlite.get()}")
    testImplementation("androidx.sqlite:sqlite-bundled-jvm:${libs.versions.sqlite.get()}")
    testImplementation("androidx.room:room-runtime-jvm:${libs.versions.room.get()}")
    testImplementation(libs.room.testing)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}
