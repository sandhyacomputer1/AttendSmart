package com.sandhyyasofttech.attendsmart.Activities;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.sandhyyasofttech.attendsmart.R;

public class WebViewActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private WebView webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }


        initializeViews();
        setupToolbar();
        setupWebView();
        loadUrl();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        String title = getIntent().getStringExtra("title");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(title != null ? title : "Web View");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });
        
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }
        });
    }

    private void loadUrl() {
        String url = getIntent().getStringExtra("url");
        if (url != null && !url.isEmpty()) {
            progressBar.setVisibility(View.VISIBLE);
            webView.loadUrl(url);
        } else {
            // Load default HTML if no URL provided
            String htmlContent = getDefaultHtmlContent();
            webView.loadData(htmlContent, "text/html", "UTF-8");
            progressBar.setVisibility(View.GONE);
        }
    }

    private String getDefaultHtmlContent() {
        String title = getIntent().getStringExtra("title");
        if (title != null && title.equals("Privacy Policy")) {
            return getPrivacyPolicyHtml();
        } else {
            return getTermsAndConditionsHtml();
        }
    }

    private String getPrivacyPolicyHtml() {
        return "<html><body style='padding:20px;font-family:Arial,sans-serif;'>" +
                "<h2>Privacy Policy</h2>" +
                "<p><strong>Last updated: January 2026</strong></p>" +
                "<h3>Information We Collect</h3>" +
                "<p>We collect information that you provide directly to us, including:</p>" +
                "<ul>" +
                "<li>Company information (name, email, phone)</li>" +
                "<li>Employee data (attendance, leaves)</li>" +
                "<li>Usage data and analytics</li>" +
                "</ul>" +
                "<h3>How We Use Your Information</h3>" +
                "<p>We use the information we collect to:</p>" +
                "<ul>" +
                "<li>Provide and maintain our services</li>" +
                "<li>Send notifications and updates</li>" +
                "<li>Improve our application</li>" +
                "</ul>" +
                "<h3>Data Security</h3>" +
                "<p>We implement appropriate security measures to protect your data from unauthorized access.</p>" +
                "<h3>Contact Us</h3>" +
                "<p>If you have questions about this Privacy Policy, please contact us.</p>" +
                "</body></html>";
    }

    private String getTermsAndConditionsHtml() {
        return "<html><body style='padding:20px;font-family:Arial,sans-serif;'>" +
                "<h2>Terms & Conditions</h2>" +
                "<p><strong>Last updated: January 2026</strong></p>" +
                "<h3>Acceptance of Terms</h3>" +
                "<p>By accessing and using AttendSmart, you accept and agree to be bound by these terms.</p>" +
                "<h3>Use License</h3>" +
                "<p>Permission is granted to use this application for attendance management purposes.</p>" +
                "<h3>User Responsibilities</h3>" +
                "<ul>" +
                "<li>Maintain the confidentiality of your account</li>" +
                "<li>Ensure accurate data entry</li>" +
                "<li>Comply with all applicable laws</li>" +
                "</ul>" +
                "<h3>Limitations</h3>" +
                "<p>In no event shall AttendSmart be liable for any damages arising out of the use or inability to use the application.</p>" +
                "<h3>Modifications</h3>" +
                "<p>We reserve the right to modify these terms at any time.</p>" +
                "</body></html>";
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}