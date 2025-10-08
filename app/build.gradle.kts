plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "1.9.10"
}

import org.gradle.api.Task

android {
    namespace = "com.example.todolist2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.todolist2"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    
    // APK 파일명 변경 (더 간단한 방법)
    setProperty("archivesBaseName", "todo2")
}

// APK 파일명 변경
tasks.register("renameApk") {
    dependsOn("assembleDebug")
    doLast {
        val apkDir = File("${project.layout.buildDirectory.get().asFile}/outputs/apk/debug/")
        val oldFile = File(apkDir, "app-debug.apk")
        val newFile = File(apkDir, "todo2_debug.apk")
        println("Looking for APK at: ${oldFile.absolutePath}")
        if (oldFile.exists()) {
            val renamed = oldFile.renameTo(newFile)
            if (renamed) {
                println("✅ APK renamed to: ${newFile.name}")
            } else {
                println("❌ Failed to rename APK")
            }
        } else {
            println("❌ APK file not found: ${oldFile.name}")
        }
    }
}

tasks.register("renameReleaseApk") {
    dependsOn("assembleRelease")
    doLast {
        val apkDir = File("${project.buildDir}/outputs/apk/release/")
        val oldFile = File(apkDir, "app-release.apk")
        val newFile = File(apkDir, "todo2_release.apk")
        if (oldFile.exists()) {
            oldFile.renameTo(newFile)
            println("APK renamed to: ${newFile.name}")
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // 음성 인식 의존성
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // JSON 직렬화 의존성
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}