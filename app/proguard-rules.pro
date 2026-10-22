# sherpa-onnx JNI reads Kotlin fields and calls methods by name. Keep the whole package as is.
-keep class com.k2fsa.sherpa.onnx.** { *; }
-keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }

# Optional codecs of commons-compress and annotations of Tink are not shipped.
-dontwarn org.apache.commons.compress.**
-dontwarn com.github.luben.zstd.**
-dontwarn org.brotli.dec.**
-dontwarn org.tukaani.xz.**
-dontwarn org.objectweb.asm.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# Readable crash traces in Play Console.
-keepattributes SourceFile,LineNumberTable