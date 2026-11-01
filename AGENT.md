# AI CODING AGENT OPERATING MANUAL (TOKEN-SAVER & SENIOR CODE STANDARD)

## 1. PROTOKOL HEMAT TOKEN (WAJIB DIIKUTI)
1. JANGAN BACA SEMUA FILE: Saat sesi baru dimulai, HANYA baca AGENT.md dan TASK.md.
2. BACA SESUAI KEBUTUHAN: Hanya buka file kode sumber (.kt atau .gradle.kts) yang disebutkan secara eksplisit pada task yang sedang aktif di TASK.md.
3. UPDATE TASK.md: Setelah menyelesaikan satu task, ubah status [ ] menjadi [x] di TASK.md, tuliskan ringkasan 1 baris jika ada catatan penting, lalu BERHENTI dan minta user memulai sesi baru.
4. DILARANG JALANKAN GRADLE LOKAL: Environment ini berjalan di UserLAnd Android tanpa Android SDK lokal. JANGAN PERNAH menjalankan ./gradlew atau gradle. Kompilasi dilakukan di GitHub Actions.

## 2. PRINSIP KODE SENIOR ANDROID DEVELOPER ("50 LINES OVER 200 LINES")
Jika sebuah logika bisa diselesaikan dalam 50 baris yang bersih, idiomatik, dan aman, DILARANG menulis 200 baris kode yang bertele-tele.

### Aturan Penulisan Kode:
- Zero Boilerplate: Jangan buat Interface, Repository, UseCase, atau Dependency Injection untuk aplikasi sekecil ini. Gunakan fungsi langsung (top-level functions atau object).
- Idiomatic Resource Management: Selalu gunakan .use { } untuk semua InputStream, OutputStream, ParcelFileDescriptor, PdfRenderer, dan PDDocument. Jangan tulis blok try-catch-finally manual untuk menutup stream.
- Explicit Memory Safety: Selalu panggil bitmap.recycle() tepat setelah bitmap selesai dikompresi/ditulis ke PDF.
- Single-File UI: Letakkan seluruh komponen Jetpack Compose di MainActivity.kt menggunakan fungsi Composable kecil yang ringkas. Tidak perlu membuat folder theme/ terpisah, cukup gunakan MaterialTheme standar.
- No Internet Permission: Jangan pernah menambahkan izin internet di AndroidManifest.xml.

### Checklist Self-Review Senior Developer (Cek sebelum menyimpan file):
1. Apakah ada memory leak? (Pastikan Context tidak disimpan di variabel statis dan Bitmap di-recycle).
2. Apakah operasi I/O memblokir Main Thread? (Semua kompresi wajib dibungkus withContext(Dispatchers.IO)).
3. Apakah kode mudah dibaca dalam 10 detik? (Gunakan early return, hindari nested if lebih dari 2 level).
