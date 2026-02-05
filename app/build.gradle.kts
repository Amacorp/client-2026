plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    // این آدرس برای شناسایی فایل‌های کاتلین شماست و نباید تغییر کند
    namespace = "com.mette.vpn"
    compileSdk = 35

    defaultConfig {
        // این بخش را تغییر دادیم تا با امضای فایل‌های .so هماهنگ شود
        applicationId = "com.v2ray.ang"
        minSdk = 26
        targetSdk = 34 // اندروید 15 هنوز در برخی کتابخانه‌ها ناپایدار است، 34 امن‌تر است
        versionCode = 1
        versionName = "2.0-PRO"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // فیلتر کردن معماری‌ها برای کاهش حجم APK و سازگاری بهتر
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
            abiFilters.add("x86_64")
        }
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

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            // اطمینان از اینکه اندروید استودیو مسیر فایل‌های .so را می‌شناسد
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // این بخش اصلاح شده تا تمام فایل‌های متداخل پوشش داده شوند
            pickFirsts += "**/libgojni.so"
            pickFirsts += "**/libtun2socks.so"
            pickFirsts += "**/libhev-socks5-tunnel.so"
            pickFirsts += "**/libhysteria2.so"
            pickFirsts += "**/libmmkv.so"
        }
    }
}

dependencies {
    // کتابخانه داخلی شما (اگر ارور داد، این خط را بررسی کن که نام پروژه درست باشد)
    implementation(project(":my_xray_lib"))

    // کتابخانه‌های شبکه و JSON
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // UI و Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.google.android.material:material:1.12.0")

    // تست‌ها
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}