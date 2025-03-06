plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.medpro"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.medpro"
        minSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies { // Latest stable



    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.inappmessaging.display)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.play.services.maps)
    implementation(libs.firebase.database)
    implementation(libs.play.services.location)
    implementation (libs.camera.camera2.v110)        // Camera2 API
    implementation (libs.androidx.camera.lifecycle.v110)      // Lifecycle integration
    implementation (libs.camera.view)
    implementation(libs.firebase.crashlytics.buildtools)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.guava)

    implementation (libs.androidx.camera.video)


            implementation ("org.tensorflow:tensorflow-lite:2.14.0")
            implementation ("org.tensorflow:tensorflow-lite-support:0.4.3")
            implementation ("org.tensorflow:tensorflow-lite-gpu:2.14.0")





    implementation (libs.lottie)
    implementation (libs.material.v190)
    implementation (libs.retrofit)
    implementation (libs.converter.gson)






// ... other androidx dependencies
// add the dependency for the Google AI client SDK for Android




}