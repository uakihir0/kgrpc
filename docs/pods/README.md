> [日本語](./README_ja.md)

# kgrpc cocoapods

This repository is the Cocoapods repository for [kgrpc].
[kgrpc] is a Kotlin Multiplatform gRPC client library.
Therefore, it can be built and used on Apple devices such as iOS and macOS. Here,
we distribute the library built as an XCFramework via Cocoapods.
Also, this repository is automatically committed by [kgrpc]'s GitHub Actions,
so please send issues and pull requests to [kgrpc].

## Usage

### Podfile

Please add the following lines to your Podfile.
There is no specific version for this repository,
and it corresponds to the branch that matches the version of [kgrpc].
The version to use is determined by specifying the branch of this repository.
Check the [Branch List](https://github.com/uakihir0/kgrpc-cocoapods/branches)
for the branch corresponding to your version.
Additionally, different dependencies are used for Debug and Release builds.

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

### Request Example

While it can be used in Objective-C as well,
here is an example of usage in Swift.
For more detailed instructions, please refer to the README of [kgrpc].

```swift
let channel = CoreChannel.Builder()
  .forAddress(host: "localhost", port: 50051)
  .usePlaintext()
  .build()
```

## License

MIT License

## Author

[Akihiro Urushihara](https://github.com/uakihir0)

[kgrpc]: https://github.com/uakihir0/kgrpc
