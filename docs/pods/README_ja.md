# kgrpc cocoapods

本レポジトリは、[kgrpc] の Cocoapods レポジトリです。[kgrpc] は Kotlin Multiplatform を用いて作成された gRPC クライアントライブラリです。
そのため、iOS や macOS 等の Apple Device でもビルドして使用することができます。ここでは、XCFramework としてビルドしたものを Cocoapods 経由で配布しています。
また、このレポジトリは [kgrpc] の GitHub Actions によって自動コミットされています。issue や pull request は [kgrpc] にお願いします。

## 使用方法

### Podfile

Podfile に以下のように記載してください。
本レポジトリにはバージョンは存在せず、[kgrpc] のバージョンと一致するブランチが存在します。
どのバージョンの [kgrpc] を使用するかは、本レポジトリのブランチを指定することで決定します。
[ブランチ一覧](https://github.com/uakihir0/kgrpc-cocoapods/branches) からバージョンに対応するブランチを確認してください。
また、Debug ビルドと Release ビルドでは異なるものを使用しています。

```ruby
target '{{PROJECT_NAME}}' do
  use_frameworks!

  # Pods for kgrpc
  pod 'kgrpc-debug',
    :configuration => ['Debug'],
    :git => 'https://github.com/uakihir0/kgrpc-cocoapods/',
    :branch => '{{BRANCH_NAME}}'
  pod 'kgrpc-release',
    :configuration => ['Release'],
    :git => 'https://github.com/uakihir0/kgrpc-cocoapods/',
    :branch => '{{BRANCH_NAME}}'
  ...
end
```

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
