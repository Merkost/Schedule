@file:Suppress("DEPRECATION")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

val generateAppBuildConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/source/appBuildConfig/commonMain/kotlin")
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("ru/dvfu/appliances/AppBuildConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package ru.dvfu.appliances

            object AppBuildConfig {
                const val USE_MOCK_REPOS: Boolean = false
            }
            """.trimIndent() + "\n"
        )
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidLibrary {
        namespace = "ru.dvfu.appliances"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        androidResources {
            enable = true
        }

        withHostTestBuilder {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_21)
                }
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            binaryOption("bundleId", "ru.dvfu.appliances.composeApp")
            export(libs.kmpnotifier)
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            kotlin.srcDir(generateAppBuildConfig.map { it.outputs.files.singleFile })
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.androidx.datastore.preferences.core)

            implementation(libs.jetbrains.navigation.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            api(libs.kmpnotifier)

            implementation(libs.gitlive.firebase.auth)
            implementation(libs.gitlive.firebase.firestore)
            implementation(libs.gitlive.firebase.database)
            implementation(libs.gitlive.firebase.storage)
            implementation(libs.gitlive.firebase.analytics)
            implementation(libs.gitlive.firebase.crashlytics)
            implementation(libs.gitlive.firebase.config)
            implementation(libs.gitlive.firebase.common)
            implementation(libs.gitlive.firebase.functions)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            implementation(libs.kizitonwose.calendar)

            implementation(libs.moko.permissions.compose)
            implementation(libs.moko.permissions.notifications)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.kotlinx.coroutines.play.services)

            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)
            implementation(libs.google.material)
            implementation(libs.androidx.constraintlayout)
            implementation(libs.androidx.splashscreen)
            implementation(libs.androidx.preference.ktx)


            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.viewmodel.ktx)
            implementation(libs.androidx.lifecycle.livedata.ktx)

            implementation(libs.ktor.client.okhttp)

            implementation(dependencies.platform(libs.androidx.compose.bom))
            implementation("androidx.compose.ui:ui-tooling") {
                exclude(group = "androidx.compose.ui", module = "ui-tooling-preview")
            }
            implementation(libs.androidx.compose.ui.util)
            implementation(libs.androidx.compose.material3.window.size)
            implementation(libs.androidx.compose.animation)

            implementation(libs.koin.android)
            implementation(libs.koin.android.compat)
            implementation(libs.koin.androidx.compose)

            implementation(libs.accompanist.permissions)

            implementation(dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
            implementation(libs.firebase.perf)
            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.database)
            implementation(libs.firebase.storage)
            implementation(libs.firebase.inappmessaging.display)
            implementation(libs.play.services.auth)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)
        }
    }
}


compose.resources {
    publicResClass = true
    packageOfResClass = "ru.dvfu.appliances.generated.resources"
    generateResClass = always
}
