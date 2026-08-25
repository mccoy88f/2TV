package com.twotv.tv.ui.components

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import com.twotv.tv.R

class WebPreviewDialog(
    context: Context,
    private val title: String,
    private val url: String
) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_web_preview, null)
        setContentView(view)

        val titleTextView = view.findViewById<TextView>(R.id.webTitleText)
        val webView = view.findViewById<WebView>(R.id.previewWebView)
        val closeBtn = view.findViewById<View>(R.id.btnCloseWeb)

        titleTextView.text = title
        closeBtn.setOnClickListener { dismiss() }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        webView.webViewClient = WebViewClient()

        var validUrl = url
        if (!validUrl.startsWith("http://") && !validUrl.startsWith("https://")) {
            validUrl = "https://$validUrl"
        }
        webView.loadUrl(validUrl)
    }
}
