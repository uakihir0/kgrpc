> [日本語](./docs/README_ja.md)

# kgrpc

<!-- ![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.repsy.io%2Fmvn%2Fuakihir0%2Fpublic%2Fwork%2Fsocialhub%2Fkgrpc%2Fcore%2Fmaven-metadata.xml) -->

![Maven Central Version](https://img.shields.io/maven-central/v/work.socialhub.kgrpc/all)

![badge][badge-jvm]
![badge][badge-js]
![badge][badge-ios]
![badge][badge-mac]
![badge][badge-linux]
![badge][badge-windows]

**This library is a gRPC client library compatible
with [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html).**
It provides a common gRPC API across JVM, JavaScript, iOS, macOS, Linux, and Windows platforms.
On JVM it uses grpc-java, on JavaScript it uses gRPC-Web over Ktor HttpClient,
and on native platforms (iOS, macOS, Linux, Windows) it uses Rust/Tonic via FFI.

## Platform Support

| Platform | Implementation | Streaming Support |
|----------|---------------|-------------------|
| JVM | grpc-java + grpc-kotlin-stub + grpc-okhttp | All types |
| JS (IR) | gRPC-Web over Ktor HttpClient | Unary + Server-streaming |
| iOS / macOS | Rust/Tonic FFI via cinterop | All types |
| Linux x64 | Rust/Tonic FFI via cinterop | All types |
| Windows mingwX64 | Rust/Tonic FFI via cinterop | All types |

## Usage

Below is how to use it with Kotlin on the supported platforms using Gradle.
**If you are using it on an Apple platform, please refer to [kgrpc-spm](https://github.com/uakihir0/kgrpc-spm) or [kgrpc-cocoapods](https://github.com/uakihir0/kgrpc-cocoapods).**
Additionally, please check the test code as well.

### Stable (Maven Central)

```kotlin:build.gradle.kts
repositories {
    mavenCentral()
}

dependencies {
+   implementation("work.socialhub.kgrpc:core:0.0.1")
}
```

### Snapshot

```kotlin:build.gradle.kts
repositories {
+   maven { url = uri("https://repo.repsy.io/mvn/uakihir0/public") }
}

dependencies {
+   implementation("work.socialhub.kgrpc:core:0.0.1-SNAPSHOT")
}
```

### Using as part of a regular Java project

All of the above can be added to and used in regular Java projects, too. All you have to do is to use the suffix `-jvm` when listing the dependency.

Here is a sample Maven configuration:

```xml
<dependency>
    <groupId>work.socialhub.kgrpc</groupId>
    <artifactId>core-jvm</artifactId>
    <version>[VERSION]</version>
</dependency>
```

### Creating a Channel

```kotlin
val channel = Channel.Builder
    .forAddress("localhost", 50051)
    .usePlaintext()
    .build()
```

### Creating a Channel with TLS

```kotlin
val channel = Channel.Builder
    .forAddress("example.com", 443)
    .useTransportSecurity()
    .build()
```

### Making a Unary RPC Call

```kotlin
val stub = MyServiceStub(channel)
val response = stub.myMethod(request)
```

### Interceptors

```kotlin
val channel = Channel.Builder
    .forAddress("localhost", 50051)
    .usePlaintext()
    .intercept(object : CallInterceptor {
        override fun onSendHeaders(headers: Metadata): Metadata {
            headers.put("authorization", "Bearer $token")
            return headers
        }
    })
    .build()
```

## Key Concepts

- **Channel** - gRPC connection (configured via `Channel.Builder`)
- **Message / MessageCompanion** - Protobuf message serialization/deserialization
- **Stub** - Generated service stubs for making RPC calls
- **CallInterceptor** - Intercept and modify RPC metadata, messages, and status

## License

MIT License

## Author

[Akihiro Urushihara](https://github.com/uakihir0)

[badge-android]: http://img.shields.io/badge/-android-6EDB8D.svg
[badge-android-native]: http://img.shields.io/badge/support-[AndroidNative]-6EDB8D.svg
[badge-wearos]: http://img.shields.io/badge/-wearos-8ECDA0.svg
[badge-jvm]: http://img.shields.io/badge/-jvm-DB413D.svg
[badge-js]: http://img.shields.io/badge/-js-F8DB5D.svg
[badge-js-ir]: https://img.shields.io/badge/support-[IR]-AAC4E0.svg
[badge-nodejs]: https://img.shields.io/badge/-nodejs-68a063.svg
[badge-linux]: http://img.shields.io/badge/-linux-2D3F6C.svg
[badge-windows]: http://img.shields.io/badge/-windows-4D76CD.svg
[badge-wasm]: https://img.shields.io/badge/-wasm-624FE8.svg
[badge-apple-silicon]: http://img.shields.io/badge/support-[AppleSilicon]-43BBFF.svg
[badge-ios]: http://img.shields.io/badge/-ios-CDCDCD.svg
[badge-mac]: http://img.shields.io/badge/-macos-111111.svg
[badge-watchos]: http://img.shields.io/badge/-watchos-C0C0C0.svg
[badge-tvos]: http://img.shields.io/badge/-tvos-808080.svg
