# Cara build (semua bisa dari HP)

1. Upload isi project ke repo GitHub baru.
2. Buat token di GitHub: Settings > Developer settings > Personal access tokens > **Tokens (classic)**, centang scope **repo**.
3. Di repo: Settings > Secrets and variables > Actions > New repository secret. Nama `GH_PAT`, isi token tadi.
4. Tab **Actions** > **Setup Keystore** > Run workflow (sekali saja). Keystore dibuat otomatis dan masuk ke Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
5. Tab **Actions** > **Build Release APK** > Run workflow. APK muncul di halaman **Releases**.

Setiap push ke `main` juga otomatis membuat release baru. Kalau langkah 2 sampai 4 dilewati, build tetap jalan memakai debug key.

## Cadangkan keystore

Setelah langkah 4, download artifact `stickerly-keystore-backup` dari halaman run (hanya tersedia 1 hari). Kalau keystore hilang, APK baru tidak bisa meng-update APK lama.
