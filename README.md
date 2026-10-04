# מעקב הוצאות — Expense Tracker

A Hebrew (RTL) monthly expense tracker: categories, per-category budgets,
recurring expenses, search/filter, a category pie chart, month-over-month
(and per-category) comparison, CSV export, and receipt scanning with
auto-fill (cloud vision inside claude.ai, offline Tesseract OCR in the
native Android build).

## Folder guide

- **`web/`** — the app as plain, self-contained HTML files. These are what's
  published as claude.ai Artifacts:
  - `expense-tracker-he.html` — the real app (this is your live data when
    opened inside claude.ai; storage falls back to the browser's
    `localStorage` anywhere else).
  - `expense-tracker-demo.html` — a restyled, pre-seeded copy in the newer
    visual design (ivory/navy/amber, Frank Ruhl Libre + Heebo), meant for
    trying things out — not connected to your real data.

- **`design/`** — the visual-design mockups (Claude's "Design" artifact
  type: `canvas.json` + one `.dc.html` file per screen). Reference material
  for the look both HTML apps above are aiming for; not runnable on their
  own outside claude.ai.

- **`android/`** — the Capacitor project that packages `www/index.html`
  (a copy of the real app, with two additions — see `android/README.md`)
  as a native Android app with offline-friendly storage and OCR. This
  folder needs `npm install` and `npx cap add android` before it's a real
  Android project — see `android/README.md` to get started.

## Picking this up in Claude Code

This project was mostly built and iterated on inside claude.ai chat (the
`web/` and `design/` folders came straight from there — they're finished
and working). The `android/` folder is freshly scaffolded here since the
original Android project's files lived in an earlier chat session's
temporary workspace and weren't recoverable — treat it as a solid starting
point to build and smoke-test, not a guaranteed-working APK yet.

Good first things to do in Claude Code:

1. `cd android && npm install && npx cap add android`, then follow
   `android/android-ocr-plugin/README.md` to fetch the two Tesseract
   trained-data files and wire the OCR plugin in.
2. Build the APK once (Android Studio, or `npx cap run android`) and check:
   - the app opens without a Chrome address bar / browser chrome,
   - adding an expense and restarting the app keeps the data,
   - the receipt-scan button actually returns usable text.
3. If you also have the original PWABuilder-packaged app (from
   `assetlinks.json` / `com.uriel.expensestracker`) still installed on your
   phone, remember this Capacitor build uses a **different** package ID
   (`com.uriel.expensestracker.app`) on purpose, so the two won't
   conflict — see `android/README.md` for why that matters.

## Continuing the web app / design work

`web/expense-tracker-he.html` and `web/expense-tracker-demo.html` are
plain HTML/CSS/JS with no build step — open directly in a browser to work
on them. If you keep iterating on both claude.ai (for quick previews and
sharing) and in Claude Code (for the Android side), the simplest way to
stay in sync is: whichever side changes `web/expense-tracker-he.html`,
copy the result into `android/www/index.html` too — that copy has the two
native-only additions (storage layer, Tesseract scan branch) layered on
top, described in `android/README.md`.
