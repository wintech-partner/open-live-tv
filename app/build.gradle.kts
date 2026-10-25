plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.wintech_partner.openlivetv"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.wintech_partner.openlivetv"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependenciesInfo {
        // F-Droid and reproducible builds: no signed dependency metadata blob in the APK.
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)
}
