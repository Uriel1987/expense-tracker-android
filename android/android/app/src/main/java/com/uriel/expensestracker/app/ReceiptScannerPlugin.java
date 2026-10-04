package com.uriel.expensestracker.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Bridges the web app to Tesseract OCR (via the Tesseract4Android library),
 * running fully offline/on-device, using the official Hebrew trained model
 * (heb.traineddata) plus English (for digits/latin fallback) — both bundled
 * as app assets. See README.md in this folder for where to get those two
 * files; they are NOT included here (binary trained-data files, a few MB
 * each, fetched from the official tesseract-ocr GitHub repos).
 *
 * Unlike Google ML Kit, Tesseract genuinely has a Hebrew script model, so
 * this can attempt real Hebrew text — but accuracy on a real, possibly
 * crumpled/angled receipt photo is still well short of perfect. The web app
 * only fully trusts the extracted AMOUNT (digits are the same across
 * scripts); the recognized text is also used to take a best-effort guess at
 * a description, which the person can and should still check/edit.
 */
@CapacitorPlugin(name = "ReceiptScanner")
public class ReceiptScannerPlugin extends Plugin {

    // Order matters for Tesseract's combined-language mode; "heb+eng" is passed as one string.
    private static final String[] TESSDATA_LANGS = { "heb", "eng" };
    private static final String TESS_LANG_MODE = "heb+eng";
    private static final int THRESHOLD = 140;

    @PluginMethod
    public void scanImage(PluginCall call) {
        String path = call.getString("path");
        if (path == null) {
            call.reject("Missing 'path' — pass the local file path of the receipt photo (e.g. from @capacitor/camera).");
            return;
        }

        Bitmap bitmap = decodeBitmap(path);
        if (bitmap == null) {
            call.reject("Could not read the image at: " + path);
            return;
        }
        Bitmap processed = preprocess(bitmap);

        TessBaseAPI tess = null;
        try {
            String dataPath = ensureTessData(getContext());
            tess = new TessBaseAPI();
            boolean ok = tess.init(dataPath, TESS_LANG_MODE);
            if (!ok) {
                call.reject("Tesseract failed to initialize — check that heb.traineddata and eng.traineddata are present under assets/tessdata/ (see README.md in this folder).");
                return;
            }
            tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_COLUMN);
            tess.setImage(processed);
            String text = tess.getUTF8Text();

            JSObject ret = new JSObject();
            ret.put("text", text == null ? "" : text);
            call.resolve(ret);
        } catch (IOException e) {
            call.reject("Could not prepare Tesseract language data: " + e.getMessage());
        } finally {
            if (tess != null) tess.recycle();
        }
    }

    private Bitmap decodeBitmap(String path) {
        try {
            if (path.startsWith("file://") || path.startsWith("content://")) {
                Uri uri = Uri.parse(path);
                return android.provider.MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
            }
            File file = new File(path);
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (IOException | SecurityException e) {
            return null;
        }
    }

    /**
     * Grayscale + contrast boost + hard black/white threshold. Cuts noise from
     * receipt paper texture and lighting before Tesseract ever sees the image —
     * this, plus PSM_SINGLE_COLUMN above, is most of what separates "unusable"
     * from "workable" accuracy on real phone photos of receipts.
     */
    private Bitmap preprocess(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap gray = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(gray);
        Paint paint = new Paint();
        ColorMatrix saturation = new ColorMatrix();
        saturation.setSaturation(0f);
        float contrast = 1.6f;
        float translate = (-0.5f * contrast + 0.5f) * 255f;
        ColorMatrix contrastMatrix = new ColorMatrix(new float[]{
                contrast, 0, 0, 0, translate,
                0, contrast, 0, 0, translate,
                0, 0, contrast, 0, translate,
                0, 0, 0, 1, 0
        });
        saturation.postConcat(contrastMatrix);
        paint.setColorFilter(new ColorMatrixColorFilter(saturation));
        canvas.drawBitmap(src, 0, 0, paint);

        int[] pixels = new int[w * h];
        gray.getPixels(pixels, 0, w, 0, 0, w, h);
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int r = Color.red(p), g = Color.green(p), b = Color.blue(p);
            int lum = (r + g + b) / 3;
            pixels[i] = (lum < THRESHOLD) ? Color.BLACK : Color.WHITE;
        }
        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        result.setPixels(pixels, 0, w, 0, 0, w, h);
        return result;
    }

    /**
     * Tesseract needs its .traineddata files on plain filesystem storage under
     * a "tessdata" subfolder — it cannot read them straight out of the APK's
     * compressed assets. This copies them out to internal storage once, the
     * first time the app scans anything, and reuses the copy after that.
     */
    private String ensureTessData(Context context) throws IOException {
        File root = new File(context.getFilesDir(), "tesseract");
        File tessdataDir = new File(root, "tessdata");
        if (!tessdataDir.exists()) {
            tessdataDir.mkdirs();
        }
        for (String lang : TESSDATA_LANGS) {
            File out = new File(tessdataDir, lang + ".traineddata");
            if (!out.exists()) {
                copyAsset(context, "tessdata/" + lang + ".traineddata", out);
            }
        }
        return root.getAbsolutePath();
    }

    private void copyAsset(Context context, String assetPath, File out) throws IOException {
        try (InputStream in = context.getAssets().open(assetPath);
             OutputStream os = new FileOutputStream(out)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
        }
    }
}
