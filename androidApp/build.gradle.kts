import java.util.Properties

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
}

android {
    namespace = "ru.dvfu.appliances.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    signingConfigs {
        create("release") {
            val storePath = keystoreProps.getProperty("storeFile")
                ?: System.getenv("SCHEDULE_KEYSTORE_FILE")
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = keystoreProps.getProperty("storePassword")
                    ?: System.getenv("SCHEDULE_KEYSTORE_PASSWORD")
                keyAlias = keystoreProps.getProperty("keyAlias")
                    ?: System.getenv("SCHEDULE_KEY_ALIAS")
                keyPassword = keystoreProps.getProperty("keyPassword")
                    ?: System.getenv("SCHEDULE_KEY_PASSWORD")
            }
        }
    }

    defaultConfig {
        applicationId = "ru.dvfu.appliances"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro", getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            extra["enableCrashlytics"] = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":composeApp"))

    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
