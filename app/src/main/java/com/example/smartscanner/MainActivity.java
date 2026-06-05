package com.example.smartscanner;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.smartscanner.activities.HistoryActivity;
import com.example.smartscanner.models.ScanModel;
import com.example.smartscanner.utils.HistoryManager;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SmartScannerPro";
    private static final int CAMERA_REQ_CODE = 101;
    private static final int STORAGE_REQ_CODE = 102;

    private PreviewView previewView;
    private EditText etResult;
    private ImageCapture imageCapture;
    private TextRecognizer textRecognizer;
    private LanguageIdentifier languageIdentifier;
    private Camera camera;
    private TextToSpeech tts;
    private boolean isTtsReady = false;
    private Button btnCapture;
    private Translator translator;
    private String currentDetectedLang = "en";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Binding
        previewView = findViewById(R.id.previewView);
        etResult = findViewById(R.id.tvResult);
        btnCapture = findViewById(R.id.btnCapture);
        Button btnListen = findViewById(R.id.btnListen);
        Button btnTranslate = findViewById(R.id.btnScan); 
        Button btnSave = findViewById(R.id.btnSave);
        Button btnHistory = findViewById(R.id.btnHistory);

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        languageIdentifier = LanguageIdentification.getClient();

        // Optimized Audio Initialization
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build();
                tts.setAudioAttributes(attributes);
                tts.setPitch(1.0f);
                tts.setSpeechRate(0.9f); // Slightly slower for clarity
                isTtsReady = true;
            }
        });

        requestCameraPermission();

        btnCapture.setOnClickListener(v -> captureAndExtractText());

        btnListen.setOnClickListener(v -> {
            String text = etResult.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Pehle scan karein!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isTtsReady) {
                if (tts.isSpeaking()) {
                    tts.stop();
                } else {
                    playAudioIntelligently(text);
                }
            } else {
                Toast.makeText(this, "Audio system taiyar nahi hai.", Toast.LENGTH_SHORT).show();
            }
        });

        btnTranslate.setOnClickListener(v -> {
            String text = etResult.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Pehle kuch scan karein!", Toast.LENGTH_SHORT).show();
            } else {
                detectAndTranslateToUrdu(text);
            }
        });

        btnSave.setOnClickListener(v -> showSaveDialog());
        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));

        setupAdvancedGestures();
    }

    private void playAudioIntelligently(String text) {
        Locale locale = Locale.US;
        if (currentDetectedLang.equals("ur")) locale = new Locale("ur", "PK");
        else if (currentDetectedLang.equals("hi")) locale = new Locale("hi", "IN");

        tts.setLanguage(locale);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ScannerTTS");
        Toast.makeText(this, "🔊 Playing... (Volume full rakhein)", Toast.LENGTH_SHORT).show();
    }

    private void detectAndTranslateToUrdu(String text) {
        Toast.makeText(this, "🌐 Pehchana ja raha hai...", Toast.LENGTH_SHORT).show();
        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener(languageCode -> {
                    currentDetectedLang = languageCode;
                    String sourceLang = languageCode.equals("und") ? TranslateLanguage.ENGLISH : languageCode;
                    startTranslation(text, sourceLang);
                })
                .addOnFailureListener(e -> startTranslation(text, TranslateLanguage.ENGLISH));
    }

    private void startTranslation(String text, String sourceLang) {
        if (translator != null) translator.close();

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(TranslateLanguage.URDU)
                .build();
        
        translator = Translation.getClient(options);
        DownloadConditions conditions = new DownloadConditions.Builder().build();

        Toast.makeText(this, "📥 Model Download ho raha hai (Pehli baar waqt lagta hai)...", Toast.LENGTH_LONG).show();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    translator.translate(text)
                            .addOnSuccessListener(translatedText -> {
                                etResult.setText(translatedText);
                                currentDetectedLang = "ur"; // Update for audio
                                Toast.makeText(this, "Tarjuma ho gaya!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Translation failed", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Internet Check karein", Toast.LENGTH_LONG).show());
    }

    private void setupAdvancedGestures() {
        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (camera != null) {
                    float currentZoom = camera.getCameraInfo().getZoomState().getValue().getZoomRatio();
                    camera.getCameraControl().setZoomRatio(currentZoom * detector.getScaleFactor());
                }
                return true;
            }
        });

        previewView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.performClick();
                if (camera != null) {
                    MeteringPoint point = previewView.getMeteringPointFactory().createPoint(event.getX(), event.getY());
                    FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                            .setAutoCancelDuration(3, TimeUnit.SECONDS).build();
                    camera.getCameraControl().startFocusAndMetering(action);
                    return true;
                }
            }
            return true;
        });
    }

    private void captureAndExtractText() {
        if (imageCapture == null) return;
        btnCapture.setEnabled(false);
        etResult.setText("AI Intelligence Processing...");

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                processWithIntelligence(imageProxy);
            }
            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                btnCapture.setEnabled(true);
                Toast.makeText(MainActivity.this, "Capture Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processWithIntelligence(ImageProxy imageProxy) {
        ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        if (bitmap != null) {
            Bitmap enhanced = applyIntelligenceFilter(bitmap);
            InputImage inputImage = InputImage.fromBitmap(enhanced, imageProxy.getImageInfo().getRotationDegrees());

            textRecognizer.process(inputImage)
                    .addOnSuccessListener(text -> {
                        btnCapture.setEnabled(true);
                        imageProxy.close();
                        if (text.getText().isEmpty()) {
                            Toast.makeText(this, "Low Accuracy: Focus behtar karein.", Toast.LENGTH_LONG).show();
                        } else {
                            etResult.setText(text.getText());
                            copyToClipboard(text.getText());
                            // Pre-detect language for zero lag audio
                            languageIdentifier.identifyLanguage(text.getText())
                                    .addOnSuccessListener(lang -> currentDetectedLang = lang);
                        }
                    })
                    .addOnFailureListener(e -> {
                        btnCapture.setEnabled(true);
                        imageProxy.close();
                    });
        } else {
            imageProxy.close();
            btnCapture.setEnabled(true);
        }
    }

    private Bitmap applyIntelligenceFilter(Bitmap src) {
        Bitmap dest = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        Canvas canvas = new Canvas(dest);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0); 
        float contrast = 1.6f;
        float brightness = -60f;
        cm.set(new float[] { contrast,0,0,0,brightness, 0,contrast,0,0,brightness, 0,0,contrast,0,brightness, 0,0,0,1,0 });
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(src, 0, 0, paint);
        return dest;
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Scanned", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Auto-Copied to Clipboard!", Toast.LENGTH_SHORT).show();
    }

    private void showSaveDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Save as PDF")
                .setMessage("Save documents to phone?")
                .setPositiveButton("Allow", (d, w) -> checkStorageAndSave())
                .setNegativeButton("Cancel", null).show();
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQ_CODE);
        } else {
            startCamera();
        }
    }

    private void checkStorageAndSave() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQ_CODE);
                return;
            }
        }
        performSave();
    }

    private void performSave() {
        String res = etResult.getText().toString().trim();
        if (res.isEmpty() || res.contains("AI Intelligence Processing")) return;
        HistoryManager.saveScan(this, new ScanModel(res, new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date())));
        createPdfInDocuments(res);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                        .build();
                camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void createPdfInDocuments(String content) {
        String fileName = "SmartScanAI_" + System.currentTimeMillis() + ".pdf";
        try {
            OutputStream out = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                v.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);
                Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), v);
                if (uri != null) out = getContentResolver().openOutputStream(uri);
            } else {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName);
                out = new FileOutputStream(file);
            }
            if (out != null) {
                PdfWriter writer = new PdfWriter(out);
                PdfDocument pdf = new PdfDocument(writer);
                new Document(pdf).add(new Paragraph("Scanner AI Output:\n\n" + content)).close();
                Toast.makeText(this, "PDF Saved to Documents!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error saving PDF", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == CAMERA_REQ_CODE) startCamera();
            if (requestCode == STORAGE_REQ_CODE) performSave();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (languageIdentifier != null) {
            languageIdentifier.close();
        }
        if (translator != null) {
            translator.close();
        }
    }
}