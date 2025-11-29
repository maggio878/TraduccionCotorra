plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.traduccioncotorra"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.traduccioncotorra"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

// Para PDFs
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
// Para Word
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
// Para documentos
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.legacy.support.v4)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Componentes de navegacion
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")
    implementation("com.google.android.material:material:1.12.0")

    // Componentes de traduccion
    implementation("com.google.mlkit:translate:17.0.3")
    //Identificacion de texto
    implementation("com.google.mlkit:text-recognition:16.0.1")
    // ML Kit Language ID (para detectar idioma)
    implementation("com.google.mlkit:language-id:17.0.6")

    // Componentes para graficas
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Componentes para camara
    val camerax_version = "1.4.1"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")
    implementation("androidx.camera:camera-extensions:${camerax_version}")
}