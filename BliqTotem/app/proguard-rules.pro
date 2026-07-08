-keepattributes Signature
-keepattributes *Annotation*

# Retrofit / OkHttp
-dontwarn okhttp3.**
-keep class retrofit2.** { *; }
-keepclassmembernames interface * { @retrofit2.http.* <methods>; }

# Gson / data classes
-keep class br.com.bliqbrasil.totem.data.model.** { *; }
-keep class br.com.bliqbrasil.totem.data.network.** { *; }
