import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val configuracionLocal = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}

fun configuracionApi(gradleKey: String, environmentKey: String, localKey: String, default: String): String =
    providers.gradleProperty(gradleKey).orNull
        ?: providers.environmentVariable(environmentKey).orNull
        ?: configuracionLocal.getProperty(localKey)
        ?: default

fun comoLiteralBuildConfig(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.tecsup.atelier"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.tecsup.atelier"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "API_BASE_URL",
            comoLiteralBuildConfig(
                configuracionApi(
                    "atelierApiBaseUrl",
                    "ATELIER_API_BASE_URL",
                    "atelier.api.baseUrl",
                    "http://10.0.2.2:8080/"
                )
            )
        )
        buildConfigField(
            "String",
            "API_USER",
            comoLiteralBuildConfig(
                configuracionApi("atelierApiUser", "ATELIER_API_USER", "atelier.api.user", "")
            )
        )
        buildConfigField(
            "String",
            "API_PASSWORD",
            comoLiteralBuildConfig(
                configuracionApi("atelierApiPassword", "ATELIER_API_PASSWORD", "atelier.api.password", "")
            )
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
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

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Retrofit & Networking para conexion API IA
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}