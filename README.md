# Music
A music player, made simple yet customizable for Android.

## Overview
<img src="docs/images/preview.png" alt="Preview" width="30%">

Music is designed to integrate seamlessly with other apps through intent filters,
enabling users to play audio files directly from various external sources, without leaving.

It aims to provide a simple & user-friendly interface with the design following the
Material 3 guidelines, and media playback powered by ExoPlayer through Media3 API.

## Download
Get the [latest build](https://github.com/feivegian/music/releases/latest) on the Releases page.

## Building
It's the same way as you build a project using Android Studio, no steps needed.

Although if you prefer the command-line for building, you can use Gradle tasks instead:
```shell
# DEBUG
$ ./gradlew assembleDebug
$ ./gradlew installDebug # install to connected device
# RELEASE
$ ./gradlew assembleRelease
$ zipalign -v -p 4 app-release-unsigned.apk app-release-unsigned-aligned.apk
$ apksigner sign --ks my-release-key.jks --out app-release.apk app-release-unsigned-aligned.apk
```

> [!IMPORTANT]
> *When building a release build, you must install `zipalign`, `apksigner`, and have your own keystore.*

## License
This project is licensed under the [MIT License](LICENSE).
