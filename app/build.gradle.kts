import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.example.dailysnapshot"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.dailysnapshot"
        minSdk = 29
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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // CameraX
    implementation(libs.androidx.camerax.core)
    implementation(libs.androidx.camerax.camera2)
    implementation(libs.androidx.camerax.lifecycle)
    implementation(libs.androidx.camerax.view)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx) // Kotlin Extensions & Coroutines support
    ksp(libs.androidx.room.compiler) // Kotlin Symbol Processing (KSP)

    // WorkManager (For daily reminders)
    implementation(libs.androidx.work.runtime)

    // Koin (Dependency Injection)
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // Image Processing & Loading
    implementation(libs.coil.compose)

    // Coroutines & Flow
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Material Icons Extended
    implementation(libs.androidx.compose.material.extended)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)
}