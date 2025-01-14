import com.android.build.api.dsl.Packaging

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
}

android {
    namespace = "com.example.ftp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ftp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    lint {
        baseline = file("lint-baseline.xml")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        dataBinding = true
        viewBinding = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "META-INF/gradle/incremental.annotation.processors"
            excludes += "META-INF/DEPENDENCIES"
        }
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation(libs.guolindev.permissionx)
    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.converter.gson)
    implementation(libs.google.gson)
    implementation(libs.loggingInterceptor)
    implementation(libs.jakewharton.timber)
//    implementation(libs.tencent.mm.opensdk)
    implementation(libs.auto.value.gson)
    implementation(libs.androidx.activity) // AutoValue Gson 扩展库
    kapt(libs.auto.value)  // AutoValue 注解处理器
    kapt(libs.auto.value.gson)   // Gson 扩展的注解处理器
    implementation(libs.zxing.core)  // ZXing 核心库
    implementation(libs.zxing.android.embedded)  // ZXing Android 嵌入式库
    implementation(libs.jsoup)
    implementation(libs.glide)  // Glide 的核心库
    kapt(libs.compiler)  // Glide 的注解处理器，用于生成 Glide API
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.compressor)
    implementation(libs.material.dialogs.core)
    implementation(libs.androidpicker.common)
    implementation(libs.androidpicker.wheelpicker)
//    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.lottie)
    implementation(libs.androidx.viewpager2)

    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)


    // sftp
    implementation(libs.jsch)//ssh
    implementation(libs.sshd.sftp)
    implementation(libs.sshd.core)
    implementation(libs.mina.core)

    //implementation(libs.sshj)

    implementation(libs.eventbus)


    // debug的时候导入，需要日志
//    debugImplementation(libs.slf4j.api) // SLF4J API
//    debugImplementation(libs.slf4j.simple)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}