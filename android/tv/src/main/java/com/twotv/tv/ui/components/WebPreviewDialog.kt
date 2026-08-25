package com.twotv.tv.ui.components

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
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

    private var webView: WebView? = null
    private var headerBar: View? = null

    private val hideHeaderRunnable = Runnable {
        headerBar?.visibility = View.GONE
    }

    private fun scheduleHeaderAutoHide() {
        headerBar?.removeCallbacks(hideHeaderRunnable)
        headerBar?.postDelayed(hideHeaderRunnable, 3000)
    }

    private fun showHeaderAndAutoHide() {
        headerBar?.visibility = View.VISIBLE
        scheduleHeaderAutoHide()
    }

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
        webView = view.findViewById(R.id.previewWebView)
        val closeBtn = view.findViewById<View>(R.id.btnCloseWeb)
        val btnOpenWith = view.findViewById<View>(R.id.btnOpenWithWeb)

        titleTextView.text = title
        closeBtn.setOnClickListener { dismiss() }
        btnOpenWith?.setOnClickListener {
            openWithExternalBrowser(url)
        }

        applyFocusScale(closeBtn)
        btnOpenWith?.let { applyFocusScale(it) }

        scheduleHeaderAutoHide()

        webView?.settings?.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        webView?.webViewClient = WebViewClient()

        webView?.isFocusable = true
        webView?.isFocusableInTouchMode = true
        webView?.requestFocus()

        webView?.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY == 0) {
                showHeaderAndAutoHide()
            } else if (scrollY > oldScrollY) {
                headerBar?.removeCallbacks(hideHeaderRunnable)
                headerBar?.visibility = View.GONE
            }
        }

        var validUrl = url
        if (!validUrl.startsWith("http://") && !validUrl.startsWith("https://")) {
            validUrl = "https://$validUrl"
        }
        webView?.loadUrl(validUrl)
    }

    private fun applyFocusScale(view: View) {
        view.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.10f).scaleY(1.10f).translationZ(12f).setDuration(150).start()
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(150).start()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_CHANNEL_DOWN,
            KeyEvent.KEYCODE_PAGE_DOWN -> {
                headerBar?.removeCallbacks(hideHeaderRunnable)
                headerBar?.visibility = View.GONE
                webView?.scrollBy(0, 350)
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_CHANNEL_UP,
            KeyEvent.KEYCODE_PAGE_UP -> {
                webView?.scrollBy(0, -350)
                if (webView?.scrollY == 0) {
                    showHeaderAndAutoHide()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                headerBar?.removeCallbacks(hideHeaderRunnable)
                headerBar?.visibility = View.GONE
                webView?.scrollBy(300, 0)
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                webView?.scrollBy(-300, 0)
                if (webView?.scrollY == 0) {
                    showHeaderAndAutoHide()
                }
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (webView?.canGoBack() == true) {
                    webView?.goBack()
                } else {
                    dismiss()
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun openWithExternalBrowser(urlStr: String) {
        try {
            var validUrl = urlStr
            if (!validUrl.startsWith("http://") && !validUrl.startsWith("https://")) {
                validUrl = "https://$validUrl"
            }
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(validUrl)).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = android.content.Intent.createChooser(intent, "Apri link con...")
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Impossibile aprire con app esterne: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
