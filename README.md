# Music
A music player, made simple yet customizable for Android.

## Overview
*TODO: Add pictures of Music here*

Music is designed to integrate seamlessly with other apps and system functions through intent filters,
enabling users to play audio files directly from various external sources.

It aims to provide a responsive and user-friendly experience while leveraging Android's powerful intent-filters.

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

## FAQs

### Who is spir0th?
That's my old username. I went under a different name because it sounded like a fart.
In the future, traces of the `spir0th` name will be removed in the source code.

### What motivated you to make this?
There are plenty of Android music players around the internet that considered good,
but they suck at playing audio content from intent filters, and most of the time,
they only care about their own music library. Which makes intent filters useless.

I created this project exclusively for intent filters, forget about libraries, use the file manager.
Use this in-case you want to hear an audio content without ever leaving your application.

### This project is too simple.
You already heard that right, *simple*. This project is designed to make it simple so that
whenever you play a media content quickly, you don't want to go to another application just so
you could hear it. **Keep it simple stupid!**

## License
This project is licensed under the [MIT License](LICENSE).
