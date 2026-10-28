# સુરક્ષિત બ્રાઉઝર

Android WebView આધારિત privacy-first browser starter project.

## Features
- No analytics, ads SDK, account, or third-party tracking SDK.
- Built-in blocklist for a selection of known tracker hosts.
- Auto-clear on app background/exit (default ON).
- Clear local browser-history entries for 24 hours, 7 days, 1 month, 1 year, or all time.
- Clear all website data: history, cookies, cache, and WebStorage.
- No location, contacts, camera, microphone, or storage permission.

## Honest limitations
Android WebView does not expose reliable per-cookie, per-cache-item, or website-local-storage timestamps. Therefore time-range clearing removes this app's own timestamped history entries for that period. It cannot selectively clear only that period's cookies/cache/site storage. Use “સંપૂર્ણ ડેટા” to clear all website data.

Android has no guaranteed callback for force-stop. With auto-clear ON this app clears on Activity `onStop()`, so data may also be cleared when the app goes to background or the screen is switched off. Tracker blocking covers only a limited known-host list and cannot guarantee all tracking is blocked.

## Build APK from GitHub
1. Extract this ZIP on your phone.
2. Upload the extracted files/folders to the root of a GitHub repository (do not upload only the ZIP).
3. Open Actions; the workflow runs on push or can be run manually.
4. Download artifact `Surakshit-Browser-debug-apk`, extract `app-debug.apk`, and install it.
