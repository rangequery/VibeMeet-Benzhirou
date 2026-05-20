package com.example.vibemeet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.vibemeet.services.AvatarSdkService;
import com.example.vibemeet.services.UserPreferencesService;

import java.io.File;

/**
 * Creates a personal 3D head avatar via Avatar SDK.
 *
 * Flow:
 *   1. User opens this screen
 *   2. Taps "📸 Take Selfie" → system camera opens
 *   3. Returns with the photo
 *   4. Taps "✨ Generate 3D Twin" → uploads to Avatar SDK
 *   5. Shows progress for ~1-2 minutes
 *   6. On success, GLB is saved locally and the path stored in prefs
 *   7. Digital Twin screen can now use the personalized avatar
 */
public class Avatar3DCreatorActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private ImageView photoPreview;
    private TextView photoHint;
    private TextView statusText;
    private Button btnTakeSelfie;
    private Button btnPickGallery;
    private Button btnGenerate;
    private ProgressBar progressBar;
    private TextView progressPercent;
    private View progressSection;
    private ImageButton btnBack;

    private AvatarSdkService avatarSdkService;
    private UserPreferencesService prefs;
    private File pendingPhoto;
    private Uri pendingCameraUri;
    private Button btnImportGlb;

    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String[]> glbPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_3d_creator);

        avatarSdkService = new AvatarSdkService(this);
        prefs = new UserPreferencesService(this);

        photoPreview = findViewById(R.id.photoPreview);
        photoHint = findViewById(R.id.photoHint);
        statusText = findViewById(R.id.statusText);
        btnTakeSelfie = findViewById(R.id.btnTakeSelfie);
        btnPickGallery = findViewById(R.id.btnPickGallery);
        btnGenerate = findViewById(R.id.btnGenerate);
        progressBar = findViewById(R.id.progressBar);
        progressPercent = findViewById(R.id.progressPercent);
        progressSection = findViewById(R.id.progressSection);
        btnBack = findViewById(R.id.btnBack);
        btnImportGlb = findViewById(R.id.btnImportGlb);

        btnBack.setOnClickListener(v -> finish());

        setupLaunchers();

        btnImportGlb.setOnClickListener(v -> launchGlbPicker());
        btnTakeSelfie.setOnClickListener(v -> launchCamera());
        btnPickGallery.setOnClickListener(v -> launchGallery());
        btnGenerate.setOnClickListener(v -> generateAvatar());

        btnGenerate.setEnabled(false);
        btnGenerate.setAlpha(0.4f);
    }

    private void setupLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && pendingCameraUri != null) {
                        File f = uriToFile(pendingCameraUri);
                        if (f != null && f.exists()) {
                            onPhotoChosen(f);
                        }
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        File f = copyUriToCache(uri);
                        if (f != null) {
                            onPhotoChosen(f);
                        }
                    }
                });

        // .glb file picker — accepts any binary file (Android's MIME types don't include .glb)
        glbPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        importGlbFile(uri);
                    }
                });
    }

    private void launchGlbPicker() {
        try {
            // .glb files use model/gltf-binary mime type, but it's often unrecognized.
            // Accept all binary types and let the user pick.
            glbPickerLauncher.launch(new String[]{
                    "model/gltf-binary",
                    "model/gltf+json",
                    "application/octet-stream",
                    "*/*"
            });
        } catch (Exception e) {
            Toast.makeText(this, "Could not open file picker: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void importGlbFile(Uri uri) {
        progressSection.setVisibility(View.VISIBLE);
        statusText.setText("Importing 3D avatar…");
        progressBar.setProgress(20);
        progressPercent.setText("20%");

        // Get the original filename
        String name = "imported.glb";
        try {
            android.database.Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        int nameIdx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                        if (nameIdx >= 0) name = c.getString(nameIdx);
                    }
                } finally {
                    c.close();
                }
            }
        } catch (Exception ignored) {}

        // Ensure .glb extension
        if (!name.toLowerCase().endsWith(".glb")) {
            name = name.replaceAll("\\.[^.]*$", "") + ".glb";
        }
        // Sanitize
        final String finalName = "twin_" + System.currentTimeMillis() + "_" + name.replaceAll("[^a-zA-Z0-9._-]", "_");

        new Thread(() -> {
            try {
                File outFile = new File(getFilesDir(), finalName);

                try (java.io.InputStream is = getContentResolver().openInputStream(uri);
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile)) {
                    if (is == null) throw new Exception("Cannot read file");
                    byte[] buf = new byte[8192];
                    int n;
                    long total = 0;
                    while ((n = is.read(buf)) != -1) {
                        fos.write(buf, 0, n);
                        total += n;
                        final long shown = total;
                        runOnUiThread(() -> statusText.setText("Importing… " + (shown / 1024) + " KB"));
                    }
                }

                if (outFile.length() < 100) {
                    throw new Exception("File too small to be a valid GLB");
                }

                // Verify it's a GLB by checking magic bytes
                try (java.io.FileInputStream fis = new java.io.FileInputStream(outFile)) {
                    byte[] header = new byte[4];
                    if (fis.read(header) != 4 || header[0] != 'g' || header[1] != 'l' ||
                            header[2] != 'T' || header[3] != 'F') {
                        outFile.delete();
                        throw new Exception("Not a valid .glb file");
                    }
                }

                long sizeKb = outFile.length() / 1024;
                final String finalPath = outFile.getAbsolutePath();
                runOnUiThread(() -> {
                    progressBar.setProgress(100);
                    progressPercent.setText("100%");
                    statusText.setText("✓ Imported " + sizeKb + " KB");
                    prefs.setAvatar3dGlbPath(finalPath);
                    Toast.makeText(Avatar3DCreatorActivity.this,
                            "✓ Your 3D twin is ready!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Avatar3DCreatorActivity.this, DigitalTwinActivity.class);
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("⚠️ Import failed: " + e.getMessage());
                    progressBar.setProgress(0);
                    progressPercent.setText("0%");
                    Toast.makeText(Avatar3DCreatorActivity.this,
                            "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }, "GlbImporter").start();
    }

    private File uriToFile(Uri uri) {
        try {
            // The camera saved directly to our cache file
            String path = uri.getPath();
            if (path != null) {
                // Find by file name
                File[] files = getCacheDir().listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (path.endsWith(f.getName())) return f;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private File copyUriToCache(Uri uri) {
        try {
            File outFile = File.createTempFile("twin_input_", ".jpg", getCacheDir());
            try (java.io.InputStream is = getContentResolver().openInputStream(uri);
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile)) {
                byte[] buf = new byte[8192];
                int n;
                while (is != null && (n = is.read(buf)) != -1) {
                    fos.write(buf, 0, n);
                }
            }
            return outFile;
        } catch (Exception e) {
            return null;
        }
    }

    private void launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            return;
        }
        try {
            File photoFile = File.createTempFile("twin_capture_", ".jpg", getCacheDir());
            pendingCameraUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile);
            cameraLauncher.launch(pendingCameraUri);
        } catch (Exception e) {
            Toast.makeText(this, "Camera unavailable: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGallery() {
        galleryLauncher.launch("image/*");
    }

    private void onPhotoChosen(File file) {
        pendingPhoto = file;
        Glide.with(this).load(file).centerCrop().into(photoPreview);
        photoHint.setText("Photo ready · Tap below to generate");
        btnGenerate.setEnabled(true);
        btnGenerate.setAlpha(1f);
        statusText.setText("");
        progressSection.setVisibility(View.GONE);
    }

    private void generateAvatar() {
        if (pendingPhoto == null) {
            Toast.makeText(this, "Take a selfie first", Toast.LENGTH_SHORT).show();
            return;
        }

        progressSection.setVisibility(View.VISIBLE);
        btnGenerate.setEnabled(false);
        btnGenerate.setAlpha(0.4f);
        btnTakeSelfie.setEnabled(false);
        btnPickGallery.setEnabled(false);
        statusText.setText("Starting…");

        avatarSdkService.generateAvatar(pendingPhoto, new AvatarSdkService.Listener() {
            @Override
            public void onProgress(int percent, String message) {
                runOnUiThread(() -> {
                    progressBar.setProgress(percent);
                    progressPercent.setText(percent + "%");
                    statusText.setText(message);
                });
            }

            @Override
            public void onSuccess(File glbFile) {
                runOnUiThread(() -> {
                    prefs.setAvatar3dGlbPath(glbFile.getAbsolutePath());
                    Toast.makeText(Avatar3DCreatorActivity.this,
                            "✓ Your 3D twin is ready!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Avatar3DCreatorActivity.this, DigitalTwinActivity.class);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    statusText.setText("⚠️ " + error);
                    btnGenerate.setEnabled(true);
                    btnGenerate.setAlpha(1f);
                    btnTakeSelfie.setEnabled(true);
                    btnPickGallery.setEnabled(true);
                    Toast.makeText(Avatar3DCreatorActivity.this,
                            "Generation failed — try a clearer selfie", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        }
    }
}
