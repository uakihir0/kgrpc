# kgrpc

<!-- ![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.repsy.io%2Fmvn%2Fuakihir0%2Fpublic%2Fwork%2Fsocialhub%2Fkgrpc%2Fcore%2Fmaven-metadata.xml) -->

![Maven Central Version](https://img.shields.io/maven-central/v/work.socialhub.kgrpc/all)

![badge][badge-jvm]
![badge][badge-js]
![badge][badge-ios]
![badge][badge-mac]
![badge][badge-linux]
![badge][badge-windows]

**このライブラリは [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) に対応した gRPC クライアントライブラリです。**
JVM、JavaScript、iOS、macOS、Linux、Windows の各プラットフォームで共通の gRPC API を提供します。
JVM では grpc-java、JavaScript では gRPC-Web (Ktor HttpClient 経由)、
ネイティブプラットフォーム (iOS、macOS、Linux、Windows) では Rust/Tonic の FFI を使用しています。

## プラットフォームサポート

| プラットフォーム | 実装 | ストリーミング対応 |
|----------|---------------|-------------------|
| JVM | grpc-java + grpc-kotlin-stub + grpc-okhttp | 全タイプ |
| JS (IR) | gRPC-Web (Ktor HttpClient 経由) | Unary + Server-streaming |
| iOS / macOS | Rust/Tonic FFI (cinterop) | 全タイプ |
| Linux x64 | Rust/Tonic FFI (cinterop) | 全タイプ |
| Windows mingwX64 | Rust/Tonic FFI (cinterop) | 全タイプ |

## 使い方

以下は対応するプラットフォームにおいて Gradle を用いて Kotlin で使用する際の使い方になります。
**Apple プラットフォームで使用する場合は、[kgrpc-spm](https://github.com/uakihir0/kgrpc-spm) または [kgrpc-cocoapods](https://github.com/uakihir0/kgrpc-cocoapods) を参照してください。**
また、テストコードも合わせて確認してください。

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

### 通常の Java プロジェクトで使用する場合

上記のすべてのパッケージは通常の Java プロジェクトにも追加して使用できます。依存関係を記載する際にサフィックス `-jvm` を使用するだけです。

Maven の設定例：

```xml
<dependency>
    <groupId>work.socialhub.kgrpc</groupId>
    <artifactId>core-jvm</artifactId>
    <version>[VERSION]</version>
</dependency>
```

### Channel の作成

```kotlin
val channel = Channel.Builder
    .forAddress("localhost", 50051)
    .usePlaintext()
    .build()
```

### TLS を使用した Channel の作成

```kotlin
val channel = Channel.Builder
    .forAddress("example.com", 443)
    .useTransportSecurity()
    .build()
```

### Unary RPC の実行

```kotlin
val stub = MyServiceStub(channel)
val response = stub.myMethod(request)
```

### インターセプター

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

## 主要な概念

- **Channel** - gRPC 接続 (`Channel.Builder` で設定)
- **Message / MessageCompanion** - Protobuf メッセージのシリアライズ/デシリアライズ
- **Stub** - RPC 呼び出しを行う生成されたサービススタブ
- **CallInterceptor** - RPC のメタデータ、メッセージ、ステータスをインターセプトして変更

## ライセンス

MIT License

## 作者

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
