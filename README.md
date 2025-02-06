# Musiqview
Quickly preview audio files without leaving, on Android.

[Download Musiqview](https://github.com/borfei/musicview/releases/latest)

## Table of Contents
- [Overview](#overview)
- [Building](#building)
- [License](#license)

## Overview
**Musiqview** lets you preview local audio files from your Android device without annoying stuff.
It is completely free, no ads, no BS, 100% open-source!

It follows the [Material You](https://m3.material.io/) guidelines, making the user interface clean
and simple. It also utilizes [Media3](https://developer.android.com/media/media3) for media playback.

## Building
It's the same way as you build a project using Android Studio, no steps needed.

Although if you prefer the hacker way, you can use Gradle tasks instead:
```shell
# For debug variants
$ ./gradlew assembleDebug
$ ./gradlew installDebug # install to connected device
# For release variants
$ ./gradlew assembleRelease
$ zipalign -v -p 4 app-release-unsigned.apk app-release-unsigned-aligned.apk
$ apksigner sign --ks my-release-key.jks --out app-release.apk app-release-unsigned-aligned.apk
```

> [!IMPORTANT]
> When building a release build using **the hacker way**, you must provide your own keystore.

## License
Musiqview is licensed under the **MIT License**.
See the [LICENSE](LICENSE) file for more information.
