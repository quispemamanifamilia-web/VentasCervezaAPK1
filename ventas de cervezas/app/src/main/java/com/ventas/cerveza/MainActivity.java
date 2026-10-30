
package com.ventas.cerveza;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.*;
import android.print.PrintManager;
import android.content.Context;
import android.widget.Toast;
import java.io.OutputStream;

public class MainActivity extends Activity {

    WebView web;
    ValueCallback<Uri[]> chooser;

    String pendingName = "ventas.json";
    String pendingType = "application/json";
    byte[] pendingBytes;

    static final int PICK_FILE = 41;
    static final int SAVE_FILE = 42;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);

        // Abrir enlaces de WhatsApp fuera de la aplicación
        web.setWebViewClient(new WebViewClient() {

            private boolean abrirEnlaceExterno(Uri uri) {
                String scheme = uri.getScheme();
                String host = uri.getHost();

                boolean esWhatsapp =
                    ("https".equalsIgnoreCase(scheme) ||
                     "http".equalsIgnoreCase(scheme))
                    && host != null
                    && (
                        host.equalsIgnoreCase("wa.me") ||
                        host.equalsIgnoreCase("api.whatsapp.com") ||
                        host.equalsIgnoreCase("www.whatsapp.com")
                    );

                if (esWhatsapp) {
                    try {
                        Intent intent = new Intent(
                            Intent.ACTION_VIEW, uri
                        );
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(
                            MainActivity.this,
                            "No se pudo abrir WhatsApp",
                            Toast.LENGTH_LONG
                        ).show();
                    }
                    return true;
                }

                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                WebView view,
                WebResourceRequest request
            ) {
                return abrirEnlaceExterno(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                WebView view,
                String url
            ) {
                return abrirEnlaceExterno(Uri.parse(url));
            }
        });

        // Seleccionar archivos JSON
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                WebView view,
                ValueCallback<Uri[]> cb,
                FileChooserParams params
            ) {
                chooser = cb;

                Intent i = new Intent(
                    Intent.ACTION_OPEN_DOCUMENT
                );
                i.setType("application/json");
                i.addCategory(Intent.CATEGORY_OPENABLE);

                startActivityForResult(i, PICK_FILE);
                return true;
            }
        });

        // Funciones para imprimir y guardar archivos
        web.addJavascriptInterface(new Object() {

            @JavascriptInterface
            public void printPage() {
                runOnUiThread(() -> {
                    PrintManager pm = (PrintManager)
                        getSystemService(Context.PRINT_SERVICE);

                    pm.print(
                        "Reporte ventas",
                        web.createPrintDocumentAdapter(
                            "Ventas cerveza"
                        ),
                        null
                    );
                });
            }

            @JavascriptInterface
            public void saveFile(
                String name,
                String content,
                String type
            ) {
                runOnUiThread(() -> {
                    pendingName = name;
                    pendingType = type;

                    pendingBytes = content.getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                    );

                    Intent i = new Intent(
                        Intent.ACTION_CREATE_DOCUMENT
                    );

                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType(type);
                    i.putExtra(Intent.EXTRA_TITLE, name);

                    startActivityForResult(i, SAVE_FILE);
                });
            }

        }, "Android");

        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(
        int request,
        int result,
        Intent data
    ) {
        super.onActivityResult(request, result, data);

        if (request == PICK_FILE) {
            if (chooser != null) {
                chooser.onReceiveValue(
                    result == RESULT_OK && data != null
                        ? new Uri[]{data.getData()}
                        : null
                );
                chooser = null;
            }
        }

        if (request == SAVE_FILE &&
            result == RESULT_OK &&
            data != null &&
            pendingBytes != null) {

            try (OutputStream o =
                getContentResolver().openOutputStream(
                    data.getData()
                )) {

                o.write(pendingBytes);

                Toast.makeText(
                    this,
                    "Archivo guardado",
                    Toast.LENGTH_SHORT
                ).show();

            } catch (Exception e) {
                Toast.makeText(
                    this,
                    "No se pudo guardar",
                    Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
