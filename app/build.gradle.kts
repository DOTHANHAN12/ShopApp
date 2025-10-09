import org.gradle.kotlin.dsl.implementation
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.version

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    // Không cần thêm plugin kotlin("kapt") nữa!
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
    implementation("androidx.recyclerview:recyclerview:1.3.2")
// CardView (dùng trong item_product.xml)
    implementation("androidx.cardview:cardview:1.0.0")
// Thư viện Picasso (tải ảnh)
    implementation("com.squareup.picasso:picasso:2.8")
    // 1. Firebase BoM (Đảm bảo chỉ có 1 lần và gộp các khối)
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))

    // 2. Thư viện AndroidX và UI
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Core libs (đã giữ nguyên từ file gốc)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase Services (Đã dọn dẹp và hợp nhất các dòng bị lặp)
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database") // Realtime Database
    implementation("com.google.firebase:firebase-firestore") // Cloud Firestore

    // -----------------------------------------------------------------
    // GIẢI PHÁP THAY THẾ: SỬ DỤNG PICASSO (KHÔNG CẦN KAPT)
    // -----------------------------------------------------------------
    implementation("com.squareup.picasso:picasso:2.8")

    // -----------------------------------------------------------------

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}