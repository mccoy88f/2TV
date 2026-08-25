package com.twotv.tv.ui.components

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.twotv.tv.R
import java.io.File

class PdfPreviewDialog(
    context: Context,
    private val title: String,
    private val pdfFile: File
) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    private var recyclerView: RecyclerView? = null
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_pdf_preview, null)
        setContentView(view)

        val titleTextView = view.findViewById<TextView>(R.id.pdfTitleText)
        recyclerView = view.findViewById(R.id.pdfRecyclerView)
        headerBar = view.findViewById(R.id.pdfHeaderBar)
        val closeBtn = view.findViewById<View>(R.id.btnClosePdf)
        val btnOpenWith = view.findViewById<View>(R.id.btnOpenWithPdf)

        titleTextView.text = title
        closeBtn.setOnClickListener { dismiss() }
        btnOpenWith?.setOnClickListener {
            openWithExternalApp(pdfFile)
        }

        applyFocusScale(closeBtn)
        btnOpenWith?.let { applyFocusScale(it) }

        scheduleHeaderAutoHide()

        recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                if (!rv.canScrollVertically(-1)) {
                    showHeaderAndAutoHide()
                } else if (dy > 0) {
                    headerBar?.removeCallbacks(hideHeaderRunnable)
                    headerBar?.visibility = View.GONE
                }
            }
        })

        try {
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)

            recyclerView?.layoutManager = LinearLayoutManager(context)
            recyclerView?.adapter = PdfPageAdapter(pdfRenderer)
            recyclerView?.requestFocus()
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
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_CHANNEL_DOWN,
            KeyEvent.KEYCODE_PAGE_DOWN -> {
                headerBar?.removeCallbacks(hideHeaderRunnable)
                headerBar?.visibility = View.GONE
                recyclerView?.smoothScrollBy(0, 450)
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_CHANNEL_UP,
            KeyEvent.KEYCODE_PAGE_UP -> {
                recyclerView?.smoothScrollBy(0, -450)
                if (recyclerView?.canScrollVertically(-1) == false) {
                    showHeaderAndAutoHide()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                headerBar?.removeCallbacks(hideHeaderRunnable)
                headerBar?.visibility = View.GONE
                recyclerView?.smoothScrollBy(0, 900)
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                recyclerView?.smoothScrollBy(0, -900)
                if (recyclerView?.canScrollVertically(-1) == false) {
                    showHeaderAndAutoHide()
                }
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                dismiss()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun openWithExternalApp(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.twotv.tv.fileprovider",
                file
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = android.content.Intent.createChooser(intent, "Apri PDF con...")
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Impossibile aprire con app esterne: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private class PdfPageAdapter(private val renderer: PdfRenderer) :
        RecyclerView.Adapter<PdfPageAdapter.PageViewHolder>() {

        class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ImageView = view.findViewById(R.id.pdfPageImageView)
            val pageNumText: TextView = view.findViewById(R.id.pdfPageNumText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pdf_page, parent, false)
            return PageViewHolder(view)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            val page = renderer.openPage(position)
            val bitmap = Bitmap.createBitmap(
                page.width * 2,
                page.height * 2,
                Bitmap.Config.ARGB_8888
            )
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            holder.imageView.setImageBitmap(bitmap)
            holder.pageNumText.text = "Pagina ${position + 1} di ${renderer.pageCount}"
        }

        override fun getItemCount(): Int = renderer.pageCount
    }
}
