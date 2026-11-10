# BPG Image Compressor untuk Android

Proyek aplikasi Android minimalis untuk mengompres satu gambar menjadi format **BPG (`.bpg`)** secara offline. Encoder menggunakan sumber `libbpg` dan `x265` dari arsip yang diberikan.

## Fitur

- Memilih gambar melalui pemilih file bawaan Android.
- Mempertahankan transparansi jika gambar sumber memiliki kanal alpha.
- Mengatur kualitas melalui slider 10–100%.
- Melakukan encoding langsung di perangkat; gambar tidak dikirim ke server.
- Menampilkan ukuran file sumber dan ukuran hasil BPG.
- Menyimpan keluaran sebagai file `.bpg`.

Gambar dikonversi ke YCbCr 8-bit 4:2:0 dan dikodekan sebagai gambar BPG statis. Preset x265 ditetapkan ke level 6 agar seimbang antara kecepatan dan efisiensi ukuran.

## Kompatibilitas

- Android minimum: Android 7.0 (API 24).
- Target kompilasi: Android API 35.
- ABI native: `arm64-v8a`, `armeabi-v7a`, dan `x86_64`.
- Android NDK: `27.2.12479018`.
- CMake: `3.22.1`.
- JDK: `17`.
- Android Gradle Plugin: `8.7.3`.
- Gradle: `8.9`.

Arsip sumber yang diberikan menggunakan versi libbpg/x265 yang relatif lama. Penghubung JNI dan konfigurasi CMake sudah disiapkan, tetapi APK Android tetap perlu diverifikasi melalui Android SDK/NDK. Lingkungan pembuatan paket sumber ini tidak memiliki Android SDK/NDK, sehingga **APK belum disertakan**.

## Membuat APK melalui GitHub Actions

1. Unggah seluruh isi folder proyek ini ke sebuah repositori GitHub.
2. Buka tab **Actions** pada repositori.
3. Jalankan workflow **Build Android APK** (atau lakukan push ke branch `main`/`master`).
4. Setelah workflow selesai, buka hasil proses tersebut.
5. Unduh artifact bernama `bpg-image-compressor-debug`, lalu ekstrak `app-debug.apk`.

Workflow akan memasang JDK 17, Android SDK Platform 35, NDK 27.2.12479018, dan CMake 3.22.1 secara otomatis.

## Build lokal

Buka proyek melalui Android Studio dengan Android SDK 35, NDK 27.2.12479018, dan CMake 3.22.1 terpasang. Jalankan task Gradle `assembleDebug`.

Jika Gradle Wrapper belum tersedia, buat wrapper Gradle 8.9:

```bash
gradle wrapper --gradle-version 8.9
```

Kemudian jalankan:

```bash
./gradlew assembleDebug
```

APK debug akan berada di:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Catatan format BPG

BPG bukan JPEG atau PNG. Sebagian besar aplikasi Galeri Android belum mendukung pembacaan `.bpg`, sehingga file hasil mungkin tidak bisa ditampilkan langsung di Galeri. Aplikasi ini menampilkan pratinjau gambar sumber dan ukuran file hasil; pratinjau tersebut bukan hasil decode BPG. Untuk membuka hasilnya, gunakan penampil/decoder yang mendukung BPG.

## Implementasi native

- `bpgenc_android.c`: penghubung JNI Android dan integrasi encoder BPG.
- `x265_glue.c`: adapter dari antarmuka encoder libbpg ke x265.
- `third_party/libbpg`: sumber encoder dan header libbpg yang diperlukan.
- `third_party/x265`: sumber x265 yang dibangun sebagai pustaka statis.

JNI menerima piksel ARGB dari API `Bitmap` Android, menyiapkan plane gambar, lalu memanggil encoder BPG secara langsung. Tidak ada proses command-line eksternal.

## Lisensi

Build ini menautkan x265 secara statis. Sumber x265 yang disertakan menyatakan bahwa x265 tersedia di bawah **GNU GPL versi 2** atau lisensi komersial terpisah. Berkas encoder libbpg memiliki pemberitahuan lisensinya sendiri di header sumber.

Karena aplikasi menautkan x265, distribusi aplikasi gabungan tunduk pada kewajiban GPL x265 yang berlaku, kecuali lisensi komersial x265 yang sesuai diperoleh. Pertahankan kode sumber, pemberitahuan hak cipta, dan teks lisensi saat mendistribusikan build. Berkas `COPYING` x265 dan pemberitahuan sumber disertakan dalam proyek ini.

Informasi ini bukan nasihat hukum. Periksa seluruh berkas lisensi sebelum menerbitkan atau menjual aplikasi.
