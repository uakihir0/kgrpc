# エージェント資料

## 概要

このレポジトリは Kotlin Multiplatform gRPC クライアントライブラリです。JVM、JavaScript、iOS、macOS、Linux、Windows の各プラットフォームで共通の API を提供します。

## ディレクトリ構成

- **`core/`**: gRPC クライアントのコアライブラリ (Channel, Stub, Message, Interceptor, RPC 実装)
- **`all/`**: Apple 向け集約モジュール (XCFramework, CocoaPods, SwiftPM)
- **`native/`**: Rust/Tonic によるネイティブ gRPC ライブラリ (Kotlin/Native cinterop 用スタティックライブラリ)
- **`plugins/`**: パブリッシュ用 Gradle コンベンションプラグイン
- **`tool/`**: ビルドヘルパースクリプト (CocoaPods 後処理)

## プラットフォーム実装

| プラットフォーム | ソースセット | gRPC 実装 | ストリーミング対応 |
|----------|-----------|-------------------|------------------|
| JVM | jvmMain | grpc-java + grpc-kotlin-stub + grpc-okhttp | 全タイプ |
| JS (IR) | jsMain | gRPC-Web (Ktor HttpClient 経由) | Unary + Server-streaming |
| iOS / macOS | nativeMain | Rust/Tonic FFI (cinterop) | 全タイプ |
| Linux x64 | nativeMain | Rust/Tonic FFI (cinterop) | 全タイプ |
| Windows mingwX64 | nativeMain | Rust/Tonic FFI (cinterop) | 全タイプ |

## 主要な概念

### Channel

`Channel` は gRPC 接続を作成するためのメインエントリーポイントです。`Channel.Builder` で設定します：

```kotlin
val channel = Channel.Builder
    .forAddress("localhost", 50051)
    .usePlaintext()
    .build()
```

### Message と MessageCompanion

すべての gRPC メッセージは `Message` (シリアライズ) を実装し、`MessageCompanion<T>` (デシリアライズ) を実装するコンパニオンオブジェクトを持ちます。これらは通常 protobuf コード生成プラグインによって生成されます。

### Stub

生成されたサービススタブは `Stub<S>` を継承し、RPC 関数 (`unaryRpc`, `serverStreamingRpc` など) を使用して呼び出しを行います。

### CallInterceptor

インターセプターは RPC ライフサイクルの各ポイントでメタデータ、メッセージ、ステータスを変更できます。

## テスト方法

JVM テストの実行 (外部依存なし)：

```shell
./gradlew core:jvmTest
```

ビルド確認 (ネットワークアクセス不要)：

```shell
./gradlew jvmJar
```

## ネイティブライブラリのビルド

ネイティブプラットフォームのビルドには Rust ツールチェーンが必要です：

```shell
# 現在の macOS プラットフォーム向けにビルド
cd native && cargo build --release

# 全 Apple ターゲット向けにビルド
cd native && bash compile.sh

# Linux/Windows 向けクロスコンパイル (`cross` ツールが必要)
cd native && cross build --target x86_64-unknown-linux-gnu --release
cd native && cross build --target x86_64-pc-windows-gnu --release
```

## 実装ガイドライン

### パッケージ構造

すべてのコードは `work.socialhub.kgrpc` 配下にあります：
- `work.socialhub.kgrpc` - コア型 (Channel, Code, Status など)
- `work.socialhub.kgrpc.config` - 設定型 (KeepAliveConfig)
- `work.socialhub.kgrpc.message` - Message インターフェース
- `work.socialhub.kgrpc.metadata` - Metadata 型 (Key, Entry, Metadata)
- `work.socialhub.kgrpc.rpc` - RPC 実装 (プラットフォームごと)
- `work.socialhub.kgrpc.stub` - Stub 基底クラス
- `work.socialhub.kgrpc.internal` - JVM 内部ヘルパー
- `work.socialhub.kgrpc.util` - ユーティリティ (PEM パース)

### expect/actual パターン

`Channel` はプラットフォーム固有の実装に expect/actual パターンを使用しています：
- `commonMain/Channel.kt` - expect 宣言
- `jvmMain/Channel.jvm.kt` - grpc-java ManagedChannel ラッパー
- `jsMain/Channel.js.kt` - Ktor HttpClient (gRPC-Web)
- `nativeMain/Channel.native.kt` - Rust/Tonic FFI

### テスト

- 共通テストは `core/src/commonTest/kotlin/` に配置
- JVM 固有テストは `core/src/jvmTest/kotlin/` に配置
- テストクラスは kotlin.test アノテーションを使用
