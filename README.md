# AabResGuard - Artic Fox Edition
<h1 align="center">
  <img src="wiki/images/logo.png" height="220" width="460"/>
  <p align="center" style="font-size: 0.3em">The tool of obfuscated aab resources</p>
</h1>

[Download V.0.1.2027](https://raw.githubusercontent.com/martinloren/AabResGuard/mvn-repo/com/bytedance/android/aabresguard-core/0.1.2027/aabresguard-core-0.1.2027.jar)

[![License](https://img.shields.io/badge/license-Apache2.0-brightgreen)](LICENSE) [![Bundletool](https://img.shields.io/badge/Dependency-Bundletool/1.18.3-blue)](https://github.com/google/bundletool)

**[English](README.md)** | [简体中文](wiki/zh-cn/README.md)

> Powered by bytedance douyin android team.

## Features
> The tool of obfuscated aab resources.

- **Obfuscate resource paths and names:** Both resource file paths (`res/layout/main.xml` → `res/a/b.xml`) and resource entry names (`string/app_name` → `string/a`) are obfuscated in the resource table, so the `resources.arsc` of APKs generated from the obfuscated bundle no longer contains readable resource names. Note: APKs must be generated from the **obfuscated** bundle (e.g. via `bundletool build-apks` or Play); APKs built directly with `assembleRelease` are never processed.
- **Merge duplicated resources:** Consolidate duplicate resource files to reduce package size.
- **Filter bundle files:** Support for filtering files in the `bundle` package. Currently only supports filtering in the `MATE-INFO/` and `lib/` paths.
- **White list:** The resources in the whitelist are not to be obfuscated.
- **Incremental obfuscation:** Input the `mapping` file to support incremental obfuscation.
- **Remove string:** Input the unused file splits by lines to support remove strings.
- **Opt-in assets obfuscation:** with `enableObfuscateAssets = true` (plugin) or `--assets-obfuscate=true` (CLI), file names under `assets/` are obfuscated too. **Off by default and use with care:** unlike resources, assets have no numeric-ID indirection — they are referenced at runtime by their path string (`AssetManager.open(...)`, `Typeface.createFromAsset(...)`, `WebView` `file:///android_asset/...`, native code, JS, ...), often inside third-party libraries. Every asset loaded via a path literal must be whitelisted (e.g. `*/assets/fonts/*`) or the app will crash at runtime.
- **Class-name obfuscation for custom views:** Optionally rename selected fully-qualified class names (e.g. custom views) in the compiled DEX files and in binary XML layouts. Use `classObfuscation { ... }` (plugin) or the `<class-obfuscation>` config (CLI). A whitelist is mandatory for classes that must keep their names (Activities, Services, Application class, manifest-referenced classes, classes accessed via reflection, etc.).
- **???:** Looking ahead, there will be more feature support, welcome to submit PR & issue.

## [Data of size savings](wiki/en/DATA.md)
**AabResGuard** is a resource obfuscation tool powered by the douyin Android team. It has been launched at the end of July 2018 in several overseas products, such as **Tiktok, Vigo**, etc.
There is no feedback on related resource issues.
For more data details, please go to **[Data of size savings](wiki/en/DATA.md)**.

## Install
Every release is published to the `mvn-repo` branch of this repository, so the compiled library can be used without building it from source.

### Gradle plugin
Resolve the plugin from the repository published on the `mvn-repo` branch.
```gradle
buildscript {
  repositories {
    mavenCentral()
    google()
    maven { url 'https://raw.githubusercontent.com/martinloren/AabResGuard/mvn-repo' }
  }
  dependencies {
    classpath "com.bytedance.android:aabresguard-plugin:0.1.2027"
  }
}
```

### Library dependency
`aabresguard-core` is a self-contained jar (all runtime dependencies are shaded and relocated), so it adds no transitive dependencies to your build.
```gradle
repositories {
  maven { url 'https://raw.githubusercontent.com/martinloren/AabResGuard/mvn-repo' }
}
dependencies {
  implementation "com.bytedance.android:aabresguard-core:0.1.2027"
}
```

### Command line jar
[Download aabresguard-core-0.1.2027.jar](https://raw.githubusercontent.com/martinloren/AabResGuard/mvn-repo/com/bytedance/android/aabresguard-core/0.1.2027/aabresguard-core-0.1.2027.jar) and run it with the JDK 21 runtime:
```cmd
java -jar aabresguard-core-0.1.2027.jar help
```

## Quick start
- **Command tool:** Support command line.
- **Gradle plugin:** Support for `gradle plugin`, using the original packaging command to execute obfuscation.

### Gradle plugin
Configured in `build.gradle(root project)`
```gradle
buildscript {
  repositories {
    mavenCentral()
    mavenLocal()
    google()
    maven { url 'https://raw.githubusercontent.com/martinloren/AabResGuard/mvn-repo' }
   }
  dependencies {
    classpath "com.bytedance.android:aabresguard-plugin:0.1.2027"
  }
}
```

Configured in `build.gradle(application)`
```gradle
apply plugin: "com.bytedance.android.aabResGuard"
aabResGuard {
    enableObfuscate = true // Obfuscate resource paths and names
    //enableObfuscateAssets = true // Also obfuscate file names under assets/ (default: false).
                                   // WARNING: assets are referenced by path at runtime, so every
                                   // asset loaded via a path literal must be whitelisted below
                                   // (e.g. "*/assets/fonts/*") or the app will crash.
    //mappingFile = file("mapping.txt").toPath() // Mapping file used for incremental obfuscation
    whiteList = [ // White list rules
        "*.R.raw.*",
        "*.R.drawable.icon",
        // Assets loaded by path (only needed when enableObfuscateAssets = true)
        // "*/assets/fonts/*",
        // Google-services & Firebase
        "*.R.string.google_app_id",
        "*.R.string.gcm_defaultSenderId",
        "*.R.string.default_web_client_id",
        "*.R.string.ga_trackingId",
        "*.R.string.firebase_database_url",
        "*.R.string.google_api_key",
        "*.R.string.google_crash_reporting_api_key",
        "*.R.string.default_web_client_id",
        "*.R.string.gcm_defaultSenderId",
        "*.R.string.google_app_id",
        "*.R.string.google_crash_reporting_api_key",
        "*.R.string.google_storage_bucket",
        "*.R.string.project_id"
    ]
    obfuscatedBundleFileName = "duplicated-app.aab" // Obfuscated file name, must end with '.aab'
    mergeDuplicatedRes = true // Whether to allow the merge of duplicate resources
    enableFilterFiles = true // Whether to allow filter files
    filterList = [ // file filter rules
        "*/arm64-v8a/*",
        "META-INF/*"
    ]

    enableFilterStrings = false // switch of filter strings
    unusedStringPath = file("unused.txt").toPath() // strings will be filtered in this file
    languageWhiteList = ["en", "zh"] // keep en,en-xx,zh,zh-xx etc. remove others.

    classObfuscation {
        isActive = false // Set true to enable class-name obfuscation for custom views.
        rules = [ // Classes matching these globs are renamed.
            "com.example.widget.*"
        ]
        whitelist = [ // Classes that MUST keep their original names.
            "com.example.MainActivity",
            "com.example.Application",
            "com.example.widget.*ViewThatIsReflected*"
        ]
        // mappingFile = file("class-mapping.txt").toPath() // Optional input for stable names
    }
}
```

The `aabResGuard plugin` intrudes the `bundle` packaging process and can be obfuscated by executing the original packaging commands.
```cmd
./gradlew clean :app:bundleDebug --stacktrace
```

Get the obfuscated `bundle` file path by `gradle` .
```groovy
def aabResGuardTask = project.tasks.named("aabresguard${VARIANT_NAME}")
Path bundlePath = aabResGuardTask.get().obfuscatedBundleFile.get().asFile.toPath()
```

### [Whitelist](wiki/en/WHITELIST.md)
The resources that can not be confused. Welcome PR your configs which is not included in [WHITELIST](wiki/en/WHITELIST.md)

### [Command line](wiki/en/COMMAND.md)
**AabResGuard** provides a `jar` file that can be executed directly from the command line. More details, please go to **[Command Line](wiki/en/COMMAND.md)**.

### [Output](wiki/en/OUTPUT.md)
After the packaging is completed, the obfuscated file and the log files will be output. More details, please go to **[Output File](wiki/en/OUTPUT.md)**.
- **resources-mapping.txt:** Resource obfuscation mapping, which can be used as the next obfuscation input to achieve incremental obfuscate.
- **class-mapping.txt:** Class-name obfuscation mapping (only when class obfuscation is enabled). Written to the output directory on every run and reused automatically as the input of the next run in the same directory, so class names stay stable across builds. Supplying `classObfuscation.mappingFile` points the input at another file. Names already present in the input mapping are always reserved before new names are generated, so adding or removing classes never produces a duplicate class name (which would abort the DEX writer with `Class <name> has already been interned`).
- **aab:** Optimized aab file.
- **-duplicated.txt:** duplicated file logging.

## [Change log](wiki/en/CHANGELOG.md)
Version change log. More details, please go to **[Change Log](wiki/en/CHANGELOG.md)** .

## [Contribute](wiki/en/CONTRIBUTOR.md)
Read the details to learn how to participate in the improvement **AabResGuard**.

### Contributor
* [JingYeoh](https://github.com/JingYeoh)
* [Jun Li]()
* [Zilai Jiang](https://github.com/Zzzia)
* [Zhiqian Yang](https://github.com/yangzhiqian)
* [Xiaoshuang Bai (Designer)](https://www.behance.net/shawnpai)
* [Martinloren](https://github.com/martinloren)

## Thanks
* [AndResGuard](https://github.com/shwenzhang/AndResGuard/)
* [BundleTool](https://github.com/google/bundletool)
