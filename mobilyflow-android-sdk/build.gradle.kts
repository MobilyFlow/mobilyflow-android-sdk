import org.jreleaser.gradle.plugin.tasks.AbstractJReleaserTask

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    id("org.jreleaser") version ("1.16.0")
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

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.mobilyflow.mobilypurchasesdk"
            artifactId = "mobilyflow-android-sdk"
            version = "0.0.1"

            afterEvaluate {
                from(components.findByName("release"))
            }

            pom {
                name.set("MobilyFlow Android SDK")
                description.set("MobilyFlow Android SDK")
                url.set("https://www.mobilyflow.com")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("MobilyFlow")
                        name.set("MobilyFlow")
                        email.set("dev@mobilyflow.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/MobilyFlow/mobilyflow-android-sdk.git")
                    developerConnection.set("scm:git:ssh://github.com:MobilyFlow/mobilyflow-android-sdk.git")
                    url.set("https://github.com/MobilyFlow/mobilyflow-android-sdk")
                }
            }
        }
    }
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("staging-deploy").get())
        }
    }
}

jreleaser {
    signing {
        setActive("ALWAYS")
        armored = true
        setMode("FILE")
        publicKey = "/Users/gtaja/Projects/MobilyFlow/mobilyflow-signatures/public.pgp"
        secretKey = "/Users/gtaja/Projects/MobilyFlow/mobilyflow-signatures/private.pgp"
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    setActive("ALWAYS")
                    uri("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository("target/staging-deploy")
                }
            }
        }
    }
}