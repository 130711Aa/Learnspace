# Rencana Fitur & Arsitektur: Math 3D Lab (Lernspace)

Dokumen ini berisi spesifikasi konsep, analisis matematika, hasil *reverse engineering* algoritma GeoGebra, dan arsitektur teknis untuk halaman mandiri **Math 3D Lab** di Lernspace.

---

## 1. Latar Belakang & Konsep Utama

* **Pemisahan Modul**: 
  Kanvas catatan (*Notes Editor*) tetap menggunakan *Floating Graph Window* untuk kalkulasi dan grafik 2D cepat agar performa catatan tetap ringan. Fitur kalkulus tingkat lanjut dan objek 3D ditempatkan di halaman tersendiri (*Dedicated Workspace*).
* **Kebutuhan Ruang & Kontrol Sentuh**:
  Visualisasi 3D membutuhkan area pandang luas untuk manipulasi kamera (orbit rotasi 360°, pan, dan pinch-to-zoom dengan jari/stylus) serta panel parameter yang terstruktur.
* **Integrasi Lembar Catatan (*Export to Notes*)**:
  Pengguna dapat melakukan eksperimen matematika di 3D Lab, lalu menekan tombol **"Tempel Snapshot ke Catatan"** untuk melampirkan tangkapan layar 3D resolusi tinggi langsung ke catatan siswa.

---

## 2. Modul Fitur yang Direncanakan

### A. Kalkulasi Benda Putar (*Solids of Revolution*)
1. **Input Parameter**:
   * Fungsi kurva: $y = f(x)$ atau $x = g(y)$.
   * Batas interval integral: $[a, b]$.
   * Pilihan sumbu putar: Sumbu-X atau Sumbu-Y.
   * Metode analisis: Cakram (*Disk*), Cincin (*Washer*), atau Kulit Tabung (*Cylindrical Shell*).
2. **Visualisasi Interaktif 3D**:
   * Render mesh 3D padat/transparan dengan pencahayaan (*specular lighting*).
   * **Slider Animasi Rotasi ($0^\circ \to 360^\circ$)**: Memungkinkan siswa melihat proses dinamis kurva 2D yang berputar mengelilingi sumbu hingga membentuk objek 3D utuh.
   * Cross-section cutaway (irisan melintang) untuk melihat rongga di dalam benda putar.
3. **Kalkulasi & Langkah Pengerjaan**:
   * Menampilkan rumus integral volume:
     $$V = \pi \int_{a}^{b} [f(x)]^2 \, dx$$
   * Perhitungan hasil numerik/eksak instan beserta langkah integrasi (*step-by-step*).

### B. Vektor 3 Dimensi ($\mathbb{R}^3$)
1. **Input Vektor**:
   * Input komponen vektor $\vec{u} = \langle x_1, y_1, z_1 \rangle$ dan $\vec{v} = \langle x_2, y_2, z_2 \rangle$.
2. **Visualisasi Grid 3D**:
   * Grid koordinat kartesius 3D dengan sumbu berlabel warna: Sumbu-X (Merah), Sumbu-Y (Hijau), Sumbu-Z (Biru).
   * Panah vektor 3D yang jelas (mesh gabungan silinder untuk batang dan kerucut untuk kepala panah).
3. **Operasi & Makna Geometris**:
   * **Magnitudo / Panjang**: $|\vec{u}| = \sqrt{x_1^2 + y_1^2 + z_1^2}$.
   * **Sudut Antar Vektor**: $\cos(\theta) = \frac{\vec{u} \cdot \vec{v}}{|\vec{u}||\vec{v}|}$.
   * **Dot Product**: $\vec{u} \cdot \vec{v} = x_1 x_2 + y_1 y_2 + z_1 z_2$.
   * **Cross Product**: $\vec{u} \times \vec{v} = \langle u_y v_z - u_z v_y, \; u_z v_x - u_x v_z, \; u_x v_y - u_y v_x \rangle$.
   * **Visualisasi Jajar Genjang**: Menampilkan bidang segi empat berbayang di antara $\vec{u}$ dan $\vec{v}$ untuk memvisualisasikan luas determinan/cross product secara geometris.

### C. Permukaan 3D & Kalkulus Multivariabel (Fase Lanjutan)
* Plot permukaan fungsi dua peubah $z = f(x, y)$ (misal paraboloid, elipsoid, pelana kuda).

---

## 3. Hasil *Reverse Engineering* Algoritma GeoGebra

Mesin GeoGebra menghasilkan objek 3D menggunakan prinsip matematika dan triangulasi WebGL yang efisien:

### 1. Pembentukan Mesh Parametrik Benda Putar
GeoGebra mentransformasikan fungsi 2D $y = f(x)$ menjadi koordinat bola/silinder parametrik:
$$\begin{aligned}
X(u, v) &= u \\
Y(u, v) &= f(u) \cdot \cos(v) \\
Z(u, v) &= f(u) \cdot \sin(v)
\end{aligned}$$
* $u \in [a, b]$ dibagi menjadi $N$ subdivisi (misal $N = 50$).
* Sudut rotasi $v \in [0, \theta]$ dibagi menjadi $M$ sudut lingkaran (misal $M = 60$).

**Triangulasi Segitiga WebGL**:
Setiap petak kuadran $(u_i, v_j)$ dibagi menjadi 2 segitiga:
* $T_1 = [P(i, j), \; P(i+1, j), \; P(i, j+1)]$
* $T_2 = [P(i+1, j), \; P(i+1, j+1), \; P(i, j+1)]$
Di kedua ujung interval ($u = a$ dan $u = b$), ditambahkan mesh cakram penutup (*cap disks*).

### 2. Integrasi Numerik Volume
GeoGebra menghitung volume fungsi non-trivial menggunakan metode **Adaptive Simpson's Rule** atau **Gauss-Legendre Quadrature**:
$$V \approx \pi \sum_{k=1}^{n} w_k [f(x_k)]^2$$
Algoritma ini memberikan hasil volume instan dalam orde milidetik tanpa membebani CPU tablet.

### 3. Kontrol Kamera Orbit 3D
Transformasi kamera menggunakan koordinat sferis (*Spherical Coordinates* / Quaternions) yang merespons event sentuh layar tablet:
* Geser 1 jari $\to$ rotasi sudut azimuth ($\theta$) dan elevasi ($\phi$).
* Pinch 2 jari $\to$ manipulasi jarak radius kamera ($zoom$).

---

## 4. Arsitektur Teknis Implementasi di Lernspace

1. **Rendering Engine**:
   * Berbasis **HTML5 Canvas / Three.js** di dalam `WebView` yang *Hardware-Accelerated*.
   * Berjalan **100% offline** dengan menyimpan pustaka Three.js dan template HTML di direktori lokal `app/src/main/assets/math3d/`.
2. **Jembatan Komunikasi Dua Arah (Android $\leftrightarrow$ JavaScript)**:
   * **Android (Kotlin) $\to$ JS**: Mengirim formula fungsi matematika, nilai batas $[a, b]$, vektor, dan sudut rotasi via `webView.evaluateJavascript()`.
   * **JS $\to$ Android (Kotlin)**: Meneruskan tangkapan layar WebGL (Base64 bitmap) via `JavascriptInterface` untuk langsung ditempelkan ke kanvas catatan siswa.
3. **Navigasi Aplikasi**:
   * Ditambahkan sebagai menu mandiri di navigasi utama atau terintegrasi di dalam ekosistem **Mimo Lab**.
   * Disediakan tombol shortcut *"Buka di 3D Lab"* pada *Floating Graph Window* di dalam catatan.
