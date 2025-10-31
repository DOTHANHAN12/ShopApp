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
    implementation("com.google.firebase:firebase-storage") // Firebase Storage
    implementation("com.google.firebase:firebase-messaging") // Firebase Cloud Messaging

    // -----------------------------------------------------------------
    // 2. ANDROIDX CORE & UI COMPONENTS
    // -----------------------------------------------------------------

    // Core libs (from version catalog)
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.material) // Material Design

    // UI/Widgets
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Required for HomeActivity and ProductDetailActivity
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // Image Loading
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")

    // -----------------------------------------------------------------
    // 3. TESTING
    // -----------------------------------------------------------------
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
