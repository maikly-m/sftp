# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keepparameternames
-repackageclasses 'repack.app.x'

# 保留行数
-keepattributes SourceFile,LineNumberTable

# activity都要保持其公共方法和属性
-keep class * extends androidx.appcompat.app.AppCompatActivity {
    public *;
}
-keep class * extends android.app.Activity {
    public *;
}

#Fragment不需要在AndroidManifest.xml中注册，需要额外保护下
-keep public class * extends android.app.Fragment
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends androidx.fragment.app.DialogFragment
-keep public class * extends android.app.AlertDialog

# 保持androix库不被混淆
-keep class androidx.** {
    *;
}

-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.Service
-keep public class * extends android.view.View

# 保持所有的R文件不被混淆
-keepclassmembers class **.R$* {
    public static <fields>;
}

    # 保持 Parcelable 不被混淆
    -keepclassmembers class * implements android.os.Parcelable {
      public static final android.os.Parcelable$Creator CREATOR;
    }

    # 保存序列化类和非private成员不被混淆
    -keepnames class * implements java.io.Serializable

    # 网络相关
    # okhttp--------------------------------------------------------------------------------- start
    # JSR 305 annotations are for embedding nullability information.
    -dontwarn javax.annotation.**

    # A resource is loaded with a relative path so the package of this class must be preserved.
    -keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

    # Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
    -dontwarn org.codehaus.mojo.animal_sniffer.*

    # OkHttp platform used only on JVM and when Conscrypt dependency is available.
    -dontwarn okhttp3.internal.platform.ConscryptPlatform
    # okhttp--------------------------------------------------------------------------------- end

    #retrofit -------------------------------------------------------------------------------- start
    # Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
    # EnclosingMethod is required to use InnerClasses.
    -keepattributes Signature, InnerClasses, EnclosingMethod

    # Retrofit does reflection on method and parameter annotations.
    -keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

    # Retain service method parameters when optimizing.
    -keepclassmembers,allowshrinking,allowobfuscation interface * {
        @retrofit2.http.* <methods>;
    }

    # Ignore annotation used for build tooling.
    -dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

    # Ignore JSR 305 annotations for embedding nullability information.
    -dontwarn javax.annotation.**

    # Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
    -dontwarn kotlin.Unit

    # Top-level functions that can only be used by Kotlin.
    -dontwarn retrofit2.KotlinExtensions
    -dontwarn retrofit2.KotlinExtensions$*

    # With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
    # and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
    -if interface * { @retrofit2.http.* <methods>; }
    -keep,allowobfuscation interface <1>
    #retrofit --------------------------------------------------------------------------------- end

#keep注解
#http://tools.android.com/tech-docs/support-annotations
-dontskipnonpubliclibraryclassmembers
-printconfiguration
-keep, allowobfuscation @interface androidx.annotation.Keep

-keep @androidx.annotation.Keep class *
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn com.nimbusds.jose.JWSVerifier
-dontwarn com.nimbusds.jose.crypto.RSASSAVerifier
-dontwarn com.nimbusds.jwt.JWTClaimsSet
-dontwarn com.nimbusds.jwt.SignedJWT
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE


-keep class retrofit2.** { *; }
-keepattributes *Annotation*
-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

-keep class timber.log.Timber {
    *;
}

# -------------
# Gson 混淆规则
# 保留 Gson 使用的类和字段信息
-keepattributes Signature
-keepattributes *Annotation*

# 保留带有 @SerializedName 注解的字段
-keep class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 保留 Gson 序列化/反序列化的类
-keep class com.google.gson.** { *; }
-keep class sun.misc.** { *; }

# 允许 obfuscation（混淆）字段名，如果类不用于反射
# 如果 Gson 使用反射，请保留字段
# 如果字段未使用 @SerializedName 或直接通过反射序列化/反序列化
# 需要保留字段名以确保正确解析
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 如果使用泛型序列化/反序列化，需要保留泛型信息
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Signature

