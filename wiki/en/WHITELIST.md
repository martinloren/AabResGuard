# Whitelist

Welcome PR your configs which is not included in whitelist.

## Google Services
```
*.R.string.default_web_client_id
*.R.string.firebase_database_url
*.R.string.gcm_defaultSenderId
*.R.string.google_api_key
*.R.string.google_app_id
*.R.string.google_crash_reporting_api_key
*.R.string.google_storage_bucket
*.R.string.project_id
*.R.string.com.crashlytics.android.build_id
```

## Assets (only when assets obfuscation is enabled)
When `enableObfuscateAssets` / `--assets-obfuscate` is on, whitelist rules also match asset file
paths (`<module>/assets/...`). **Every asset loaded via a path literal must be whitelisted** —
this includes assets loaded by third-party libraries:
```
# e.g. icon font libraries loading TTFs via Typeface.createFromAsset(...)
*/assets/fonts/*
# e.g. a WebView page referencing other assets relatively
*/assets/web/*
```

## Class names (only when class-name obfuscation is enabled)
When `classObfuscation { isActive = true }` / `<class-obfuscation isactive="true">` is on, the
whitelist matches **fully-qualified class names** (e.g. `com.example.MainActivity`).

Class-name obfuscation rewrites every occurrence it can see statically:
- type references in the DEX files (including array element types),
- `const-string` / `const-string/jumbo` literals that equal a mapped class name (this covers
  `Class.forName("...")` and similar reflective lookups),
- string values of annotations,
- binary XML resources under `res/**/*.xml` (custom view tags such as
  `<com.example.widget.MyView ...>` and other class-valued attributes).

It does **not** rewrite `AndroidManifest.xml`, `assets/`, `res/raw/`, native code, or names that are
concatenated at runtime, so anything referenced from those places must be whitelisted.

**Always whitelist** classes whose names must remain stable at runtime:
- Android components declared in `AndroidManifest.xml` (`Application`, `Activity`, `Service`, `Receiver`, `Provider`).
- Custom views inflated by class name from XML (if not matched by the rule, they are already safe).
- Classes accessed by reflection (`Class.forName(...)`, JSON parsers, dependency injection, ...).
- Classes referenced from native code or JS bridges.

Names already present in an input `class-mapping.txt` are reused verbatim, and newly added classes
always receive a name that no mapped class owns yet, so repeated builds stay stable.

Example:
```
com.example.Application
com.example.MainActivity
com.example.*ViewThatIsReflected*
```
