package com.yuvraj.openchatai.data.attachments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import com.yuvraj.openchatai.data.model.Attachment
import com.yuvraj.openchatai.data.model.AttachmentKind
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

class AttachmentException(message: String) : Exception(message)

/**
 * Turns a picked content Uri into an [Attachment]:
 * - images are downscaled and re-encoded as base64 JPEG for vision models
 * - PDFs get their text extracted on-device
 * - text-like documents (txt, md, csv, json, code) are read directly
 */
class AttachmentProcessor(private val context: Context) {

    suspend fun process(uri: Uri): Attachment = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val name = queryDisplayName(uri) ?: "file"
        val mime = resolver.getType(uri) ?: guessMimeFromName(name)
        val size = queryFileSize(uri)
        if (size > MAX_FILE_BYTES) {
            throw AttachmentException("\"$name\" is too large (max ${MAX_FILE_BYTES / (1024 * 1024)} MB).")
        }

        when {
            mime.startsWith("image/") -> processImage(uri, name, size)
            mime == "application/pdf" || name.endsWith(".pdf", ignoreCase = true) ->
                processPdf(uri, name, size)
            else -> processText(uri, name, mime, size)
        }
    }

    private fun processImage(uri: Uri, name: String, size: Long): Attachment {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // decodeStream always returns null in bounds-only mode; only a null
        // stream means the file could not be opened.
        val boundsStream = resolver.openInputStream(uri)
            ?: throw AttachmentException("Could not open \"$name\".")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw AttachmentException("\"$name\" is not a valid image.")
        }

        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_IMAGE_DIMENSION) {
            sampleSize *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: throw AttachmentException("Could not decode \"$name\".")

        val scaled = scaleDown(decoded, MAX_IMAGE_DIMENSION)
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        if (scaled !== decoded) decoded.recycle()
        scaled.recycle()

        val base64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        return Attachment(
            id = UUID.randomUUID().toString(),
            name = name,
            kind = AttachmentKind.IMAGE,
            mimeType = "image/jpeg",
            base64Data = base64,
            sizeBytes = size,
        )
    }

    private fun processPdf(uri: Uri, name: String, size: Long): Attachment {
        ensurePdfBoxInitialized()
        val text = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                PDDocument.load(stream).use { document ->
                    val stripper = PDFTextStripper()
                    stripper.startPage = 1
                    stripper.endPage = minOf(document.numberOfPages, MAX_PDF_PAGES)
                    stripper.getText(document)
                }
            } ?: throw AttachmentException("Could not open \"$name\".")
        } catch (e: AttachmentException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "PDF extraction failed for $name")
            throw AttachmentException("Could not read \"$name\" — the PDF may be corrupted or password-protected.")
        }
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            throw AttachmentException("\"$name\" contains no extractable text (it may be a scanned PDF).")
        }
        return Attachment(
            id = UUID.randomUUID().toString(),
            name = name,
            kind = AttachmentKind.PDF,
            mimeType = "application/pdf",
            textContent = trimmed.take(MAX_TEXT_CHARS),
            sizeBytes = size,
        )
    }

    private fun processText(uri: Uri, name: String, mime: String, size: Long): Attachment {
        val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes()
        } ?: throw AttachmentException("Could not open \"$name\".")
        val text = raw.toString(Charsets.UTF_8)
        val replacementRatio = if (text.isEmpty()) 1.0 else
            text.count { it == '\uFFFD' }.toDouble() / text.length
        if (text.isBlank() || replacementRatio > 0.05) {
            throw AttachmentException("\"$name\" is not a readable text document. Supported: images, PDFs and text files.")
        }
        return Attachment(
            id = UUID.randomUUID().toString(),
            name = name,
            kind = AttachmentKind.TEXT,
            mimeType = mime,
            textContent = text.take(MAX_TEXT_CHARS),
            sizeBytes = size,
        )
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= maxDimension) return bitmap
        val ratio = maxDimension.toDouble() / largest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).roundToInt().coerceAtLeast(1),
            (bitmap.height * ratio).roundToInt().coerceAtLeast(1),
            true,
        )
    }

    private fun queryDisplayName(uri: Uri): String? =
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }

    private fun queryFileSize(uri: Uri): Long =
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) cursor.getLong(index) else 0L
        } ?: 0L

    private fun guessMimeFromName(name: String): String {
        val extension = name.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "pdf" -> "application/pdf"
            "json" -> "application/json"
            "csv" -> "text/csv"
            "md" -> "text/markdown"
            "html", "htm" -> "text/html"
            else -> "text/plain"
        }
    }

    private fun ensurePdfBoxInitialized() {
        synchronized(pdfBoxLock) {
            if (!pdfBoxInitialized) {
                PDFBoxResourceLoader.init(context.applicationContext)
                pdfBoxInitialized = true
            }
        }
    }

    private companion object {
        const val TAG = "AttachmentProcessor"
        const val MAX_FILE_BYTES = 25L * 1024 * 1024
        const val MAX_IMAGE_DIMENSION = 1568
        const val JPEG_QUALITY = 82
        const val MAX_PDF_PAGES = 100
        const val MAX_TEXT_CHARS = 60_000

        val pdfBoxLock = Any()
        var pdfBoxInitialized = false
    }
}
