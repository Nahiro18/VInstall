-keep class com.vinstall.alwiz.model.** { *; }
-keep class com.vinstall.alwiz.parser.** { *; }
-keep class com.vinstall.alwiz.apkv.** { *; }
-keep class com.vinstall.alwiz.receiver.** { *; }
-keep class com.vinstall.alwiz.installer.** { *; }
-keep class com.vinstall.alwiz.history.** { *; }
-keep class com.vinstall.alwiz.backup.** { *; }
-keep class com.vinstall.alwiz.settings.** { *; }
-keep class com.vinstall.alwiz.util.** { *; }
-keep class com.vinstall.alwiz.ui.** { *; }
-keep class com.vinstall.alwiz.shizuku.** { *; }
-keep enum com.vinstall.alwiz.settings.InstallMode { *; }
-keep enum com.vinstall.alwiz.settings.DialogStyle { *; }
-keep class com.vinstall.alwiz.ui.ConfirmationBottomSheet { *; }
-keep class com.vinstall.alwiz.BuildConfig { *; }

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributesLineNumberNumbers
-keepattributes SourceFile,LineNumberTable

-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class * implements com.google.gson.JsonDeserializer { *; }
-keep class * implements com.google.gson.TypeAdapter { *; }

-keep class rikka.shizuku.** { *; }

-keepclassmembers class androidx.lifecycle.* {
    <fields>;
}

-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <fields>;
}

-assumenosideclassshrinking {
    * extends kotlin.Any
}