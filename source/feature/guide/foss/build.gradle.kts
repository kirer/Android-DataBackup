plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.hilt)
    alias(libs.plugins.library.compose)
}

android {
    namespace = "com.xayah.feature.guide.foss"
}

dependencies {
    // Core
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:util"))
    implementation(project(":core:datastore"))

    // Common
    implementation(project(":feature:guide:common"))

    // Compose Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Preferences DataStore
    implementation(libs.androidx.datastore.preferences)

    // libsu
    implementation(libs.libsu.core)
}