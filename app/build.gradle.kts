plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.zak.inventory"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.zak.inventory"
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

    // Room Database
    implementation(libs.room)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    annotationProcessor(libs.room.compiler)
    implementation(libs.room.rxjava3)

    // RxJava & RxAndroid for reactive programming - makes multithreading easy
    implementation(libs.rxjava3)
    implementation(libs.rxandroid)

    // Zxing Android Embedded for writing and reading QR codes
    implementation(libs.zxing.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}