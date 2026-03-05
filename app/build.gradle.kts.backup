plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("androidx.compose.compiler.plugins.kotlin")
    id("com.google.gms.google-services")
}

android {
    lint {
        baseline = file("lint-baseline.xml")
        lintConfig = file("lint.xml")
        abortOnError = false
        checkReleaseBuilds = false
    }
    namespace = "fm.mrc.cloudassignment"
    compileSdk = 35

    defaultConfig {
        applicationId = "fm.mrc.cloudassignment"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Added to fix lint error
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // Compose Compiler
    implementation("androidx.compose.compiler:compiler:1.5.8")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // Image loading with Coil
    implementation("io.coil-kt:coil-compose:2.4.0")
    
    // Biometric authentication
    implementation("androidx.biometric:biometric:1.1.0")
    
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.3"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // We'll implement our own chart visualization for now
    // implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
    // Added for lint fix
    lint {
        baseline = file("lint-baseline.xml")
    }








