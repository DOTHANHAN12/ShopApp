// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Top-level build file
        alias(libs.plugins.android.application) apply false
        // THÊM DÒNG NÀY VÀO:
        id("com.google.gms.google-services") version "4.4.2" apply false
    }

