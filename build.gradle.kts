plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dz.baladiya.check"
    compileSdk = 34

    defaultConfig {
        applicationId = "dz.baladiya.check"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    // ملفات tessdata كبيرة: لا تُضغط حتى يمكن نسخها بسرعة
    androidResources {
        noCompress += "traineddata"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.core:core-ktx:1.13.1")

    // قراءة اتجاه الصورة الملتقطة
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // قراءة صفحة المكتبة الخارجية (cb-maktaba.net)
    implementation("org.jsoup:jsoup:1.17.2")

    // OCR عربي دون إنترنت (Tesseract)
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.7.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
}
