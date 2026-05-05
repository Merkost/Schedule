import java.util.Properties

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val fcmServerKey: String = localProps.getProperty("FCM_SERVER_KEY")
    ?: System.getenv("FCM_SERVER_KEY")
    ?: ""

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "ru.dvfu.appliances"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    sourceSets {
        named("main") {
            java.srcDirs("src/androidMain/kotlin", "src/commonMain/kotlin")
            res.srcDirs("src/androidMain/res")
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
        }
    }

    buildTypes {
        release {
            buildConfigField("boolean", "USE_MOCK_REPOS", "false")
            buildConfigField("String", "FCM_SERVER_KEY", "\"$fcmServerKey\"")
        }
        debug {
            buildConfigField("boolean", "USE_MOCK_REPOS", "false")
            buildConfigField("String", "FCM_SERVER_KEY", "\"$fcmServerKey\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.coroutines.play.services)
    api(libs.kotlinx.datetime)
    api(libs.kotlinx.serialization.json)

    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api(libs.google.material)
    api(libs.androidx.constraintlayout)
    api(libs.androidx.splashscreen)
    api(libs.androidx.preference.ktx)
    api(libs.androidx.work.runtime.ktx)

    api(libs.androidx.navigation.fragment.ktx)
    api(libs.androidx.navigation.ui.ktx)
    api(libs.jetbrains.navigation.compose)

    api(libs.androidx.activity.compose)
    api(libs.jetbrains.lifecycle.viewmodel)
    api(libs.jetbrains.lifecycle.viewmodel.compose)
    api(libs.androidx.lifecycle.viewmodel.ktx)
    api(libs.androidx.lifecycle.livedata.ktx)
    api(libs.androidx.datastore.preferences)

    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.foundation.layout)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.util)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material3.window.size)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.androidx.compose.animation)
    api(libs.androidx.compose.ui.tooling)
    api(libs.androidx.constraintlayout.compose)
    api(libs.lottie.compose)

    debugImplementation(libs.androidx.compose.ui.test.manifest)

    api(libs.koin.core)
    api(libs.koin.compose)
    api(libs.koin.compose.viewmodel)
    api(libs.koin.android)
    api(libs.koin.android.compat)
    api(libs.koin.androidx.workmanager)
    api(libs.koin.androidx.compose)

    api(libs.accompanist.permissions)

    api(libs.glide)
    api(libs.coil.compose)

    api(libs.retrofit)
    api(libs.retrofit.converter.gson)
    api(libs.okhttp.logging.interceptor)
    api(libs.gson)

    api(platform(libs.firebase.bom))
    api(libs.firebase.messaging)
    api(libs.firebase.analytics)
    api(libs.firebase.crashlytics)
    api(libs.firebase.perf)
    api(libs.firebase.auth)
    api(libs.firebase.firestore)
    api(libs.firebase.database)
    api(libs.firebase.storage)
    api(libs.firebase.inappmessaging.display)
    api(libs.firebase.ui.auth)
    api(libs.play.services.auth)

    api(libs.compose.calendar)

    debugImplementation(libs.leakcanary)
}
