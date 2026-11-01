# PROJECT TASK TRACKER & SESSION STATE

> Instruksi untuk Agent: Cari task pertama dengan tanda [ ]. Hanya baca file yang tertera pada bagian Target File di task tersebut. Kerjakan hingga tuntas, ubah menjadi [x], lalu berhenti.

---

## FASE 1: Project Skeleton & CI/CD Pipeline
- [x] Task 1.1: Konfigurasi Gradle & CI/CD (Selesai: konfigurasi repositori, dependensi eksplisit, dan workflow GitHub Actions CI siap)
  - Referensi: PRD.md Bagian 2 & 3.
  - Target File (Buat Baru): settings.gradle.kts, build.gradle.kts, gradle.properties, app/build.gradle.kts, .github/workflows/build.yml.
  - Spesifikasi:
    - Konfigurasi repositori google() dan mavenCentral().
    - Masukkan semua dependensi eksplisit dari PRD.md Bagian 2 ke app/build.gradle.kts.
    - Buat .github/workflows/build.yml menggunakan actions/checkout@v4, actions/setup-java@v4 (temurin 17), gradle/actions/setup-gradle@v4 (gradle 8.9), jalankan gradle assembleDebug --stacktrace, dan upload APK dengan actions/upload-artifact@v4.
  - Batas Kode: Maksimal 65 baris untuk app/build.gradle.kts.

- [x] Task 1.2: AndroidManifest & Kontrak Data (Models.kt) (Selesai: manifest tanpa izin internet/storage dan Models.kt siap)
  - Referensi: PRD.md Bagian 3 & 4.
  - Target File (Buat Baru): app/src/main/AndroidManifest.xml, app/src/main/java/com/pocketcompress/Models.kt.
  - Spesifikasi:
    - AndroidManifest.xml hanya berisi deklarasi <application> dan <activity android:name=".MainActivity" android:exported="true">. Tanpa permission internet/storage.
    - Salin kontrak data Mode, CompressConfig, dan UiState ke Models.kt.
  - Batas Kode: Maksimal 40 baris per file.

---

## FASE 2: Core Compression Engines (Senior Level Conciseness)
- [x] Task 2.1: Mesin Kompresi Foto (ImageEngine.kt) (Selesai: EXIF rotation, subsampling RGB_565, loop targetKb, dan daur ulang bitmap)
  - Referensi: PRD.md Bagian 5.1. Hanya baca Models.kt untuk tipe data.
  - Target File (Buat Baru): app/src/main/java/com/pocketcompress/ImageEngine.kt.
  - Spesifikasi:
    - Buat object ImageEngine dengan satu fungsi utama: suspend fun compress(context: Context, uri: Uri, config: CompressConfig): File = withContext(Dispatchers.IO) { ... }.
    - Baca rotasi EXIF dengan ExifInterface, decode subsampled bitmap (RGB_565), rotasi jika perlu, kompres ke JPEG (dukung iterasi targetKb), tulis ke File(context.cacheDir, "compressed_img.jpg"), panggil bitmap.recycle().
  - Batas Kode: Target 55-80 baris. Gunakan .use {} untuk semua stream.

- [x] Task 2.2: Mesin Kompresi PDF (PdfEngine.kt) (Selesai: PdfRenderer white canvas, konversi JPEG stream, daur ulang bitmap per halaman)
  - Referensi: PRD.md Bagian 5.2. Hanya baca Models.kt untuk tipe data.
  - Target File (Buat Baru): app/src/main/java/com/pocketcompress/PdfEngine.kt.
  - Spesifikasi:
    - Buat object PdfEngine dengan fungsi: suspend fun compress(context: Context, uri: Uri, config: CompressConfig, onProgress: (Int, Int) -> Unit): File = withContext(Dispatchers.IO) { ... }.
    - Inisialisasi PDFBoxResourceLoader.init(context).
    - Render setiap halaman PdfRenderer ke Bitmap berlatar Color.WHITE, konversi ke JPEG stream di PDDocument memakai JPEGFactory.createFromImage, panggil bitmap.recycle(), simpan ke File(context.cacheDir, "compressed_doc.pdf").
  - Batas Kode: Target 65-90 baris. Pastikan tidak ada kebocoran memori pada loop halaman.

---

## FASE 3: State Management & UI
- [x] Task 3.1: ViewModel & Helper SAF (MainViewModel.kt) (Selesai: StateFlow UiState & CompressConfig, coroutine engine execution, SAF saver)
  - Referensi: PRD.md Bagian 4 & 5.3. Baca Models.kt, ImageEngine.kt, dan PdfEngine.kt.
  - Target File (Buat Baru): app/src/main/java/com/pocketcompress/MainViewModel.kt.
  - Spesifikasi:
    - Kelola StateFlow<UiState> dan StateFlow<CompressConfig>.
    - Fungsi selectFile(context: Context, uri: Uri, mode: Mode): ambil nama & ukuran file via OpenableColumns.
    - Fungsi startCompress(context: Context): panggil ImageEngine atau PdfEngine sesuai mode, tangkap exception ke UiState.Error.
    - Fungsi saveToUri(context: Context, sourceFile: File, destUri: Uri): salin file via copyTo di Dispatchers.IO.
  - Batas Kode: Target 65-85 baris.

- [x] Task 3.2: Single-Activity Jetpack Compose UI (MainActivity.kt) (Selesai: UI Material 3 satu layar, Tabs, Sliders, Preset KB, dan SAF Document Pick/Save)
  - Referensi: Hanya baca Models.kt dan MainViewModel.kt.
  - Target File (Buat Baru): app/src/main/java/com/pocketcompress/MainActivity.kt.
  - Spesifikasi:
    - Implementasikan ComponentActivity dengan tema Material 3 inline (MaterialTheme).
    - Gunakan rememberLauncherForActivityResult untuk OpenDocument (pilih file) dan CreateDocument (simpan hasil).
    - Buat UI bersih satu layar: Tab (Foto | PDF), Tombol Pilih File, Slider Kualitas (10% - 100%), Slider Skala Resolusi (20% - 100%), Opsi Preset Target KB (khusus Foto), indikator Running, serta kartu hasil (Ukuran Awal vs Ukuran Akhir + Tombol Simpan).
  - Batas Kode: Target 110-145 baris.

---

## FASE 4: Audit Senior Developer & Perbaikan Build
- [x] Task 4.1: Code Review & Dead Code Elimination (Selesai: audit sintaks Kotlin, proteksi stream/descriptor .use {}, daur ulang bitmap, dan zero dead code/unused import)
  - Instruksi Khusus: Periksa ImageEngine.kt, PdfEngine.kt, MainViewModel.kt, dan MainActivity.kt. Hapus import yang tidak terpakai, pastikan semua Stream/ParcelFileDescriptor memakai .use {}, dan pastikan tidak ada bug sintaks Kotlin sebelum di-push ke GitHub Actions.
