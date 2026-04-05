plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.joi.ghosthand"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.joi.ghosthand"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val releaseConfig = maybeCreate("release")
        val storeFilePath = providers.gradleProperty("GHOSTHAND_RELEASE_STORE_FILE").orNull
        if (storeFilePath != null) {
            releaseConfig.storeFile = file(storeFilePath)
            releaseConfig.storePassword = providers.gradleProperty("GHOSTHAND_RELEASE_STORE_PASSWORD").get()
            releaseConfig.keyAlias = providers.gradleProperty("GHOSTHAND_RELEASE_KEY_ALIAS").get()
            releaseConfig.keyPassword = providers.gradleProperty("GHOSTHAND_RELEASE_KEY_PASSWORD").get()
        }
    }

    buildTypes {
        debug {
            signingConfig = null
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (providers.gradleProperty("GHOSTHAND_RELEASE_STORE_FILE").orNull != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
