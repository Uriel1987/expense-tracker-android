# Expense tracker — notes for Claude

## Releasing to the phone (GitHub Releases + Obtainium) — the ONLY way to ship builds

The Android app (`android/`, Capacitor) is installed on the user's phone via **Obtainium**,
tracking GitHub Releases of this repo (`Uriel1987/expense-tracker-android`). Do **not** hand the
user debug APKs or ask them to uninstall/reinstall — that wipes their expense data.

- After changing `web/expense-tracker-he.html`, copy it into `android/www/index.html` as described in
  `README.md` (keeping the native-only additions), commit, then run `.\release.ps1 -Notes "what changed"`
  from the repo root. It runs `cap sync`, builds a signed release APK, pushes, and runs
  `gh release create` with the APK attached. The user then taps Update in Obtainium.
- Versions are automatic: `versionCode` = git commit count, tag/`versionName` = `1.0.<count>`
  (passed as `-PappVersionCode` / `-PappVersionName`). Never hardcode them in `app/build.gradle`.
- Signing uses the shared keystore `C:\Users\Riley\keys\release.jks`; passwords are only in
  `C:\Users\Riley\.gradle\gradle.properties` (`RELEASE_*`). Never commit keystores or passwords,
  never generate a new keystore — a different key means the app can't be updated in place.
- `applicationId` (`com.uriel.expensestracker.app`) must never change.
- Env: JAVA_HOME = `C:\Program Files\Android\Android Studio\jbr`, ANDROID_HOME =
  `%LOCALAPPDATA%\Android\Sdk` (the script sets both if unset).
