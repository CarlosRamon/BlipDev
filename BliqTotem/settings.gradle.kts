import java.util.Properties

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val localProp = Properties()
val localPropFile = file("local.properties")
if (localPropFile.exists()) localPropFile.inputStream().use { localProp.load(it) }

val packageCloudReadToken: String =
    System.getenv("PACKAGECLOUD_READ_TOKEN") ?: localProp.getProperty("packageCloudReadToken", "")
val packageCloudReadTokenInternal: String? =
    System.getenv("PACKAGECLOUD_READ_TOKEN_INTERNAL") ?: localProp.getProperty("packageCloudReadTokenInternal")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://packagecloud.io/priv/$packageCloudReadToken/stone/pos-android/maven2") }
        if (!packageCloudReadTokenInternal.isNullOrEmpty()) {
            maven { url = uri("https://packagecloud.io/priv/$packageCloudReadTokenInternal/stone/pos-android-internal/maven2") }
        }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("https://www.jitpack.io") }
    }
}

rootProject.name = "BliqTotem"
include(":app")
