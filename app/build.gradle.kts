import java.net.URI

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val newpipeExtractorVersion: String by project

android {
    namespace = "pt.tubeaudio"
    compileSdk = 36
    defaultConfig {
        applicationId = "pt.tubeaudio"
        minSdk = 26
        targetSdk = 36
        versionCode = 11
        versionName = "0.9.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
    packaging.resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1")
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-session:1.8.0")
    implementation("org.schabi.newpipe:extractor:$newpipeExtractorVersion")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    testImplementation("junit:junit:4.13.2")
}

tasks.register("checkNewPipeUpdate") {
    group = "maintenance"
    doLast {
        val current = newpipeExtractorVersion
        val text = URI("https://api.github.com/repos/TeamNewPipe/NewPipeExtractor/releases/latest")
            .toURL().openConnection().apply {
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "TubeAudio")
                connectTimeout = 5000
                readTimeout = 5000
            }.getInputStream().bufferedReader().use { it.readText() }
        val latest = Regex(""tag_name"\s*:\s*"v([^"]+)"").find(text)?.groupValues?.get(1)
            ?: error("Could not read latest release")
        println("NewPipeExtractor current=$current latest=$latest")
        if (latest != current && providers.gradleProperty("apply").orNull == "true") {
            val f = rootProject.file("gradle.properties")
            f.writeText(f.readText().replace(Regex("(?m)^newpipeExtractorVersion=.*$"),
                "newpipeExtractorVersion=$latest"))
            println("Updated NewPipeExtractor to $latest")
        }
    }
}
