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
        val closeBtn = view.findViewById<View>(R.id.btnClosePdf)

        titleTextView.text = title
        closeBtn.setOnClickListener { dismiss() }

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

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_CHANNEL_DOWN,
            KeyEvent.KEYCODE_PAGE_DOWN -> {
                recyclerView?.smoothScrollBy(0, 450)
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_CHANNEL_UP,
            KeyEvent.KEYCODE_PAGE_UP -> {
                recyclerView?.smoothScrollBy(0, -450)
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                recyclerView?.smoothScrollBy(0, 900)
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                recyclerView?.smoothScrollBy(0, -900)
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                dismiss()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
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