# 保留 ZXing 的核心库
-keep class com.google.zxing.** { *; }

# 保留 ZXing Android 集成（如 IntentIntegrator）
-keep class com.journeyapps.barcodescanner.** { *; }

# 保留使用到的默认解码器
-keepclassmembers class com.google.zxing.common.** { *; }
-keepclassmembers class com.google.zxing.multi.** { *; }

# 保留反射使用的字段和方法（如解码器、格式化器）
-keepclassmembers enum com.google.zxing.BarcodeFormat { *; }
-keepclassmembers class com.google.zxing.DecodeHintType { *; }
-keepclassmembers class com.google.zxing.ResultMetadataType { *; }

# 保留带有 @Keep 注解的类和字段
-keep @com.google.zxing.annotations.** class * { *; }


# Glide 混淆规则

# 保留 Glide 的类和方法
-keep public class com.bumptech.glide.** { *; }

# 保留 Glide App 模块生成的类
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl { *; }

# 如果使用 Glide 的注解，需要保留注解
-keep @com.bumptech.glide.annotation.GlideModule class * { *; }
-keep @com.bumptech.glide.annotation.GlideExtension class * { *; }

# 保留使用到的资源相关的类
-keep class com.bumptech.glide.load.resource.bitmap.** { *; }
-keep class com.bumptech.glide.load.resource.gif.** { *; }

# 保留 Glide 反射调用的类
-dontwarn com.bumptech.glide.**
-dontwarn com.bumptech.glide.load.model.stream.**

# 如果使用 OkHttp 或其他库作为 Glide 的网络栈
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# 保留 Media3 ExoPlayer 的核心库
-keep class androidx.media3.** { *; }

# 保留用于反射的类和方法
-keepclassmembers class androidx.media3.** {
    *;
}

# 禁止警告
-dontwarn androidx.media3.**

# 保留注解信息
-keepattributes *Annotation*
-keepattributes Signature

# 保留 CameraX 的所有类
-keep class androidx.camera.** { *; }

# 保留 CameraX 内部使用的反射类和方法
-keepclassmembers class androidx.camera.** {
    *;
}

