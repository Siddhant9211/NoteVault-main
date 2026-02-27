plugins {
    alias(libs.plugins.android.application)
    // Apply Google services plugin in the app module
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.notevault"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.notevault"
        minSdk = 24
        targetSdk = 34
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // Firebase BOM to manage Firebase library versions
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    // Firebase Authentication (Email/Password only)
    implementation("com.google.firebase:firebase-auth")
    // Cloud Firestore
    implementation("com.google.firebase:firebase-firestore")
    // Firebase Storage (profile photo)
    implementation("com.google.firebase:firebase-storage")
    
    // Lifecycle components for ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")
    
    // RecyclerView and CardView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    
    // CoordinatorLayout for Material Design layouts
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    // Glide for loading profile images
    implementation("com.github.bumptech.glide:glide:4.16.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}