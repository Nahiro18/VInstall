-keep class com.vinstall.alwiz.model.** { *; }
-keep class com.vinstall.alwiz.parser.** { *; }
-keep class com.vinstall.alwiz.apkv.** { *; }
-keep class com.vinstall.alwiz.receiver.** { *; }
-keep enum com.vinstall.alwiz.settings.InstallMode { *; }
-keep enum com.vinstall.alwiz.settings.DialogStyle { *; }
-keep class com.vinstall.alwiz.ui.ConfirmationBottomSheet
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class * implements com.google.gson.JsonDeserializer { *; }
-keep class rikka.shizuku.** { *; }