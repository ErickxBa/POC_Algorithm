plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.erickballas.pruebaconceptoalgoritmolpa"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.erickballas.pruebaconceptoalgoritmolpa"
        minSdk = 24
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.runtime.android)
    implementation(libs.androidx.material3.android)
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // 1. OSM
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("org.osmdroid:osmdroid-wms:6.1.18")

    // 2. Retrofit (Para conectar con Nest.js)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // 3. Corutinas (Para no bloquear la App mientras calcula)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // 4. ViewModel y Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // El BOM (Bill of Materials) ayuda a gestionar las versiones de Compose compatibles
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))

    // Esta es la librería CRÍTICA que te falta para usar 'setContent'
    implementation("androidx.activity:activity-compose:1.9.0")

    // Componentes gráficos básicos de Compose (necesarios para tus pantallas)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3") // Diseño Material 3

    // Navegación en Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Para instanciar ViewModels dentro de composables (viewModel())
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // AGREGA ESTA LÍNEA PARA EL GPS:
    implementation("com.google.android.gms:play-services-location:21.2.0")

}