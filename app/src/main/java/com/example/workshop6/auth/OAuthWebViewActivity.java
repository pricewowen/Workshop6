package com.example.workshop6.auth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.workshop6.BuildConfig;
import com.example.workshop6.R;

/**
 * Runs the server-side OAuth redirect chain inside a {@link WebView} so sign-in does not depend
 * on the emulator Chrome app (first-run dialogs there are often untappable / “frozen”).
 */
public class OAuthWebViewActivity extends AppCompatActivity {

    public static final String EXTRA_START_URL = "oauth_start_url";
    public static final String EXTRA_TICKET = "oauth_ticket";

    private WebView webView;
    private ProgressBar progressBar;
    private boolean finished;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oauth_webview);

        Toolbar toolbar = findViewById(R.id.oauth_toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        progressBar = findViewById(R.id.oauth_progress);
        webView = findViewById(R.id.oauth_webview);
        setupWebView(webView);

        String startUrl = getIntent().getStringExtra(EXTRA_START_URL);
        if (startUrl == null || startUrl.trim().isEmpty()) {
            Toast.makeText(this, R.string.login_oauth_claim_failed, Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        webView.loadUrl(startUrl.trim());
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView w) {
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setSupportMultipleWindows(false);
        // Match a normal mobile Chrome UA so IdPs are less likely to block embedded flows on emulators.
        s.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/120.0.0.0 Mobile Safari/537.36"
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
            CookieManager.getInstance().setAcceptThirdPartyCookies(w, true);
        }
        CookieManager.getInstance().setAcceptCookie(true);

        w.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    return handleUri(request.getUrl());
                }
                return super.shouldOverrideUrlLoading(view, request);
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUri(Uri.parse(url));
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(android.view.View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(android.view.View.GONE);
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && request.isForMainFrame()) {
                    Toast.makeText(
                            OAuthWebViewActivity.this,
                            R.string.login_oauth_webview_error,
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(
                    WebView view,
                    int errorCode,
                    String description,
                    String failingUrl
            ) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    Toast.makeText(
                            OAuthWebViewActivity.this,
                            R.string.login_oauth_webview_error,
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        });
    }

    private boolean handleUri(Uri uri) {
        if (uri == null) {
            return false;
        }
        if (!BuildConfig.OAUTH_REDIRECT_SCHEME.equalsIgnoreCase(uri.getScheme())) {
            return false;
        }
        if (!"oauth".equalsIgnoreCase(uri.getHost())) {
            return false;
        }
        String ticket = uri.getQueryParameter("ticket");
        if (ticket == null || ticket.isEmpty()) {
            return false;
        }
        if (finished) {
            return true;
        }
        finished = true;
        Intent data = new Intent();
        data.putExtra(EXTRA_TICKET, ticket);
        setResult(RESULT_OK, data);
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
