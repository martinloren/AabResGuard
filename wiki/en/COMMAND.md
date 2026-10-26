**[English](COMMAND.md)** | [简体中文](../zh-cn/COMMAND.md)

# Command line

> **AabResGuard** provides a jar file that can run resource obfuscation by command line.

## Install
The command line tool is the self-contained `aabresguard-core` jar published on the `mvn-repo` branch of this repository.

- Download [aabresguard-core-0.1.2027.jar](https://raw.githubusercontent.com/martinloren/AabResGuard/mvn-repo/com/bytedance/android/aabresguard-core/0.1.2027/aabresguard-core-0.1.2027.jar), or let Gradle/Maven resolve `com.bytedance.android:aabresguard-core:0.1.2027` from `https://raw.githubusercontent.com/martinloren/AabResGuard/mvn-repo`.
- Run it with the JDK 21 runtime:
```cmd
java -jar aabresguard-core-0.1.2027.jar help
```

The `aabresguard <command>` examples below are shorthand for `java -jar aabresguard-core-0.1.2027.jar <command>`.

## Merge duplicated resources
The duplicate files will be merged according to the file `md5` value, only one file will be retained, and then the values in the original resource path index table will be redirected to reduce the volume of the package.
```cmd
aabresguard merge-duplicated-res --bundle=app.aab --output=merged.aab 
--storeFile=debug.store
--storePassword=android
--keyAlias=android
--keyPassword=android
```
The signature information is optional. If you do not specify the signature information, it will be signed using the `Android` default signature file on the PC.

## File filtering
Support for specifying specific files for filtering. Currently only filtering under the `META-INF/` and `lib/` folders is supported.
```cmd
aabresguard filter-file --bundle=app.aab --output=filtered.aab --config=config.xml
--storeFile=debug.store
--storePassword=android
--keyAlias=android
--keyPassword=android
```

Configuration file `config.xml`, filtering rules support `regular expressions`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<resproguard>
    <filter isactive="false">
        <rule value="*/arm64-v8a/*" />
        <rule value="META-INF/*.RSA" />
    </filter>
</resproguard>
```
**Applicable scenarios:** Due to the needs of the business, some channels need to make a full package, but the full package will include all `so` files, `files filter` can be used to filter the `abi` of a certain latitude and will not affect `bundletool` process.

## Resources obfuscation
Resource aliasing of the input `aab` file, and outputting the obfuscated `aab` file, supporting `Merge duplicated resources` and `file filtering`.
```cmd
aabresguard obfuscate-bundle --bundle=app.aab --output=obfuscated.aab --config=config.xml --mapping=mapping.txt
--merge-duplicated-res=true
--assets-obfuscate=true
--storeFile=debug.store
--storePassword=android
--keyAlias=android
--keyPassword=android
```

Optional flags:
- `--assets-obfuscate=true` — also obfuscate file names under `assets/`. **Use with care:** assets are referenced by path at runtime (`AssetManager.open(...)`, `Typeface.createFromAsset(...)`, `file:///android_asset/...` URLs, native code, JS), so every asset loaded via a path literal must be covered by the whitelist (e.g. `*/assets/fonts/*`) or the app will crash at runtime.
- `--class-mapping=class-mapping.txt` — path to an existing class-name mapping file. If it exists, its mappings are reused; the final mapping is always written to `class-mapping.txt` in the output directory.
- `--disable-sign=true` — do not sign the output bundle. Use it when the bundle is signed later (e.g. by Play or a separate signing step).

Configuration file `config.xml`, whitelist support `regular expressions`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<resproguard>
    <issue id="whitelist" isactive="true">
        <path value="com.ss.android.ugc.aweme.R.raw.*" />
    </issue>
    <class-obfuscation isactive="true">
        <!-- Rename matching fully-qualified class names in DEX and binary XML layouts -->
        <rule value="com.example.widget.*" />
        <!-- Classes that must keep their names (Activities, manifest classes, reflection, ...) -->
        <whitelist value="com.example.MainActivity" />
        <whitelist value="com.example.Application" />
    </class-obfuscation>
    <filter isactive="false">
        <rule value="*/arm64-v8a/*" />
        <rule value="META-INF/*.RSA" />
    </filter>
</resproguard>
```

To install the obfuscated bundle on a device, build a universal APK from it with `bundletool`:
```cmd
bundletool build-apks --bundle=obfuscated.aab --output=universal.apks --mode=universal
```
The Gradle plugin automates this with `buildUniversalApk` (see **[Output File](OUTPUT.md)**).

## String filtering
Specify a line-by-line split string list file to filter out value and translations if name is matched in the string resource type
```cmd
aabresguard filter-string --bundle=app.aab --output=filtered.aab --config=config.xml
--storeFile=debug.store
--storePassword=android
--keyAlias=android
--keyPassword=android
```
Configuration file `config.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<resproguard>
    <filter-str isactive="false">
        <!--remove strings in file-->
        <path value="unused.txt" />
        <!--keep strings by language such as en, en-xx, and remove others-->
        <language value="en" />
        <language value="zh" />
    </filter-str>
</resproguard>
```


## #Parameter Description
For the description of the parameters, please execute the following command:

```cmd
aabresguard help
```