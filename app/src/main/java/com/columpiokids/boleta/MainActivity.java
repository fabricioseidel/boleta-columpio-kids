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
        @JavascriptInterface
        public void downloadImage(final String base64Data, final String filename) {
            runOnUiThread(() -> {
                try {
                    String data = base64Data.replace("data:image/jpeg;base64,", "");
                    byte[] bytes = Base64.decode(data, Base64.DEFAULT);

                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                    values.put(MediaStore.Downloads.MIME_TYPE, "image/jpeg");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    }

                    ContentResolver resolver = getContentResolver();
                    Uri uri;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    } else {
                        uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
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
                        "Error al guardar imagen",
                        Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
