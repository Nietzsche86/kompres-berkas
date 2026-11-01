package com.pocketcompress

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

object PdfEngine {
    suspend fun compress(
        context: Context,
        uri: Uri,
        config: CompressConfig,
        onProgress: (Int, Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (!PDFBoxResourceLoader.isReady()) {
            PDFBoxResourceLoader.init(context.applicationContext)
        }

        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalArgumentException("Gagal membuka file PDF")

        val outFile = File(context.cacheDir, "compressed_doc.pdf")

        pfd.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                PDDocument().use { doc ->
                    val totalPages = renderer.pageCount
                    val qualityRatio = (config.quality / 100f).coerceIn(0.1f, 1f)

                    for (i in 0 until totalPages) {
                        val page = renderer.openPage(i)
                        val renderW = (page.width * config.scale).roundToInt().coerceAtLeast(1)
                        val renderH = (page.height * config.scale).roundToInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)

                        try {
                            Canvas(bitmap).drawColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        } finally {
                            page.close()
                        }

                        val pdImage = try {
                            JPEGFactory.createFromImage(doc, bitmap, qualityRatio)
                        } finally {
                            bitmap.recycle()
                        }

                        val pdPage = PDPage(PDRectangle(renderW.toFloat(), renderH.toFloat()))
                        doc.addPage(pdPage)
                        PDPageContentStream(doc, pdPage).use { cs ->
                            cs.drawImage(pdImage, 0f, 0f, renderW.toFloat(), renderH.toFloat())
                        }

                        onProgress(i + 1, totalPages)
                    }

                    doc.save(outFile)
                }
            }
        }

        outFile
    }
}
