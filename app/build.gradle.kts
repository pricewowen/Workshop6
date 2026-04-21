import java.util.Properties
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

/**
 * API base URL; override in local.properties as api.base.url=
 * Defaults to the deployed Workshop 7 API.
 */
val apiBaseUrl = (localProperties.getProperty("api.base.url") ?: "https://peelin-good-kdeft.ondigitalocean.app/")
    .trim()
    .let { if (it.endsWith("/")) it else "$it/" }

val stripePublishableKey = (localProperties.getProperty("stripe.publishable.key") ?: "")

android {
    buildFeatures {
        buildConfig = true
    }
    namespace = "com.example.workshop6"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.workshop6"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl.replace("\"", "\\\"")}\"")
        buildConfigField("String", "STRIPE_PUBLISHABLE_KEY", "\"${stripePublishableKey.replace("\"", "\\\"")}\"")
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

    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // RecyclerView
    implementation(libs.recyclerview)

    // GPS
    implementation(libs.play.services.location)
    implementation(libs.security.crypto)

    // Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.gson)
    implementation(libs.glide)
    implementation(libs.browser)
    implementation(libs.stripe.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

/**
 * Plain "Generate Javadoc" in the IDE does not put android.jar or AndroidX on the classpath.
 * Run this instead: ./gradlew :app:androidJavadoc
 * Output: app/build/docs/javadoc/index.html
 */
tasks.register<Javadoc>("androidJavadoc") {
    group = "documentation"
    description = "Javadoc for main Java sources with Android SDK and debug compile classpath"

    dependsOn(tasks.named("compileDebugJavaWithJavac"))

    val javaCompile = tasks.named("compileDebugJavaWithJavac", JavaCompile::class.java).get()
    source = javaCompile.source
    // AGP 9 new DSL does not expose BaseExtension; javac already has android.jar on bootstrapClasspath.
    doFirst {
        classpath = project.objects.fileCollection().from(
            javaCompile.classpath,
            javaCompile.options.bootstrapClasspath,
        )
    }

    val outDir = layout.buildDirectory.dir("docs/javadoc").get().asFile
    setDestinationDir(outDir)

    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)

    isFailOnError = false
}