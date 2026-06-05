# 📝 SmartScanner - AI Powered Document Scanner

SmartScanner is a high-performance Android application designed to bridge the gap between physical documents and digital data. Leveraging Google's ML Kit OCR and CameraX API, it allows users to capture images, extract text in real-time, and export them into professional PDF documents.

---

## 🚀 Key Features
- **Smart AI Scanning:** Utilizes Google ML Kit's Text Recognition (OCR) to extract text from images with high accuracy.
- **Real-time Camera Integration:** Built on Android Jetpack's CameraX for a smooth and responsive camera preview.
- **PDF Generation:** Instantly convert extracted text into professional PDF files stored directly in the device's Documents folder.
- **Scan History:** Maintain a local history of all previous scans using Gson and SharedPreferences for quick access.
- **Modern UI/UX:** Clean, intuitive interface with real-time feedback and custom permission handling.
- **Scoped Storage:** Fully compatible with Android 10+ (API 29+) storage policies.

---

## 🛠️ Tech Stack & Libraries
| Technology | Purpose |
|---|---|
| Java | Primary Language |
| Jetpack CameraX | Camera Control & Image Capture |
| Google ML Kit | OCR Text Recognition |
| iText7 | PDF Generation |
| Gson | Data Serialization for History |
| Material Design | Modern UI/UX |

---

## 📸 Screenshots
| Home Screen | OCR Processing | PDF Preview | History |
|---|---|---|---|
| Coming Soon | Coming Soon | Coming Soon | Coming Soon |

---

## ⚙️ Installation

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/MariaZafar22/SmartScanner2.git
   ```
2. **Open in Android Studio:**
   - File > Open > Select SmartScanner folder
3. **Sync Gradle:**
   - Wait for all dependencies to download (ML Kit, iText, CameraX)
4. **Run:**
   - Connect your device and click Run 'app'

---

## 📂 Project Structure
```
com.example.smartscanner
 ┣ 📂 activities
 ┃ ┗ 📜 HistoryActivity.java   # Manages saved scan logs
 ┣ 📂 models
 ┃ ┗ 📜 ScanModel.java         # Data model for scans
 ┣ 📂 utils
 ┃ ┗ 📜 HistoryManager.java    # Local storage logic (Gson)
 ┗ 📜 MainActivity.java        # Core Logic (Camera + OCR + PDF)
```

---

## 🛡️ Permissions Required
- **CAMERA:** To capture document images.
- **WRITE_EXTERNAL_STORAGE** (API < 29): To save PDF files locally.

---

## 🤝 Contributing
Contributions are welcome! If you have ideas for features (like Cloud Sync or Image Filtering), feel free to:

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request
