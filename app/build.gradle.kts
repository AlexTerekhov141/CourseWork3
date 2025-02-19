

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.packt.myapplication"
    compileSdk = 35
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId = "com.packt.myapplication"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.google.ar:core:1.33.0")
    implementation("com.google.ar:core:1.33.0")
    implementation("com.google.ar.sceneform.ux:sceneform-ux:1.17.1")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation("uk.co.samuelwall:material-tap-target-prompt:3.3.2")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation ("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.tbuonomo:dotsindicator:5.1.0")
    implementation ("org.rajawali3d:rajawali:1.1.970")


    implementation(project(":opencv"))
}