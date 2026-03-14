# kgrpc SPM

本レポジトリは、[kgrpc] の SwiftPackage レポジトリです。[kgrpc] は Kotlin Multiplatform を用いて作成された gRPC クライアントライブラリです。
そのため、iOS や macOS 等の Apple Device でもビルドして使用することができます。ここでは、XCFramework としてビルドしたものを SwiftPackage 経由で配布しています。
また、このレポジトリは [kgrpc] の GitHub Actions によって自動コミットされています。issue や pull request は [kgrpc] にお願いします。

## 使用方法

本レポジトリにはバージョンは存在せず、[kgrpc] のバージョンと一致するブランチが存在します。
どのバージョンの [kgrpc] を使用するかは、本レポジトリのブランチを指定することで決定します。
[ブランチ一覧](https://github.com/uakihir0/kgrpc-spm/branches) からバージョンに対応するブランチを確認してください。

### リクエスト方法

Objective-C でも使用可能ですが、以下に Swift での使用方法を記載します。
詳しい使い方については、[kgrpc] の README も合わせて確認してください。

```swift
let channel = CoreChannel.Builder()
  .forAddress(host: "localhost", port: 50051)
  .usePlaintext()
  .build()
```

## ライセンス

MIT License

## 作者

[Akihiro Urushihara](https://github.com/uakihir0)

[kgrpc]: https://github.com/uakihir0/kgrpc
