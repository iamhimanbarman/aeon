import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.exists()) propertiesFile.inputStream().use(::load)
}

fun String.asBuildConfigString(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val defaultAuthBaseUrl = "https://aeon-9cds.onrender.com"

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.aeon.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aeon.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Direct API key usage is only for personal/debug use. Do not ship this in production.
            buildConfigField(
                "String",
                "BEDROCK_API_KEY",
                localProperties.getProperty("BEDROCK_API_KEY", "").asBuildConfigString()
            )
            buildConfigField(
                "String",
                "BEDROCK_REGION",
                localProperties.getProperty("BEDROCK_REGION", "us-east-1").asBuildConfigString()
            )
            buildConfigField(
                "String",
                "AUTH_BASE_URL",
                localProperties.getProperty("AUTH_BASE_URL", defaultAuthBaseUrl).asBuildConfigString()
            )
            buildConfigField(
                "String",
                "AUTH_MOBILE_REDIRECT_URI",
                localProperties.getProperty("AUTH_MOBILE_REDIRECT_URI", "aeon://auth/callback").asBuildConfigString()
            )
            buildConfigField("boolean", "DIRECT_CLOUD_AI_ENABLED", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("String", "BEDROCK_API_KEY", "\"\"")
            buildConfigField("String", "BEDROCK_REGION", "\"us-east-1\"")
            buildConfigField(
                "String",
                "AUTH_BASE_URL",
                localProperties.getProperty("AUTH_BASE_URL", defaultAuthBaseUrl).asBuildConfigString()
            )
            buildConfigField("String", "AUTH_MOBILE_REDIRECT_URI", "\"aeon://auth/callback\"")
            buildConfigField("boolean", "DIRECT_CLOUD_AI_ENABLED", "false")
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        optIn.add("androidx.compose.animation.ExperimentalAnimationApi")
        optIn.add("androidx.compose.foundation.ExperimentalFoundationApi")
        optIn.add("androidx.compose.foundation.layout.ExperimentalLayoutApi")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

ksp {
    arg("room.schemaLocation", file("schemas").absolutePath)
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

dependencies {
    // Compose Foundation 1.10+ removes the value-based BasicTextField API used by
    // the current design system. Keep this on the latest compatible 1.9 line.
    val composeBom = platform("androidx.compose:compose-bom:2025.10.01")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.lifecycle:lifecycle-process:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    ksp("androidx.room:room-compiler:2.8.4")

    implementation("androidx.work:work-runtime-ktx:2.11.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.3.20")
    testImplementation("org.json:json:20260522")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
