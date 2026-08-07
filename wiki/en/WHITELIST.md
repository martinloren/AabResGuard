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
