plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.castpc"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.castpc"
        minSdk = 29          // 29 = first Android version that can capture game audio
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("com.github.pedroSG94.RootEncoder:library:2.5.5")
}
