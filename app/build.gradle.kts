plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val ciRunNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
val testKeystorePath = System.getenv("OCLAX_TEST_KEYSTORE_FILE")
val testKeystorePassword = System.getenv("OCLAX_TEST_KEYSTORE_PASSWORD")
val testKeyAlias = System.getenv("OCLAX_TEST_KEY_ALIAS")
val testKeyPassword = System.getenv("OCLAX_TEST_KEY_PASSWORD")
val hasStableTestSigning =
    !testKeystorePath.isNullOrBlank() &&
        !testKeystorePassword.isNullOrBlank() &&
        !testKeyAlias.isNullOrBlank() &&
        !testKeyPassword.isNullOrBlank()

android {
    namespace = "io.github.hnalvaradohn.oclax"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.hnalvaradohn.oclax"
        minSdk = 26
        targetSdk = 36
        versionCode = ciRunNumber ?: 1
        versionName = if (ciRunNumber != null) "0.1.$ciRunNumber" else "0.1.0"
    }

    signingConfigs {
        if (hasStableTestSigning) {
            create("oclaxTest") {
                storeFile = file(requireNotNull(testKeystorePath))
                storePassword = testKeystorePassword
                keyAlias = testKeyAlias
                keyPassword = testKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (hasStableTestSigning) {
                signingConfig = signingConfigs.getByName("oclaxTest")
            }
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs {
            // Syncthing runs as a child process, so Android must extract the executable .so.
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
