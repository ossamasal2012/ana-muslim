import java.security.KeyStore
import java.security.MessageDigest
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ============================================================================
// قراءة معلومات توقيع النسخة النهائية (Release) من متغيرات البيئة.
// نظام GitHub Actions يمرر هذه القيم من GitHub Secrets وقت البناء الآلي.
// إذا لم تكن موجودة (مثلاً مطوّر يبني نسخة تجريبية محلياً على جهازه)
// سيتم التوقيع تلقائياً بمفتاح Debug الافتراضي حتى لا يفشل البناء المحلي أبداً.
// ============================================================================
val releaseStoreFile: String? = System.getenv("RELEASE_STORE_FILE")
val releaseStorePassword: String? = System.getenv("RELEASE_STORE_PASSWORD")
val releaseKeyAlias: String? = System.getenv("RELEASE_KEY_ALIAS")
val releaseKeyPassword: String? = System.getenv("RELEASE_KEY_PASSWORD")
val hasReleaseSigning = !releaseStoreFile.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

// ============================================================================
// بصمة شهادة التوقيع (SHA-256) تُحسب تلقائياً هنا من نفس ملف مفتاح الإصدار
// المستخدَم أعلاه للتوقيع — دون أي إدخال يدوي، فتبقى صحيحة دائماً بالضرورة لأنها
// مُشتقّة من نفس السرّ الذي سيُوقَّع به APK هذا بالضبط. تُستخدم وقت التشغيل
// (راجع IntegrityChecker.kt) للتنبيه فقط إن أُعيد توقيع نسخة مُعدَّلة من التطبيق
// بمفتاح مختلف عن مفتاحكم الأصلي ثم أُعيد توزيعها. لا تؤثر على أي وظيفة أخرى:
// أي خطأ أثناء الحساب يُهمَل بأمان (سلسلة فارغة = الفحص معطّل تلقائياً وقت
// التشغيل) فلا يتوقف بناء APK بسبب هذا أبداً.
// ============================================================================
val releaseSigningCertSha256: String = if (hasReleaseSigning) {
    runCatching {
        var certBytes: ByteArray? = null
        for (keystoreType in listOf("PKCS12", "JKS")) {
            if (certBytes != null) break
            runCatching {
                val keyStore = KeyStore.getInstance(keystoreType)
                file(releaseStoreFile!!).inputStream().use { stream ->
                    keyStore.load(stream, releaseStorePassword!!.toCharArray())
                }
                certBytes = keyStore.getCertificate(releaseKeyAlias!!)?.encoded
            }
        }
        certBytes?.let { bytes ->
            MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { b: Byte -> "%02X".format(b.toInt() and 0xFF) }
        } ?: ""
    }.getOrDefault("")
} else {
    ""
}

// قراءة رقم ونص الإصدار من ملف واحد بسيط (version.properties) بدل دفنهما داخل
// سكربت البناء — هذا يجعل رفع رقم الإصدار عند كل تحديث عملية تحرير سطرين فقط
// وهي نفس الطريقة المتبعة سابقاً في مشروعكم الآخر، لتبقى العملية مألوفة لكم.
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        versionPropsFile.inputStream().use { load(it) }
    } else {
        setProperty("VERSION_CODE", "1")
        setProperty("VERSION_NAME", "1.0")
    }
}
val appVersionCode = versionProps.getProperty("VERSION_CODE").trim().toInt()
val appVersionName = versionProps.getProperty("VERSION_NAME").trim()

android {
    namespace = "com.anamuslim.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.anamuslim.app"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        // رابط مستودع GitHub الذي تُنشر منه تحديثات التطبيق (ملف version.json + APK).
        // ⚠️ عدّل هذا السطر ليطابق اسم المستخدم/المستودع الفعلي لديك على GitHub قبل أول بناء.
        buildConfigField(
            "String",
            "UPDATE_VERSION_JSON_URL",
            "\"https://github.com/ossamasal2012/ana-muslim/releases/latest/download/version.json\""
        )
        buildConfigField(
            "String",
            "GITHUB_REPO_RELEASES_URL",
            "\"https://github.com/ossamasal2012/ana-muslim/releases/latest\""
        )
        // فارغة تلقائياً لأي بناء محلي/تجريبي بلا أسرار توقيع حقيقية (الفحص وقت
        // التشغيل يتجاهل نفسه تماماً في هذه الحالة). تُملأ فعلياً فقط في بناء
        // GitHub Actions الموقَّع رسمياً.
        buildConfigField(
            "String",
            "EXPECTED_SIGNING_CERT_SHA256",
            "\"$releaseSigningCertSha256\""
        )
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                // شبكة أمان للبناء المحلي فقط دون أسرار GitHub — يوقّع بمفتاح Debug
                // حتى لا يتوقف البناء، لكن هذه النسخة لن تُقبل كتحديث فوق نسخة
                // موقّعة رسمياً على أجهزة المستخدمين.
                signingConfigs.getByName("debug")
            }
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // فحوص Lint لا تُوقف بناء النسخة النهائية على GitHub Actions (لا تؤثر على عمل التطبيق نفسه)
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

// إعداد المترجم بالصيغة الرسمية الحالية (compilerOptions). الصيغة القديمة android.kotlinOptions
// أُزيلت رسمياً بدءاً من Kotlin 2.2.0 وتسبب فشل البناء، لذلك استُبدلت هنا (تحقق من توثيق Kotlin الرسمي).
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // ---------- أساسيات أندرويد و Kotlin ----------
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // ---------- Jetpack Compose (عبر BOM لضمان توافق كل النسخ الفرعية) ----------
    // BOM 2026.06.00 = Compose 1.11.x (compileSdk 36) وهو آخر خط متوافق مع AGP 8.x.
    // الإصدارات الأحدث (2026.08.00 فما فوق = Compose 1.12) تتطلب compileSdk 37 و AGP 9.1.1+،
    // فترقية BOM وحدها ستكسر البناء. هذا اختيار توافق واستقرار وليس أحدث إصدار متاح.
    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ---------- تخزين محلي دائم (إعدادات، آخر صفحة، السبحات) ----------
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ---------- جدولة عمل دورية (شبكة أمان لمواعيد الأذان + فحص التحديث) ----------
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // ---------- شبكة للتحقق من التحديثات ----------
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
