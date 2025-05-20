plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.drunkdetector"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.drunkdetector"
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

    configurations.all {
        // Force clean resolution of Android Auto dependencies
        resolutionStrategy {
            force("androidx.car.app:app:1.4.0")
            force("androidx.car.app:app-projected:1.4.0")

            // Disable caching for these modules
            cacheDynamicVersionsFor(0, "seconds")
            cacheChangingModulesFor(0, "seconds")
        }
    }
}

dependencies {
    implementation(libs.jdsp)
    implementation(libs.commons.math4.core)
    implementation(libs.onnxruntime.android)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.app)
    implementation ("androidx.car.app:app:1.4.0")
    implementation ("androidx.car.app:app-projected:1.4.0")
    testImplementation ("androidx.car.app:app-testing:1.4.0")
    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

tasks.register("deleteAndroidAutoLibs") {
    doLast {
        // Delete downloaded libs
        delete(fileTree(layout.buildDirectory.dir("intermediates").get()).matching {
            include("**/androidx.car.app/**")
        })

        // Delete cached dependencies
        delete(fileTree("${gradle.gradleUserHomeDir}/caches/modules-2/files-2.1/").matching {
            include("**/androidx.car.app/**")
        })
    }
}

// Make sure clean depends on this task
tasks.named("clean") {
    dependsOn("deleteAndroidAutoLibs")
}
