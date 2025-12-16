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


-keep, includedescriptorclasses class android.** { *; }
-keep, includedescriptorclasses class androidx.** { *; }
-keep, includedescriptorclasses class com.android.** { *; }
-keep, includedescriptorclasses class com.google.** { *; }
-keep, includedescriptorclasses class me.dm7.** { *; }
-keep, includedescriptorclasses class org.chromium.** { *; }
-dontwarn org.apache.http.**
-dontwarn java.awt.**
-dontwarn androidx.**
-dontwarn com.gemalto.**
-dontwarn com.android.**
-dontnote com.google.**
-dontwarn com.google.android.gms.**

# There is an issue on R8 on GSON. Please refer to: https://r8.googlesource.com/r8/+/refs/heads/master/compatibility-faq.md
# option 2
-dontshrink

