plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-parcelize")
    id("maven-publish")
    id("signing")
}

android {
    compileSdk = (properties["android.compileSdk"] as String).toInt()

    defaultConfig {
        minSdk = (properties["android.minSdk"] as String).toInt()
        targetSdk = (properties["android.targetSdk"] as String).toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = properties["version.kotlinCompilerExtension"] as String
    }
}

dependencies {
    implementation("androidx.compose.ui:ui:${properties["version.compose"]}")
    implementation("androidx.compose.foundation:foundation:${properties["version.compose"]}")
    implementation("androidx.compose.animation:animation:${properties["version.compose"]}")
    // For BackHandler
    implementation("androidx.activity:activity-compose:${properties["version.composeActivity"]}")
    implementation("org.jetbrains.kotlin:kotlin-parcelize-runtime:${properties["version.kotlin"]}")
    // For LocalViewModelStoreOwner
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:${properties["version.lifecycle"]}")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}

tasks.withType(Test::class) {
    useJUnitPlatform()
}

val sourceJar by tasks.registering(Jar::class) {
    from(android.sourceSets["main"].java.srcDirs().srcDirs)
    archiveClassifier.set("sources")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            artifact(sourceJar.get())
            artifact("$buildDir/outputs/aar/${artifactId}-${name}.aar")
        }
    }
}