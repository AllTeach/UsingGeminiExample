import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// 1. Load the API Key from secrets.properties
val secrets = Properties()
val secretsFile = rootProject.file("secrets.properties")
if (secretsFile.exists()) {
    secretsFile.inputStream().use { secrets.load(it) }
}
val geminiApiKey = secrets.getProperty("GEMINI_API_KEY", "REPLACE_ME")



android {
    namespace = "com.example.usinggeminiexample"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.usinggeminiexample"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 2. Inject the key into BuildConfig
       buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }
    // 3. Enable BuildConfig generation
    buildFeatures {
        buildConfig = true
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("org.json:json:20230227")
    implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")
    // --- FIX STARTS HERE ---
    // Use the specific Prompt API (Alpha) for Gemini Nano
    implementation("com.google.mlkit:genai-prompt:1.0.0-alpha1")

    // Guava for Java Future support (Required for Java implementation)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}