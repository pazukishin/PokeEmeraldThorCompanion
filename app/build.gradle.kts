import java.util.Properties
import java.net.URI

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val versionPropsFile = file("$rootDir/version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        versionPropsFile.inputStream().use { load(it) }
    }
}

val appVersionCode = (versionProps.getProperty("VERSION_CODE", "1").toIntOrNull() ?: 1)
val appVersionName = versionProps.getProperty("VERSION_NAME", "0.1.0")
val isThorDebug = gradle.startParameter.taskNames.any { it.endsWith("thorDebug", ignoreCase = true) }
val buildVersionCode = if (isThorDebug) appVersionCode + 1 else appVersionCode
val buildVersionName = if (isThorDebug) {
    val parts = appVersionName.split(".").map { it.toIntOrNull() ?: 0 }
    "${parts.getOrElse(0) { 0 }}.${parts.getOrElse(1) { 0 }}.${parts.getOrElse(2) { 0 } + 1}"
} else appVersionName

android {
    namespace = "com.thorcompanion"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.thorcompanion"
        minSdk = 26
        targetSdk = 35
        versionCode = buildVersionCode
        versionName = buildVersionName
        resValue("string", "thor_companion_version_name", buildVersionName)
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures { compose = true }

    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}

tasks.register("bumpVersion") {
    doLast {
        val current = Properties().apply {
            if (versionPropsFile.exists()) {
                versionPropsFile.inputStream().use { load(it) }
            }
        }

        val currentCode = (current.getProperty("VERSION_CODE", "1").toIntOrNull() ?: 1)
        val currentName = current.getProperty("VERSION_NAME", "0.1.0")
        val versionParts = currentName.split(".").map { it.toIntOrNull() ?: 0 }
        val nextPatch = if (versionParts.size >= 3) versionParts[2] + 1 else 1
        val nextName = "${versionParts.getOrElse(0) { 0 }}.${versionParts.getOrElse(1) { 0 }}.$nextPatch"
        val nextCode = currentCode + 1

        val updated = Properties().apply {
            setProperty("VERSION_CODE", nextCode.toString())
            setProperty("VERSION_NAME", nextName)
        }

        versionPropsFile.outputStream().use { updated.store(it, null) }
        println("Version bumped to $nextName ($nextCode)")
    }
}

tasks.register("thorDebug") {
    dependsOn("bumpVersion", "assembleDebug")
    doLast {
        val props = Properties().apply {
            if (versionPropsFile.exists()) {
                versionPropsFile.inputStream().use { load(it) }
            }
        }

        val versionName = props.getProperty("VERSION_NAME", "0.1.0")
        val versionCode = props.getProperty("VERSION_CODE", "1")
        val releaseDir = file("$rootDir/releases")
        releaseDir.mkdirs()

        val apkDir = file("$rootDir/app/build/outputs/apk/debug")
        val apkFile = apkDir.listFiles()?.firstOrNull { it.name.endsWith(".apk") }
            ?: throw GradleException("No APK found in ${apkDir.absolutePath}")

        val outputFile = File(releaseDir, "ThorCompanion-v${versionName}-${versionCode}-debug.apk")
        apkFile.copyTo(outputFile, overwrite = true)

        println("APK copied to ${outputFile.absolutePath}")
    }
}

tasks.register("buildThorRelease") {
    dependsOn("thorDebug")
}

val generateEmeraldWildCatalog = tasks.register("generateEmeraldWildCatalog") {
    val output = file("src/main/assets/emerald_wild_encounters.json")
    val speciesOutput = file("src/main/assets/emerald_species_ids.json")
    outputs.file(output)
    outputs.file(speciesOutput)
    doLast {
        output.parentFile.mkdirs()
        URI("https://raw.githubusercontent.com/pret/pokeemerald/master/src/data/wild_encounters.json")
            .toURL()
            .openStream()
            .use { input -> output.outputStream().use { outputStream -> input.copyTo(outputStream) } }
        URI("https://pokeapi.co/api/v2/pokemon?limit=386")
            .toURL()
            .openStream()
            .use { input -> speciesOutput.outputStream().use { outputStream -> input.copyTo(outputStream) } }
        println("Embedded Emerald wild encounters catalog at ${output.relativeTo(projectDir)}")
    }
}

tasks.named("preBuild") {
    dependsOn(generateEmeraldWildCatalog)
}