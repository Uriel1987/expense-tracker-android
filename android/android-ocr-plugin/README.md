# Offline Hebrew OCR plugin (Tesseract)

This is a small custom Capacitor plugin that scans a receipt photo fully
on-device, with no network call — using [Tesseract4Android](https://github.com/adaptech-cz/Tesseract4Android),
which (unlike Google ML Kit) has a real Hebrew trained model.

## Setup

1. Follow `gradle.snippet.txt` in this folder to wire the plugin into the
   generated Android project (repo + dependency + copy the Java files +
   register the plugin).

2. **Download the two Hebrew + English trained-data files** and place them at:

   ```
   android/app/src/main/assets/tessdata/heb.traineddata
   android/app/src/main/assets/tessdata/eng.traineddata
   ```

   Use the `tessdata_best` model tier (not `tessdata_fast`) — it's
   meaningfully more accurate and this is an offline app, so the larger
   download only costs disk space, not runtime speed:

   - https://github.com/tesseract-ocr/tessdata_best/raw/main/heb.traineddata (~3 MB)
   - https://github.com/tesseract-ocr/tessdata_best/raw/main/eng.traineddata (~15 MB)

   These are binary files — Claude Code can fetch them for you with `curl`,
   or you can download them by hand and drop them in the folder above.

3. `npx cap sync android`, then build/run from Android Studio (or
   `npx cap run android`).

## How it fits together

- `ReceiptScannerPlugin.java` exposes one method, `scanImage({ path })`,
  which returns `{ text }` — the raw OCR text. It also does image
  preprocessing (grayscale, contrast boost, hard black/white threshold) and
  sets `PSM_SINGLE_COLUMN` before running Tesseract, both of which noticeably
  improve accuracy on real receipt photos vs. handing Tesseract the raw
  camera image.
- The web app (`www/index.html`) calls this plugin from `scanWithTesseract()`
  when it detects it's running as a native Android build
  (`window.Capacitor.Plugins.ReceiptScanner` exists). It takes a photo with
  `@capacitor/camera`, sends the path to `scanImage`, then runs simple text
  heuristics (`extractAmountFromText`, `guessDescriptionFromText`) over the
  raw OCR text to prefill the amount/description fields.

## Realistic accuracy expectations

Offline OCR on a phone photo of a real, possibly creased or badly-lit
thermal-paper receipt is a genuinely hard problem — this is not unique to
this plugin. The preprocessing and `tessdata_best` model above are the two
things actually worth tuning; if amounts and text are still inconsistently
recognized after that, that's a real ceiling of on-device OCR, not a sign
something is misconfigured. The web app is built so that manual entry is
always one tap away and never blocked by a bad scan, and only the numeric
amount from a scan should be trusted without double-checking — the
description/category are a convenience guess to edit, not a source of truth.
The only meaningfully more accurate route is a cloud OCR/vision API (e.g.
Google Cloud Vision), which costs money per call and needs network access —
worth it only if offline isn't a hard requirement.
