# Recommended package so to be consistent with Ezio SDK
# !!! it is extremely important to 'flatten' or to 'repackage' into package 'util',
# so to hide important packages that cannot be obfuscated further !!!
-flattenpackagehierarchy util

## for maintenance purposes, keep this files confidential
#-dump class_files.txt
#-printseeds seeds.txt
#-printusage unused.txt
#-printmapping mapping.txt
-printconfiguration build/obfuscation-rules.pro


-keep class * extends android.** { *; }
-keep class * extends com.android.** { *; }
-keep class * extends com.google.** { *; }
-dontwarn org.apache.http.**
-dontwarn java.awt.**

# There is an issue on R8 on GSON. Please refer to: https://r8.googlesource.com/r8/+/refs/heads/master/compatibility-faq.md
# option 1
-keepclassmembers,allowobfuscation class util.a.y.** {
  <fields>;
}

# option 2
#-dontshrink

