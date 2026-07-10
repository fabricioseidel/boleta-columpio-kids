package com.columpiokids.boleta;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);

        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private class AndroidBridge {

        /** Guarda cualquier archivo (PDF, PNG, etc.) en Descargas */
        @JavascriptInterface
        public void downloadFile(final String base64Data, final String filename, final String mimeType) {
            runOnUiThread(() -> saveToDownloads(base64Data, filename, mimeType));
        }

        /** Compatibilidad con la versión anterior (imágenes JPEG) */
        @JavascriptInterface
        public void downloadImage(final String base64Data, final String filename) {
            String data = base64Data;
            String mime = "image/jpeg";
            if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
                mime = "application/pdf";
            } else if (filename != null && filename.toLowerCase().endsWith(".png")) {
                mime = "image/png";
            }
            final String cleanData = data
                .replace("data:image/jpeg;base64,", "")
                .replace("data:image/png;base64,", "")
                .replace("data:application/pdf;base64,", "")
                .replaceAll("^data:.*?;base64,", "");
            final String finalMime = mime;
            runOnUiThread(() -> saveToDownloads(cleanData, filename, finalMime));
        }

        private void saveToDownloads(String base64Data, String filename, String mimeType) {
            try {
                byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);

                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                }

                ContentResolver resolver = getContentResolver();
                Uri uri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                } else {
                    uri = resolver.insert(MediaStore.Files.getContentUri("external"), values);
                }

                if (uri != null) {
                    try (OutputStream os = resolver.openOutputStream(uri)) {
                        if (os != null) {
                            os.write(bytes);
                        }
                    }
                    Toast.makeText(MainActivity.this,
                        "Guardado en Descargas: " + filename,
                        Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                Toast.makeText(MainActivity.this,
                    "Error al guardar archivo",
                    Toast.LENGTH_SHORT).show();
            }
        }
    }
}
