package com.twotv.tv.ui.components

import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.twotv.tv.R
import java.io.File

class ImagePreviewDialog(
    context: Context,
    private val title: String,
    private val pathOrUrl: String
) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    private var headerBar: View? = null
    private val hideHeaderRunnable = Runnable {
        headerBar?.visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_image_preview, null)
        setContentView(view)

        val titleTextView = view.findViewById<TextView>(R.id.imageTitleText)
        val imageView = view.findViewById<ImageView>(R.id.previewImageView)
        headerBar = view.findViewById(R.id.imageHeaderBar)
        val closeBtn = view.findViewById<View>(R.id.btnCloseImage)
        val btnOpenWith = view.findViewById<View>(R.id.btnOpenWithImage)

        titleTextView.text = title
        closeBtn.setOnClickListener { dismiss() }
        btnOpenWith?.setOnClickListener {
            openWithExternalApp()
        }

        applyFocusScale(closeBtn)
        btnOpenWith?.let { applyFocusScale(it) }

        headerBar?.postDelayed(hideHeaderRunnable, 3000)

        try {
            val file = File(pathOrUrl)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                imageView.setImageBitmap(bitmap)
            } else {
                imageView.setImageURI(Uri.parse(pathOrUrl))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            headerBar?.visibility = View.VISIBLE
            headerBar?.removeCallbacks(hideHeaderRunnable)
            headerBar?.postDelayed(hideHeaderRunnable, 3000)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun openWithExternalApp() {
        try {
            val file = File(pathOrUrl)
            val intent = if (file.exists()) {
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "com.twotv.tv.fileprovider",
                    file
                )
                val ext = file.extension.lowercase()
                val mimeType = when (ext) {
                    "jpg", "jpeg", "png", "webp", "gif", "bmp" -> "image/*"
                    else -> "*/*"
                }
                android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(pathOrUrl)).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            val chooser = android.content.Intent.createChooser(intent, "Apri immagine con...")
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Impossibile aprire con app esterne: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}

