# Android build (Capacitor)

Wraps `www/index.html` (the expense tracker) as a native Android app, with
two things a browser/PWA can't give you:

- **Storage independent of Chrome** — data is saved via `@capacitor/preferences`,
  which lives outside the WebView/Chrome storage. Clearing Chrome's data or
  uninstalling a PWABuilder-packaged version of this app does **not** touch it.
- **Offline Hebrew OCR** for receipt scanning, via a custom Tesseract plugin
  (see `android-ocr-plugin/README.md`) — Google ML Kit has no Hebrew model,
  so this app doesn't use it.

## First-time setup

```bash
cd android
npm install
npx cap add android
```

Then follow `android-ocr-plugin/README.md` and `android-ocr-plugin/gradle.snippet.txt`
to wire in the OCR plugin (this has to happen after `cap add android` since
that's what generates the `android/` native project folder to add the Java
files and Gradle config into).

```bash
npx cap sync android
npx cap open android    # opens Android Studio — build/run from there
```

## Package ID

`capacitor.config.json` uses `com.uriel.expensestracker.app` (not `.native` —
`native` is a reserved Java keyword, so it can't be a package name) — deliberately
**different** from the package ID used for any PWABuilder-packaged version of
this app you may have set up before (that one used `com.uriel.expensestracker`
per your earlier `assetlinks.json`). Two Android apps with the same package ID
but different signing keys can't both be installed on one device
(`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) — keep them distinct unless you
intend this Capacitor build to fully replace the PWABuilder one.

## What's carried over from `www/index.html`'s browser/claude.ai version

The base app (categories, budgets, recurring expenses, search/filter, the
pie chart, CSV export, and both month-comparison modes) is unchanged. Two
things were added specifically for this native build, since they only make
sense outside a browser:

- The storage layer (`lsGet`/`lsSet`) now checks for
  `@capacitor/preferences` first, falling back to `localStorage` when not
  running as a native app.
- The scan button now also recognizes a `"tesseract"` mode
  (`state.scanMode`), calling the native OCR plugin instead of the
  claude.ai cloud-vision path used inside claude.ai.