# 禁止混淆日志类（可选）
-assumenosideeffects class androidx.camera.core.Logger {
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# 禁止警告
-dontwarn androidx.camera.**

# 保留 Lottie 的核心类
-keep class com.airbnb.lottie.** { *; }

# 保留 Lottie 动画中的反射调用
-keep class com.airbnb.lottie.animation.content.** { *; }

# 保留 Lottie 动画资源的反射类和方法
-keep class com.airbnb.lottie.parser.** { *; }

# 保留 Lottie 动画控制相关的类
-keep class com.airbnb.lottie.model.** { *; }

# 保留与文件解析相关的类
-keep class com.airbnb.lottie.utils.** { *; }

# 保留注解信息
-keepattributes *Annotation*
-keepattributes Signature

# 禁止警告
-dontwarn com.airbnb.lottie.**

# 保留 ViewPager2 的核心类
-keep class androidx.viewpager2.** { *; }

# 保留 ViewPager2 的 Adapter 类
-keep class androidx.viewpager2.widget.** { *; }

# 保留 ViewPager2 的页面切换动画
-keep class androidx.viewpager2.widget.PageTransformer { *; }

# 保留与 RecyclerView 相关的类（因为 ViewPager2 是基于 RecyclerView 的）
-keep class androidx.recyclerview.** { *; }

# 禁止警告
-dontwarn androidx.viewpager2.**
-dontwarn androidx.recyclerview.**


# 保留 Room 的核心类
-keep class androidx.room.** { *; }

# 保留 Room 注解处理生成的代码（DAO、Entity 等）
-keep class androidx.room.**$$* { *; }

# 保留与 Room 相关的反射使用类
-keep class androidx.room.RoomDatabase { *; }

# 防止警告
-dontwarn androidx.room.**

# 保留 JSch 核心类
-keep class com.jcraft.jsch.** { *; }

# 保留 JSch 使用反射的类
-keep class com.jcraft.jsch.agentproxy.** { *; }

# 保留 JSch 与密钥和认证相关的类
-keep class com.jcraft.jsch.JSch$** { *; }

# 保留 JSch 的代理和认证相关的类
-keep class com.jcraft.jsch.auth.** { *; }

# 防止警告
-dontwarn com.jcraft.jsch.**

# 保留 sshd-sftp 核心类
-keep class org.apache.sshd.** { *; }

# 保留 sshd-sftp 使用反射的类
-keep class org.apache.sshd.common.** { *; }

# 保留与 SFTP 和 SSH 相关的类
-keep class org.apache.sshd.sftp.** { *; }

# 保留 sshd-sftp 的相关认证类
-keep class org.apache.sshd.auth.** { *; }

# 保留 sshd-sftp 使用的系统类
-keep class org.apache.sshd.server.** { *; }

# 防止警告
-dontwarn org.apache.sshd.**
-dontwarn org.apache.sshd.server.**

# 保留 Apache SSHD 核心类
-keep class org.apache.sshd.** { *; }

# 保留 SSH 服务器类
-keep class org.apache.sshd.server.** { *; }

# 保留 SSH 客户端类
-keep class org.apache.sshd.client.** { *; }

# 保留 SSHD 相关的认证类
-keep class org.apache.sshd.auth.** { *; }

# 保留 SSHD 的公共类
-keep class org.apache.sshd.common.** { *; }

# 保留所有的密钥、加密相关类
-keep class org.apache.sshd.common.kex.** { *; }

# 保留 sshd-core 库可能使用反射的类
-keep class org.apache.sshd.server.auth.password.** { *; }
-keep class org.apache.sshd.server.auth.gss.** { *; }

# 防止警告
-dontwarn org.apache.sshd.**
-dontwarn org.apache.sshd.server.**
-dontwarn org.apache.sshd.client.**
-dontwarn org.apache.sshd.auth.**

# 保留 mina-core 核心类
-keep class org.apache.mina.** { *; }

# 保留与 mina 服务端和客户端相关的类
-keep class org.apache.mina.transport.** { *; }

# 保留 mina 编解码相关的类
-keep class org.apache.mina.core.filterchain.** { *; }

# 保留 mina 使用反射的类
-keep class org.apache.mina.common.** { *; }

# 保留 mina 的传输和连接相关的类
-keep class org.apache.mina.transport.socket.** { *; }

# 保留 mina 的处理器相关类
-keep class org.apache.mina.handler.** { *; }

# 防止警告
-dontwarn org.apache.mina.**
-dontwarn org.apache.mina.transport.**
-dontwarn org.apache.mina.core.**

# 保留 SSHJ 核心类
-keep class com.jcraft.jsch.** { *; }

# 保留 SSHJ 的身份验证、连接相关类
-keep class com.jcraft.jsch.auth.** { *; }

# 保留 SSHJ 的加密相关类
-keep class com.jcraft.jsch.jce.** { *; }

# 保留 SSHJ 相关的 I/O 和管道处理类
-keep class com.jcraft.jsch.io.** { *; }

# 保留 SSHJ 的 SFTP 类
-keep class com.jcraft.jsch.sftp.** { *; }

# 保留 SSHJ 使用反射的类
-keep class com.jcraft.jsch.Channel.** { *; }

# 防止警告
-dontwarn com.jcraft.jsch.**
-dontwarn com.jcraft.jsch.auth.**
-dontwarn com.jcraft.jsch.jce.**
-dontwarn com.jcraft.jsch.io.**
-dontwarn com.jcraft.jsch.sftp.**

# 保留 JSoup 核心类
-keep class org.jsoup.** { *; }

# 保留 JSoup 的解析器相关类
-keep class org.jsoup.parser.** { *; }

# 保留 JSoup 的选择器相关类
-keep class org.jsoup.select.** { *; }

# 保留 JSoup 的数据存储和输出相关类
-keep class org.jsoup.nodes.** { *; }

# 防止警告
-dontwarn org.jsoup.**


# 保留 EventBus 的核心类
-keep class org.greenrobot.eventbus.** { *; }

# 保留 EventBus 中的反射调用（用于事件订阅）
-keep class org.greenrobot.eventbus.util.** { *; }

# 保留 EventBus 中与注解相关的类
-keep class org.greenrobot.eventbus.Subscribe { *; }

# 防止警告
-dontwarn org.greenrobot.eventbus.**

#---------
# Firebase 基本混淆规则
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firebase Cloud Messaging (FCM)
-keep class com.google.firebase.messaging.** { *; }
-dontwarn com.google.firebase.messaging.**

# Firebase Analytics
-keep class com.google.firebase.analytics.** { *; }
-dontwarn com.google.firebase.analytics.**

# Firebase Firestore 和 Firebase Realtime Database
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.database.** { *; }
-dontwarn com.google.firebase.firestore.**
-dontwarn com.google.firebase.database.**

# Firebase Authentication
-keep class com.google.firebase.auth.** { *; }
-dontwarn com.google.firebase.auth.**

# Firebase Storage
-keep class com.google.firebase.storage.** { *; }
-dontwarn com.google.firebase.storage.**

# Firebase Dynamic Links
-keep class com.google.firebase.dynamiclinks.** { *; }
-dontwarn com.google.firebase.dynamiclinks.**

# Firebase Crashlytics
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Firebase Performance Monitoring
-keep class com.google.firebase.perf.** { *; }
-dontwarn com.google.firebase.perf.**

#---------

-keep class com.emoji.ftp.bean**{
    *;
}
-keep class com.emoji.ftp.room**{
    *;
}

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn autovalue.shaded.com.google$.errorprone.annotations.$CanIgnoreReturnValue
-dontwarn autovalue.shaded.com.google$.errorprone.annotations.$CompatibleWith
-dontwarn autovalue.shaded.com.google$.errorprone.annotations.$ForOverride
-dontwarn autovalue.shaded.com.google$.errorprone.annotations.concurrent.$GuardedBy
-dontwarn autovalue.shaded.com.google$.errorprone.annotations.concurrent.$LazyInit
-dontwarn javax.lang.model.SourceVersion
-dontwarn javax.lang.model.element.AnnotationMirror
-dontwarn javax.lang.model.element.AnnotationValue
-dontwarn javax.lang.model.element.AnnotationValueVisitor
-dontwarn javax.lang.model.element.Element
-dontwarn javax.lang.model.element.ElementKind
-dontwarn javax.lang.model.element.ElementVisitor
-dontwarn javax.lang.model.element.ExecutableElement
-dontwarn javax.lang.model.element.Name
-dontwarn javax.lang.model.element.TypeElement
-dontwarn javax.lang.model.type.DeclaredType
-dontwarn javax.lang.model.type.ExecutableType
-dontwarn javax.lang.model.type.TypeKind
-dontwarn javax.lang.model.type.TypeMirror
-dontwarn javax.lang.model.type.TypeVisitor
-dontwarn javax.lang.model.util.AbstractElementVisitor6
-dontwarn javax.lang.model.util.ElementFilter
-dontwarn javax.lang.model.util.SimpleAnnotationValueVisitor6
-dontwarn javax.lang.model.util.SimpleAnnotationValueVisitor8
-dontwarn javax.lang.model.util.SimpleElementVisitor6
-dontwarn javax.lang.model.util.SimpleElementVisitor8
-dontwarn javax.lang.model.util.SimpleTypeVisitor6
-dontwarn javax.lang.model.util.SimpleTypeVisitor8
-dontwarn javax.lang.model.util.Types
-dontwarn javax.tools.SimpleJavaFileObject
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder
-dontwarn org.slf4j.impl.StaticMarkerBinder

