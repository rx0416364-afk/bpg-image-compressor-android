package com.example.bpgcompressor

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.graphics.drawable.GradientDrawable
import java.util.concurrent.Executors
import kotlin.math.max

class MainActivity : Activity() {

    companion object {
        private const val REQUEST_PICK_IMAGE = 4101
        private const val REQUEST_SAVE_BPG = 4102
        private const val MAX_IMAGE_EDGE = 4096
        private const val MAX_IMAGE_PIXELS = 18_000_000L

        init {
            System.loadLibrary("bpg")
        }
    }

    private external fun nativeCompress(
        argbPixels: IntArray,
        width: Int,
        height: Int,
        qp: Int,
        compressionLevel: Int
    ): ByteArray?

    private val worker = Executors.newSingleThreadExecutor()

    private var sourceBitmap: Bitmap? = null
    private var sourceUri: Uri? = null
    private var sourceBytes: Long = -1L
    private var compressedBytes: ByteArray? = null
    private var quality = 65

    private lateinit var preview: ImageView
    private lateinit var emptyPreview: TextView
    private lateinit var sourceInfo: TextView
    private lateinit var qualityInfo: TextView
    private lateinit var resultInfo: TextView
    private lateinit var statusInfo: TextView
    private lateinit var qualityBar: SeekBar
    private lateinit var compressButton: Button
    private lateinit var saveButton: Button
    private lateinit var progress: ProgressBar
    private lateinit var resultCard: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(246, 247, 250))
            isFillViewport = true
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(28))
        }
        scroll.addView(page)
        setContentView(scroll)

        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brandMark = TextView(this).apply {
            text = "BPG"
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(48, 77, 190), 14)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        brandRow.addView(brandMark)
        val brandText = TextView(this).apply {
            text = "IMAGE COMPRESSOR"
            textSize = 12f
            letterSpacing = 0.12f
            setTextColor(Color.rgb(93, 103, 122))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(12), 0, 0, 0)
        }
        brandRow.addView(brandText)
        page.addView(brandRow)

        page.addView(textView(
            "Kompres gambar\nlebih efisien",
            29f,
            Color.rgb(23, 31, 49),
            true
        ).apply {
            setPadding(0, dp(24), 0, dp(5))
        })
        page.addView(textView(
            "Ubah gambar menjadi format BPG langsung di perangkat. Tidak perlu koneksi internet.",
            14f,
            Color.rgb(102, 111, 128),
            false
        ).apply {
            setLineSpacing(dp(3).toFloat(), 1f)
            setPadding(0, 0, 0, dp(20))
        })

        val previewCard = card()
        val previewFrame = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(239, 242, 248), 18)
            minimumHeight = dp(210)
        }
        preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            visibility = View.GONE
            maxHeight = dp(250)
        }
        emptyPreview = textView(
            "Pilih gambar untuk mulai",
            15f,
            Color.rgb(112, 122, 141),
            true
        ).apply {
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(70), dp(20), dp(70))
        }
        previewFrame.addView(
            preview,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        previewFrame.addView(emptyPreview)
        previewCard.addView(previewFrame)
        sourceInfo = textView("Belum ada gambar dipilih", 13f, Color.rgb(102, 111, 128), false)
        sourceInfo.setPadding(0, dp(12), 0, 0)
        previewCard.addView(sourceInfo)
        page.addView(previewCard)

        val pickButton = makeButton("Pilih gambar", true)
        pickButton.setOnClickListener { openImagePicker() }
        page.addView(pickButton, matchWidth(dp(52)).apply {
            topMargin = dp(15)
            bottomMargin = dp(18)
        })

        val settingsCard = card()
        settingsCard.addView(textView("Pengaturan kompresi", 17f, Color.rgb(23, 31, 49), true))
        val qualityHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(18), 0, dp(4))
        }
        qualityHeader.addView(textView("Kualitas gambar", 14f, Color.rgb(62, 72, 91), true),
            LinearLayout.LayoutParams(0, dp(24), 1f))
        qualityInfo = textView("$quality%", 14f, Color.rgb(48, 77, 190), true)
        qualityHeader.addView(qualityInfo)
        settingsCard.addView(qualityHeader)

        qualityBar = SeekBar(this).apply {
            max = 100
            progress = quality
            progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(48, 77, 190))
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.rgb(48, 77, 190))
        }
        qualityBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                quality = value.coerceIn(10, 100)
                qualityInfo.text = "$quality%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        settingsCard.addView(qualityBar)
        settingsCard.addView(textView(
            "Kualitas lebih tinggi biasanya menghasilkan file lebih besar. Proses memakai HEVC melalui encoder BPG.",
            12f,
            Color.rgb(115, 124, 141),
            false
        ).apply {
            setLineSpacing(dp(2).toFloat(), 1f)
            setPadding(0, dp(3), 0, 0)
        })
        page.addView(settingsCard)

        compressButton = makeButton("Kompres gambar", true)
        compressButton.isEnabled = false
        compressButton.alpha = 0.48f
        compressButton.setOnClickListener { startCompression() }
        page.addView(compressButton, matchWidth(dp(54)).apply {
            topMargin = dp(18)
        })

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            visibility = View.GONE
        }
        page.addView(progress, matchWidth(dp(4)).apply { topMargin = dp(12) })

        statusInfo = textView("Pilih gambar, lalu mulai kompresi.", 13f, Color.rgb(102, 111, 128), false)
        statusInfo.setPadding(0, dp(10), 0, dp(12))
        page.addView(statusInfo)

        resultCard = card().apply { visibility = View.GONE }
        resultCard.addView(textView("Hasil kompresi", 17f, Color.rgb(23, 31, 49), true))
        resultInfo = textView("", 14f, Color.rgb(65, 75, 94), false)
        resultInfo.setLineSpacing(dp(5).toFloat(), 1f)
        resultInfo.setPadding(0, dp(12), 0, dp(2))
        resultCard.addView(resultInfo)
        page.addView(resultCard)

        saveButton = makeButton("Simpan file .bpg", false).apply {
            visibility = View.GONE
            setOnClickListener { saveCompressedFile() }
        }
        page.addView(saveButton, matchWidth(dp(52)).apply {
            topMargin = dp(13)
        })

        page.addView(textView(
            "Catatan: format BPG belum didukung oleh sebagian besar galeri Android. Aplikasi ini menyimpan file .bpg dan menampilkan ukuran hasilnya; pratinjau di atas adalah gambar sumber.",
            12f,
            Color.rgb(119, 127, 143),
            false
        ).apply {
            setLineSpacing(dp(3).toFloat(), 1f)
            setPadding(dp(2), dp(22), dp(2), 0)
        })
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    @Deprecated("Using the system document picker for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        val uri = data.data!!

        when (requestCode) {
            REQUEST_PICK_IMAGE -> loadSelectedImage(uri)
            REQUEST_SAVE_BPG -> writeCompressedFile(uri)
        }
    }

    private fun loadSelectedImage(uri: Uri) {
        setBusy(true, "Membuka gambar…")
        worker.execute {
            try {
                val size = queryFileSize(uri)
                val decoded = decodeImage(uri)
                runOnUiThread {
                    sourceBitmap?.recycle()
                    sourceBitmap = decoded
                    sourceUri = uri
                    sourceBytes = size
                    compressedBytes = null
                    preview.setImageBitmap(decoded)
                    preview.visibility = View.VISIBLE
                    emptyPreview.visibility = View.GONE
                    sourceInfo.text = "${decoded.width} × ${decoded.height} piksel  •  ${formatBytes(size)}"
                    compressButton.isEnabled = true
                    compressButton.alpha = 1f
                    saveButton.visibility = View.GONE
                    resultCard.visibility = View.GONE
                    statusInfo.text = "Gambar siap dikompres."
                    setBusy(false)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    setBusy(false)
                    statusInfo.text = "Gagal membuka gambar: ${e.message ?: "format tidak didukung"}"
                }
            }
        }
    }

    private fun decodeImage(uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalArgumentException("File bukan gambar yang valid")
        }

        var sample = 1
        while (
            max(bounds.outWidth / sample, bounds.outHeight / sample) > MAX_IMAGE_EDGE ||
            bounds.outWidth.toLong() * bounds.outHeight.toLong() / (sample.toLong() * sample) > MAX_IMAGE_PIXELS
        ) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = contentResolver.openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: throw IllegalArgumentException("Android tidak dapat membaca gambar ini")

        val orientation = try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) ExifInterface.ORIENTATION_NORMAL
                else ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(degrees) }
        val corrected = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (corrected !== bitmap) bitmap.recycle()
        return corrected
    }

    private fun queryFileSize(uri: Uri): Long {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else -1L
            } ?: -1L
        } catch (_: Exception) {
            -1L
        }
    }

    private fun startCompression() {
        val bitmap = sourceBitmap ?: return
        val width = bitmap.width
        val height = bitmap.height
        val qp = (51 - (quality * 51 / 100)).coerceIn(0, 51)

        setBusy(true, "Mengompres gambar dengan BPG…")
        compressButton.isEnabled = false
        worker.execute {
            try {
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                val output = nativeCompress(pixels, width, height, qp, 6)
                    ?: throw IllegalStateException("Encoder BPG gagal memproses gambar")
                runOnUiThread {
                    compressedBytes = output
                    val resultSize = output.size.toLong()
                    val comparison = if (sourceBytes > 0L) {
                        val delta = (1.0 - resultSize.toDouble() / sourceBytes.toDouble()) * 100.0
                        when {
                            delta >= 0 -> "Ukuran berkurang ${"%.1f".format(java.util.Locale.US, delta)}%"
                            else -> "Ukuran bertambah ${"%.1f".format(java.util.Locale.US, -delta)}%"
                        }
                    } else {
                        "Ukuran file sumber tidak tersedia"
                    }
                    resultInfo.text = "Ukuran awal: ${formatBytes(sourceBytes)}\nUkuran BPG: ${formatBytes(resultSize)}\n$comparison\n\nKualitas: $quality%  •  QP: $qp  •  Encoder: x265"
                    resultCard.visibility = View.VISIBLE
                    saveButton.visibility = View.VISIBLE
                    statusInfo.text = "Kompresi selesai. File BPG siap disimpan."
                    setBusy(false)
                    compressButton.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    setBusy(false)
                    compressButton.isEnabled = true
                    statusInfo.text = "Kompresi gagal: ${e.message ?: "kesalahan tidak diketahui"}"
                }
            }
        }
    }

    private fun saveCompressedFile() {
        if (compressedBytes == null) return
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, "hasil_kompresi.bpg")
        }
        startActivityForResult(intent, REQUEST_SAVE_BPG)
    }

    private fun writeCompressedFile(uri: Uri) {
        val bytes = compressedBytes ?: return
        try {
            contentResolver.openOutputStream(uri, "w").use { output ->
                if (output == null) throw IllegalStateException("Tidak dapat membuka lokasi penyimpanan")
                output.write(bytes)
                output.flush()
            }
            statusInfo.text = "File berhasil disimpan (${formatBytes(bytes.size.toLong())})."
        } catch (e: Exception) {
            statusInfo.text = "Gagal menyimpan file: ${e.message ?: "kesalahan penyimpanan"}"
        }
    }

    private fun setBusy(busy: Boolean, message: String? = null) {
        progress.visibility = if (busy) View.VISIBLE else View.GONE
        if (message != null) statusInfo.text = message
        if (busy) {
            compressButton.isEnabled = false
            compressButton.alpha = 0.65f
        } else {
            compressButton.alpha = if (sourceBitmap == null) 0.48f else 1f
        }
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(Color.WHITE, 20)
        setPadding(dp(17), dp(17), dp(17), dp(17))
        elevation = dp(2).toFloat()
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(4) }
    }

    private fun makeButton(label: String, primary: Boolean): Button = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (primary) Color.WHITE else Color.rgb(48, 77, 190))
        background = rounded(
            if (primary) Color.rgb(48, 77, 190) else Color.rgb(231, 236, 250),
            16
        )
        stateListAnimator = null
        setPadding(dp(14), dp(5), dp(14), dp(5))
    }

    private fun textView(value: String, size: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun matchWidth(height: Int) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        height
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "Tidak diketahui"
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
        return String.format(java.util.Locale.US, "%.2f MB", kb / 1024.0)
    }

    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }
}
