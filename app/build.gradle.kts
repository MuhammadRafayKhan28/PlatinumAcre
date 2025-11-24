plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.platinumacre.realestateapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.platinumacre.realestateapp"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures{
        buildConfig = true
        viewBinding =  true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.activity:activity:1.12.0")
    implementation("com.google.android.material:material:1.13.0")

    //Firebase
    implementation("com.google.firebase:firebase-auth:24.0.1")
    implementation("com.google.firebase:firebase-database:22.0.1")
    implementation("com.google.firebase:firebase-storage:22.0.1")
    implementation("com.google.android.gms:play-services-auth:21.4.0")

    //Google MAP & Search Places Libs
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.android.libraries.places:places:4.4.1")

    //Country code picker: https://github.com/hbb20/CountryCodePickerProject
    implementation("com.hbb20:ccp:2.7.3")
    //media management and image loading framework: https://github.com/bumptech/glide
    implementation("com.github.bumptech.glide:glide:5.0.5")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}