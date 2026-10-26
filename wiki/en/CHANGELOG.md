**[English](CHANGELOG.md)** | [简体中文](../zh-cn/CHANGELOG.md)

# Change log
## 0.1.2027
- Bugfix: `classObfuscation` no longer aborts with `Class <name> has already been interned` when classes are added or removed between builds. Names already handed out by the input `class-mapping.txt` are now reserved before any new name is generated; previously they were reserved only when the generator reached the mapped class, so a newly added class could take a name that another mapped class still owned.
- Bugfix: a `class-mapping.txt` that maps two classes to the same name is detected, reported and repaired instead of failing the bundle.
- Class obfuscation now fails with an explicit message when two classes would end up with the same name.

## 0.1.2026
- Added opt-in assets obfuscation (`enableObfuscateAssets` / `--assets-obfuscate`): file names under `assets/` are renamed, and whitelist rules now match asset paths too.
- Added class-name obfuscation for DEX files and binary XML resources (`classObfuscation { ... }` / `<class-obfuscation>`), with a reusable `class-mapping.txt`.
- Added universal APK output: the Gradle plugin builds `universal.apk` from the obfuscated bundle (`buildUniversalApk`, on by default) and hooks it into `assemble<Variant>`.
- Updated the toolchain to JDK 21, Gradle 9.5 and AGP 9.3.1, with bundletool 1.18.3.

## 0.1.6（2020/4/21）
- Compatible wit `AGP-3.5.0`
- Bugfix: `Fix get AGP version failed issue`

## 0.1.5（2020/4/5）
- Compatible with `AGP-4.0.0-alpha09`
- Add `enableObfuscate` for plugin extension.

## 0.1.3（2020/1/8）
- Compatible with `AGP-3.5.2`

## 0.1.2（2020/1/7）
- Compatible with `AGP-4.0.0-alpha07`
-  Fix issue [#13](https://github.com/bytedance/AabResGuard/issues/13)

## 0.1.1（2019/11/26）
- Compatible with `AGP-3.4.1.`
- Fix issue [#4](https://github.com/bytedance/AabResGuard/issues/4)

## 0.1.0（2019/10/16）
- Add support for resources obfuscation.
- Add support for merge duplicated resources.
- Add support for files filtering.
- Add support for string filtering.
- Added support for `gradle plugin` .
- Add support for `command line` .
