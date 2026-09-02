import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use(props::load)
}
fun prop(key: String, envKey: String): String =
    (localProps.getProperty(key) ?: System.getenv(envKey) ?: "")

fun signingProp(envKey: String, gradleKey: String): String =
    System.getenv(envKey)
        ?: localProps.getProperty(gradleKey)
        ?: (project.findProperty(gradleKey) as? String ?: "")

android {
    namespace  = "br.com.bliqbrasil.totem"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.com.bliqbrasil.totem"
        minSdk        = 22
        targetSdk     = 35
        versionCode   = 6
        versionName   = "1.0.5"

        buildConfigField("boolean", "STONE_ENABLED", "true")
        buildConfigField("String", "STONE_CODE",
            "\"${prop("stone.code", "STONE_CODE")}\"")
    }

    signingConfigs {
        val storeFilePath = signingProp("MYAPP_RELEASE_STORE_FILE", "MYAPP_RELEASE_STORE_FILE")
        val keyAlias      = signingProp("MYAPP_RELEASE_KEY_ALIAS", "MYAPP_RELEASE_KEY_ALIAS")
        val keyPassword   = signingProp("MYAPP_RELEASE_KEY_PASSWORD", "MYAPP_RELEASE_KEY_PASSWORD")
        val storePwd      = signingProp("MYAPP_RELEASE_STORE_PASSWORD", "MYAPP_RELEASE_STORE_PASSWORD")

        create("gertecGpos700") {
            storeFile     = if (storeFilePath.isNotEmpty()) rootProject.file("app/$storeFilePath") else null
            this.keyAlias      = keyAlias
            this.keyPassword   = keyPassword
            storePassword = storePwd
        }
        create("gertecGposSeries7") {
            storeFile     = if (storeFilePath.isNotEmpty()) rootProject.file("app/$storeFilePath") else null
            this.keyAlias      = keyAlias
            this.keyPassword   = keyPassword
            storePassword = storePwd
        }
        create("positivoStandard") {
            storeFile     = if (storeFilePath.isNotEmpty()) rootProject.file("app/$storeFilePath") else null
            this.keyAlias      = keyAlias
            this.keyPassword   = keyPassword
            storePassword = storePwd
        }
        create("tectoyStandard") {
            storeFile     = if (storeFilePath.isNotEmpty()) rootProject.file("app/$storeFilePath") else null
            this.keyAlias      = keyAlias
            this.keyPassword   = keyPassword
            storePassword = storePwd
        }
        create("sunmiStandard") {
            storeFile     = if (storeFilePath.isNotEmpty()) rootProject.file("app/$storeFilePath") else null
            this.keyAlias      = keyAlias
            this.keyPassword   = keyPassword
            storePassword = storePwd
        }
        create("ingenicoStandard") {
            storeFile     = if (storeFilePath.isNotEmpty()) rootProject.file("app/$storeFilePath") else null
            this.keyAlias      = keyAlias
            this.keyPassword   = keyPassword
            storePassword = storePwd
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "STONE_QRCODE_PROVIDER_ID",
                "\"${prop("stone.qrcode.providerId.prod", "STONE_QRCODE_PROVIDER_ID_PROD")}\"")
            buildConfigField("String", "STONE_QRCODE_AUTHORIZATION",
                "\"${prop("stone.qrcode.authorization.prod", "STONE_QRCODE_AUTHORIZATION_PROD")}\"")
        }
        debug {
            buildConfigField("String", "STONE_QRCODE_PROVIDER_ID",
                "\"${prop("stone.qrcode.providerId.staging", "STONE_QRCODE_PROVIDER_ID_STAGING")}\"")
            buildConfigField("String", "STONE_QRCODE_AUTHORIZATION",
                "\"${prop("stone.qrcode.authorization.staging", "STONE_QRCODE_AUTHORIZATION_STAGING")}\"")
        }
    }

    flavorDimensions += "model"

    productFlavors {
        create("generic") {
            dimension = "model"
            buildConfigField("boolean", "STONE_ENABLED", "false")
        }
        create("gertecGpos700") {
            dimension    = "model"
            signingConfig = signingConfigs.getByName("gertecGpos700")
        }
        create("gertecGposSeries7") {
            dimension    = "model"
            signingConfig = signingConfigs.getByName("gertecGposSeries7")
            ndk { abiFilters.add("armeabi-v7a") }
        }
        create("ingenico") {
            dimension    = "model"
            signingConfig = signingConfigs.getByName("ingenicoStandard")
        }
        create("positivoSeriesL") {
            dimension    = "model"
            signingConfig = signingConfigs.getByName("positivoStandard")
        }
        create("sunmi") {
            dimension    = "model"
            signingConfig = signingConfigs.getByName("sunmiStandard")
        }
        create("sunmiSeriesP") {
            dimension    = "model"
            signingConfig = signingConfigs.getByName("sunmiStandard")
        }
        create("tectoySeriesT") {
            dimension    = "model"
            signingConfig = signingConfigs.getByName("tectoyStandard")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/api_release.kotlin_module"
            excludes += "META-INF/client_release.kotlin_module"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    implementation(libs.security.crypto)

    implementation(libs.stone.sdk) {
        exclude(group = "com.android.support")
    }
    implementation(libs.stone.sdk.posandroid) {
        exclude(group = "com.android.support")
    }

    "tectoySeriesTImplementation"(libs.stone.sdk.posandroid.tectoy) {
        exclude(group = "com.android.support")
    }
    "ingenicoImplementation"(libs.stone.sdk.posandroid.ingenico) {
        exclude(group = "com.android.support")
    }
    "sunmiImplementation"(libs.stone.sdk.posandroid.sunmi) {
        exclude(group = "com.android.support")
    }
    "sunmiSeriesPImplementation"(libs.stone.sdk.posandroid.sunmi) {
        exclude(group = "com.android.support")
    }
    "gertecGpos700Implementation"(libs.stone.sdk.posandroid.gertec) {
        exclude(group = "com.android.support")
    }
    "gertecGposSeries7Implementation"(libs.stone.sdk.posandroid.gertec) {
        exclude(group = "com.android.support")
    }
    "positivoSeriesLImplementation"(libs.stone.sdk.posandroid.positivo) {
        exclude(group = "com.android.support")
    }

    debugImplementation(libs.stone.sdk.debugmode)
}
