plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.example.warehousescan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.warehousescan"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions.add("mode")
    productFlavors {
        create("dev") {
            dimension = "mode"
            applicationIdSuffix = ".dev"
        }
        create("uat") {
            dimension = "mode"
            applicationIdSuffix = ".uat"
        }
        create("prod") {
            dimension = "mode"
        }
        create("demo") {
            dimension = "mode"
            applicationIdSuffix = ".demo"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

detekt {
    config.setFrom(files("$rootDir/detekt.yml"))
    buildUponDefaultConfig = true
}

tasks.register("assembleDebugUnitTest") {
    description = "Assembles unit test code for all Debug flavors."
    group = "build"
    dependsOn(
        "assembleDevDebugUnitTest",
        "assembleDemoDebugUnitTest",
        "assembleProdDebugUnitTest",
        "assembleUatDebugUnitTest"
    )
}

tasks.register("testDebugUnitTest") {
    description = "Runs unit tests for all Debug flavors."
    group = "verification"
    dependsOn(
        "testDevDebugUnitTest",
        "testDemoDebugUnitTest",
        "testProdDebugUnitTest",
        "testUatDebugUnitTest"
    )
}

tasks.register("assembleReleaseUnitTest") {
    description = "Assembles unit test code for all Release flavors."
    group = "build"
    dependsOn(
        "assembleDevReleaseUnitTest",
        "assembleDemoReleaseUnitTest",
        "assembleProdReleaseUnitTest",
        "assembleUatReleaseUnitTest"
    )
}

tasks.register("testReleaseUnitTest") {
    description = "Runs unit tests for all Release flavors."
    group = "verification"
    dependsOn(
        "testDevReleaseUnitTest",
        "testDemoReleaseUnitTest",
        "testProdReleaseUnitTest",
        "testUatReleaseUnitTest"
    )
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.tink.android)
    implementation(libs.timber)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.room.testing)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.compose.ui.tooling)
}
