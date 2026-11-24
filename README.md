<p align="center">
  <img src="docs/banner.png" alt="Stickerly" width="100%">
</p>

# Stickerly

**Stickerly** adalah aplikasi Android untuk membuat stiker WhatsApp sendiri. Pilih teks, gambar, atau video, atur sedikit, lalu kirim paketnya langsung ke WhatsApp. Semua dilakukan di HP.

## Fitur

- **Stiker teks**: ketik tulisan, pilih gaya huruf, warna, outline tebal, dan ukuran. Hasilnya stiker transparan 512x512.
- **Stiker gambar**: ambil dari galeri, geser dan cubit untuk mengatur posisi, lalu pilih bentuk (kotak, membulat, atau lingkaran).
- **Stiker video (bergerak)**: pilih video, potong sampai 10 detik, dan Stickerly mengubahnya menjadi WebP animasi. Ukuran dijaga di bawah batas WhatsApp (500 KB) dengan menurunkan kualitas dan fps secara otomatis kalau perlu.
- **Video bersuara**: stiker WhatsApp tidak menyimpan suara di dalam filenya, jadi Stickerly mengekspor potongan video yang sama sebagai MP4 bersuara yang bisa dibagikan terpisah.
- **Paket stiker**: simpan stiker ke paket (3 sampai 30 stiker per paket), lalu kirim ke WhatsApp atau WhatsApp Business dengan satu ketukan. Stiker statis dan animasi dipisah per paket sesuai aturan WhatsApp.

## Tampilan

Antarmuka bertema hijau-putih mengikuti logo, dengan outline hitam tebal dan bayangan solid supaya terasa seperti stiker. Beranda berisi empat pintu masuk (Teks, Gambar, Video, Suara) dan tab Paket untuk mengelola hasil.

## Cara kerja singkat

1. Stiker dibuat dan disimpan di penyimpanan internal aplikasi.
2. Stickerly menyediakan paketnya ke WhatsApp lewat Sticker ContentProvider resmi.
3. WhatsApp membaca paket itu dan menambahkannya ke koleksi stikermu.

Stiker video dibuat tanpa FFmpeg: frame diambil dengan `MediaMetadataRetriever`, dikompres ke WebP, lalu dirakit menjadi WebP animasi oleh kode sendiri (`AnimatedWebp.kt`). Aplikasinya jadi lebih ringan.

## Teknologi

- Kotlin dan Jetpack Compose (Material 3)
- Android 7.0 (API 24) ke atas
- Package: `com.stikerly.app`

## Build

Langkah build lewat GitHub Actions ada di [BUILD.md](BUILD.md).

## Lisensi

MIT. Lihat file [LICENSE](LICENSE). Dibuat oleh AdityaDevID.
