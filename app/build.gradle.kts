import org.gradle.kotlin.dsl.implementation

import org.gradle.kotlin.dsl.project

import org.gradle.kotlin.dsl.version

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.shopapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.shopapp"
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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // -----------------------------------------------------------------
    // 1. FIREBASE & GOOGLE SERVICES
    // -----------------------------------------------------------------
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.code.gson:gson:2.10.1")

    // Firebase Core Services
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database") // Realtime Database
    implementation("com.google.firebase:firebase-firestore") // Cloud Firestore

    // -----------------------------------------------------------------
    // 2. ANDROIDX CORE & UI COMPONENTS
    // -----------------------------------------------------------------

    // Core libs (libs.xxx giữ nguyên)
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // UI/Widgets
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Material Design (Rất quan trọng cho AppBarLayout/CoordinatorLayout)
    implementation(libs.material) // Dùng cái libs này nếu nó là phiên bản mới
    // HOẶC dùng version cố định để chắc chắn: implementation("com.google.android.material:material:1.12.0")

    // Component BẮT BUỘC cho HomeActivity và ProductDetailActivity
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // Thư viện Picasso (tải ảnh)
    implementation("com.squareup.picasso:picasso:2.8")

    // -----------------------------------------------------------------
    // 3. TESTING
    // -----------------------------------------------------------------
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}