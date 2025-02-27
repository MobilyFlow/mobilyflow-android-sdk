import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)

    id("com.vanniktech.maven.publish") version ("0.30.0")
}

android {
    namespace = "com.mobilyflow.mobilypurchasesdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.datetime)
    implementation(libs.okhttp)
    implementation(libs.billing)
}

//project.setProperty("signingInMemoryKey", System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey"))
//project.setProperty("signingInMemoryKeyId", System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyId"))
//project.setProperty("signingInMemoryKeyPassword", System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyPassword"))

//println("signingInMemoryKey: " + project.findOptionalProperty("signingInMemoryKey"))
//println("signingInMemoryKeyId: " + project.findProperty("signingInMemoryKeyId"))
//println("signingInMemoryKeyPassword: " + project.findProperty("signingInMemoryKeyPassword"))

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()

    coordinates("com.mobilyflow.mobilypurchasesdk", "mobilyflow-android-sdk", "0.0.1")

    pom {
        name.set("MobilyFlow Android SDK")
        description.set("MobilyFlow Android SDK")
        inceptionYear.set("2025")
        url.set("https://github.com/MobilyFlow/mobilyflow-android-sdk")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("MobilyFlow")
                name.set("MobilyFlow")
                url.set("https://github.com/MobilyFlow/")
            }
        }
        scm {
            url.set("https://github.com/MobilyFlow/mobilyflow-android-sdk")
            connection.set("scm:git:git://github.com/MobilyFlow/mobilyflow-android-sdk.git")
            developerConnection.set("scm:git:ssh://git@github.com/MobilyFlow/mobilyflow-android-sdk.git")
        }
    }
}