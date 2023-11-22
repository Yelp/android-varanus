@file:Suppress("Unused")

object Versions {
    // Android
    const val COMPILE_SDK = 31
    const val MIN_SDK = 21
    const val TARGET_SDK = 31

    // Java
    const val SOURCE_COMPATIBILITY = 1.8
    const val TARGET_COMPATIBILITY = 1.8

    const val ANDROID_BUILD_TOOLS = "8.1.1" // If you update this you should update LINT as well
    const val ANDROID_CHECK = "1.2.5"
    const val APPCOMPAT = "1.0.0"
    const val CACHE2K = "1.2.2.Final"
    const val COROUTINES = "1.7.3"
    const val CONSTRAINTLAYOUT = "1.1.3"
    const val DESIGN = "1.0.0"
    const val DETEKT = "1.23.3"
    const val GOOGLE_PLAY_SERVICES_PLUGIN = "4.0.1"
    // If you want to update the Gradle version, change this number and then run `./gradlew wrapper`.
    const val GRADLE = "8.4.0"
    const val KOTLIN = "1.9.10"
    const val MAVEN_PUBLISH = "3.6.2"
    const val MAVEN_SETTINGS = "0.5"
    const val OK_HTTP = "4.5.0"
    const val OKIO = "2.2.2"
    const val REALM = "10.15.1"

    const val ESPRESSO = "3.1.0"
    const val JUNIT = "4.12"
    const val MOCKITO = "2.27.0"
    const val MOCKITO_KOTLIN_2 = "2.1.0"
}

private object PlayServicesVersions {
    const val BASEMENT = "15.0.1"
}

object Libs {
    const val CONSTRAINTLAYOUT = "androidx.constraintlayout:constraintlayout:${Versions.CONSTRAINTLAYOUT}"
    const val COROUTINES = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.COROUTINES}"
    const val DETEKT = "io.gitlab.arturbosch.detekt:detekt-cli:${Versions.DETEKT}"
    const val DESIGN = "com.google.android.material:material:${Versions.DESIGN}"
    const val KOTLIN = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.KOTLIN}"
    const val KOTLIN_REFLECT = "org.jetbrains.kotlin:kotlin-reflect:${Versions.KOTLIN}"
    const val OKHTTP = "com.squareup.okhttp3:okhttp:${Versions.OK_HTTP}"
    const val OKIO = "com.squareup.okio:okio:${Versions.OKIO}"
    const val REALM = "io.realm:realm-android-library:${Versions.REALM}"
    const val APPCOMPAT = "androidx.appcompat:appcompat:${Versions.APPCOMPAT}"
}

object PlayServicesLibs {
    const val BASEMENT = "com.google.android.gms:play-services-basement:${PlayServicesVersions.BASEMENT}"
}

object PublishLibs {
    const val MAVEN_PUBLISH = "digital.wup:android-maven-publish:${Versions.MAVEN_PUBLISH}"
    const val MAVEN_SETTINGS =
            "net.linguica.gradle:maven-settings-plugin:${Versions.MAVEN_SETTINGS}"
}

object TestLibs {
    const val ESPRESSO = "androidx.test.espresso:espresso-core:${Versions.ESPRESSO}"
    const val JUNIT = "junit:junit:${Versions.JUNIT}"
    const val KOTLIN_JUNIT = "org.jetbrains.kotlin:kotlin-test-junit:${Versions.KOTLIN}"
    const val MOCKITO_KOTLIN_2 = "com.nhaarman.mockitokotlin2:mockito-kotlin:${Versions.MOCKITO_KOTLIN_2}"
    const val MOCKITO_CORE = "org.mockito:mockito-core:${Versions.MOCKITO}"
}

object BuildScriptLibs {
    const val ANDROID_CHECK = "com.noveogroup.android:check:${Versions.ANDROID_CHECK}"
    const val ANDROID = "com.android.tools.build:gradle:${Versions.ANDROID_BUILD_TOOLS}"
    const val DETEKT = "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${Versions.DETEKT}"
    const val KOTLIN = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.KOTLIN}"
    const val PLAY_SERVICES = "com.google.gms:google-services:${Versions.GOOGLE_PLAY_SERVICES_PLUGIN}"
    const val REALM = "io.realm:realm-gradle-plugin:${Versions.REALM}"
}

object Publishing {
    const val GROUP = "com.yelp.android"
    const val VERSION = "3.0.0"
}

