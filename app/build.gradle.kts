plugins { id("com.android.application") }
android {
    namespace = "com.example.login"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.zakk.travel"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"
        testInstrumentationRunner = "com.example.login.SmokeTests"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_1_8; targetCompatibility = JavaVersion.VERSION_1_8 }
}
dependencies { implementation("androidx.recyclerview:recyclerview:1.3.2") }
