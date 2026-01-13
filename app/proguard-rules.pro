-keep class dev.fzer0x.fucksolidexplorer.XposedInit { *; }
-keepclassmembers class dev.fzer0x.fucksolidexplorer.MainActivity {
    private boolean isModuleActive();
}
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**