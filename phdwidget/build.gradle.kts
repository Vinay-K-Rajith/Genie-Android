plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.entab.phdwidget"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // AGP requires explicitly opting a variant into a software component before it can be
    // published — without this, components["release"] below does not exist.
    publishing {
        singleVariant("release")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.9.0")
}

// JitPack invokes `gradle -Pgroup=<group> -Pversion=<tag> publishToMavenLocal`. Without this
// block there is no publishToMavenLocal task on this module at all, so JitPack has nothing to
// publish regardless of which tag/commit is requested.
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = (findProperty("group") as String?) ?: "com.github.Vinay-K-Rajith.Genie-Android"
                artifactId = "phdwidget"
                version = (findProperty("version") as String?)?.takeIf { it.isNotBlank() && it != "unspecified" } ?: "0.0.1-local"
            }
        }
    }
}
