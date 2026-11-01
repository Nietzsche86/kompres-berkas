# Product Requirement Document (PRD): PocketCompress Offline (v2.1)

## 1. Executive Summary
Aplikasi utilitas Android 100% offline untuk mengompresi foto (JPEG/PNG/WEBP) dan dokumen PDF hasil scan tanpa koneksi internet. Dikembangkan secara headless di UserLAnd menggunakan Agentic AI dan dikompilasi di GitHub Actions.

## 2. Tech Stack & Explicit Versions
- **Kotlin:** 2.0.21 | **AGP:** 8.7.2 | **Gradle:** 8.9 (via GitHub Actions setup-gradle@v4)
- **SDK:** compileSdk = 35, targetSdk = 35, minSdk = 26, jvmTarget = "17"
- **Jetpack Compose BOM:** 2024.10.01 (Material 3)
- **Dependencies:**
  - androidx.activity:activity-compose:1.9.3
  - androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7
  - androidx.exifinterface:exifinterface:1.3.7
  - com.tom-roush:pdfbox-android:2.0.27.0
  - org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0

## 3. Flat & Minimalist Directory Structure
Gunakan struktur paket rata (flat package) untuk meminimalkan jumlah file dan navigasi direktori:
pocket-compress/
 .github/workflows/build.yml
 app/
   ├── build.gradle.kts
   └── src/main/
       ├── AndroidManifest.xml
       └── java/com/pocketcompress/
           ├── Models.kt           # Semua data class & sealed interface (< 45 baris)
           ├── ImageEngine.kt      # Logika EXIF, Subsampling & Kompresi Foto (< 85 baris)
           ├── PdfEngine.kt        # Logika PdfRenderer + PdfBox JPEG stream (< 95 baris)
           ├── MainViewModel.kt    # Stateflow & SAF File Saver (< 90 baris)
           └── MainActivity.kt     # UI Compose Material 3 + ActivityResult (< 150 baris)
 build.gradle.kts
 gradle.properties
 settings.gradle.kts
 PRD.md
 AGENT.md
 TASK.md

## 4. Data Contracts (Models.kt)
    package com.pocketcompress

    import android.net.Uri
    import java.io.File

    enum class Mode { IMAGE, PDF }

    data class CompressConfig(
        val mode: Mode = Mode.IMAGE,
        val quality: Int = 70,         // 10..100 (Image JPEG quality / PDF JPEG quality)
        val scale: Float = 0.6f,       // 0.2f..1.0f (Resolusi skala dimensi)
        val targetKb: Int? = null      // Opsional untuk iterasi ukuran foto
    )

    sealed interface UiState {
        data object Idle : UiState
        data class Selected(val uri: Uri, val name: String, val size: Long, val mode: Mode) : UiState
        data class Running(val progress: String) : UiState
        data class Done(val origSize: Long, val tempFile: File, val outName: String) : UiState
        data class Error(val msg: String) : UiState
    }

## 5. Core Engine Algorithms

### 5.1. ImageEngine (ImageEngine.kt)
1. Baca orientasi rotasi (90/180/270) menggunakan ExifInterface dari contentResolver.openInputStream(uri).
2. Baca dimensi gambar (inJustDecodeBounds = true), hitung inSampleSize berdasarkan config.scale.
3. Decode bitmap dengan Bitmap.Config.RGB_565 (hemat 50% memori dibanding ARGB_8888), terapkan rotasi Matrix jika ada.
4. Simpan hasil ke File(context.cacheDir, "compressed_img.jpg"). Jika targetKb != null, lakukan perulangan pengurangan kualitas -10 sampai ukuran file di bawah target atau quality <= 15. Wajib bitmap.recycle().

### 5.2. PdfEngine (PdfEngine.kt)
1. Panggil PDFBoxResourceLoader.init(context).
2. Buka ParcelFileDescriptor dari URI, masukkan ke PdfRenderer. Buat dokumen baru PDDocument().
3. Iterasi setiap halaman:
   - Render halaman PdfRenderer.Page ke Bitmap (ARGB_8888, ukuran dikali config.scale) yang sudah diberi warna dasar putih (canvas.drawColor(Color.WHITE)).
   - Konversi Bitmap menjadi PDImageXObject menggunakan JPEGFactory.createFromImage(doc, bitmap, config.quality / 100f).
   - Segera panggil bitmap.recycle() dan page.close().
   - Gambar PDImageXObject ke PDPage baru dan tambahkan ke PDDocument.
4. Simpan PDDocument ke File(context.cacheDir, "compressed_doc.pdf") dan tutup semua resource.

### 5.3. File Saving Flow (SAF)
- Hasil kompresi selalu disimpan di context.cacheDir terlebih dahulu (UiState.Done).
- Ketika user menekan tombol "Simpan ke HP", panggil ActivityResultContracts.CreateDocument dan salin isi tempFile ke Uri tujuan menggunakan inputStream.copyTo(outputStream).
