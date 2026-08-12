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
**Always whitelist** classes whose names must remain stable at runtime:
- Android components declared in `AndroidManifest.xml` (`Application`, `Activity`, `Service`, `Receiver`, `Provider`).
- Custom views inflated by class name from XML (if not matched by the rule, they are already safe).
- Classes accessed by reflection (`Class.forName(...)`, JSON parsers, dependency injection, ...).
- Classes referenced from native code or JS bridges.

Example:
```
com.example.Application
com.example.MainActivity
com.example.*ViewThatIsReflected*
```
