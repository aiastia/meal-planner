plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.aiastia.mealplanner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aiastia.mealplanner"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "2.1"
    }

    // 固定签名：CI 用 GitHub Secrets 里的密钥库签名，保证每次构建签名一致、可直接覆盖安装
    val ksFile = System.getenv("STORE_FILE")
    if (ksFile != null) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(ksFile)
                storePassword = System.getenv("STORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (ksFile != null) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
