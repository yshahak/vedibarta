package org.vedibarta.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;

public class SplashActivity extends Activity {
    private WebView webviewSplash;
    private int index = 0;
    private Intent returnIntent;

    @JavascriptInterface
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);
        returnIntent = new Intent(this, VedibartaActivity.class);
        webviewSplash = findViewById(R.id.webPageSplash);
        webviewSplash.setInitialScale(1);
        WebSettings settings = webviewSplash.getSettings();
        webviewSplash.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                returnIntent.putExtra("log" + index, "splash");
                index++;
                returnIntent.putExtra("log" + index,
                        "url: " + webviewSplash.getUrl());
                index++;

            }
        });

        settings.setJavaScriptEnabled(true);
        // settings.setBuiltInZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setSupportMultipleWindows(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        File dir = new File(getFilesDir() + File.separator + "HtmlFiles");
        dir.mkdirs();
        String fileName = "splash.html";
        File myFile = new File(dir, fileName);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (myFile.length() > 100 && prefs.getBoolean("upgraded", false)) {
            webviewSplash.loadUrl("file:///" + myFile.getAbsolutePath());
            returnIntent.putExtra("log" + index, "splash");
            index++;
            index++;
        } else {
            webviewSplash.loadUrl("file:///android_asset/splash.html");
            returnIntent.putExtra("log" + index, "splash");
            index++;
            index++;
        }

        webviewSplash.addJavascriptInterface(new WebAppInterface(), "orly");

    }

    private class WebAppInterface {
        @JavascriptInterface
        public void close() {
            returnIntent.putExtra("index", index);
            startActivity(returnIntent);
        }

        @JavascriptInterface
        public void rateApp() {
            final String packageName = getApplicationContext().getPackageName();
            Uri uri = Uri.parse("market://details?id=" + packageName);
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            try {
                startActivity(goToMarket);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=" + packageName)));
            }
        }
    }
}
