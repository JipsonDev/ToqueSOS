plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.toquesos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.toquesos"
        minSdk = 30
        targetSdk = 35
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
    buildFeatures {
        viewBinding = true
    }
        kotlinOptions {
        jvmTarget = "11"
    }
    testOptions {
        unitTests.isIncludeAndroidResources = false
        unitTests.all {
            it.enabled = false // desactiva unit tests temporalmente
        }
    }
}

dependencies {
    // Dependencias principales (usando libs o versiones específicas)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Dependencias específicas para ToqueSOS
    implementation ("androidx.recyclerview:recyclerview:1.3.2")
    implementation ("androidx.biometric:biometric:1.1.0")
    implementation ("androidx.activity:activity-ktx:1.10.1")
    implementation ("androidx.fragment:fragment-ktx:1.6.2")
    implementation ("com.google.code.gson:gson:2.10.1")


    // Para mejor manejo de ubicación (opcional)
    implementation ("com.google.android.gms:play-services-location:21.0.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

