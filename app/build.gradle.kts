plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.zenpeartree.karoopoimap"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zenpeartree.karoopoimap"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../release.keystore")
            storePassword = providers.gradleProperty("POI_KEYSTORE_PASSWORD")
                .orElse(providers.environmentVariable("POI_KEYSTORE_PASSWORD"))
                .getOrElse("")
            keyAlias = "karoo-poi"
            keyPassword = storePassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("io.hammerhead:karoo-ext:1.1.8")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    implementation("ch.hsr:geohash:1.4.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20231013")
}
