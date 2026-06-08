# Add project specific ProGuard rules here.

# Dialer InCallService
-keep class com.example.helper_application.dialer.feature.incall.MyInCallService { *; }

# Shizuku UserService (must keep class name for bindUserService)
-keep class com.example.helper_application.shizuku.ShizukuRecordingUserService { *; }
-keepclassmembers class com.example.helper_application.shizuku.ShizukuRecordingUserService {
    public <init>();
    public <init>(android.content.Context);
}
-keep class com.example.helper_application.shizuku.IShizukuRecordingService$Stub { *; }
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