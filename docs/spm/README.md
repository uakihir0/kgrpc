# kgrpc SPM

This repository is the Swift Package repository for [kgrpc].
[kgrpc] is a Kotlin Multiplatform gRPC client library.
As a result, it can be built and used on Apple devices such as iOS and macOS.
Here, we distribute the library built as an XCFramework via Swift Package.
Additionally, this repository is automatically committed to via GitHub Actions from [kgrpc].
Please direct any issues or pull requests to [kgrpc].

## Usage

This repository does not have its own versioning.
Instead, branches corresponding to the versions of [kgrpc] are provided.
To use a specific version of [kgrpc], specify the corresponding branch of this repository.
Check the [list of branches](https://github.com/uakihir0/kgrpc-spm/branches) to find the branch matching your desired version.

### How to Use

Although it is also usable from Objective-C, below is an example of how to use it in Swift.
For more detailed usage instructions, please refer to the [kgrpc] README.

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
