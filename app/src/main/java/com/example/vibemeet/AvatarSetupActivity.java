package com.example.vibemeet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.vibemeet.services.AvatarService;
import com.example.vibemeet.services.UserPreferencesService;
import com.example.vibemeet.views.AnimatedAvatarView;

import java.io.File;

/**
 * Avatar setup screen:
 *  - Take a selfie via system camera
 *  - Pick mood (changes the floating particles)
 *  - Live-preview the animated avatar
 *  - Save to internal storage
 */
public class AvatarSetupActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 50;
    private static final String[] MOODS = {"happy", "chill", "romantic", "adventure", "party", "coffee", "foodie"};

    private AnimatedAvatarView avatarView;
    private Button btnTakePhoto, btnPickGallery, btnSave;
    private Spinner moodSpinner;
    private TextView statusText;
    private ImageButton btnBack;

    private AvatarService avatarService;
    private UserPreferencesService prefs;

    private Uri pendingCameraUri;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_setup);

        avatarService = new AvatarService(this);
        prefs = new UserPreferencesService(this);

        avatarView = findViewById(R.id.avatarView);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnPickGallery = findViewById(R.id.btnPickGallery);
        btnSave = findViewById(R.id.btnSave);
        moodSpinner = findViewById(R.id.moodSpinner);
        statusText = findViewById(R.id.statusText);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Load existing avatar (if any)
        Bitmap existing = avatarService.loadAvatar();
        if (existing != null) {
            avatarView.setAvatarBitmap(existing);
            statusText.setText("Tap below to take a new photo or change your mood.");
        } else {
            statusText.setText("Take a selfie to create your animated avatar.");
        }

        // Apply existing mood
        String savedMood = prefs.getCurrentMood();
        avatarView.setMood(savedMood);
        avatarView.startAnimating();

        setupMoodSpinner(savedMood);
        setupLaunchers();

        btnTakePhoto.setOnClickListener(v -> launchCamera());
        btnPickGallery.setOnClickListener(v -> launchGallery());
        btnSave.setOnClickListener(v -> {
            // Save current mood
            prefs.setCurrentMood((String) moodSpinner.getSelectedItem());
            Toast.makeText(this, "Avatar saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void setupMoodSpinner(String selectedMood) {
        String[] displayMoods = new String[MOODS.length];
        for (int i = 0; i < MOODS.length; i++) {
            displayMoods[i] = capitalizeMood(MOODS[i]);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, displayMoods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        moodSpinner.setAdapter(adapter);

        // Select existing mood
        for (int i = 0; i < MOODS.length; i++) {
            if (MOODS[i].equals(selectedMood)) {
                moodSpinner.setSelection(i);
                break;
            }
        }

        moodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String mood = MOODS[position];
                avatarView.setMood(mood);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private String capitalizeMood(String mood) {
        return mood.substring(0, 1).toUpperCase() + mood.substring(1);
    }

    private void setupLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && pendingCameraUri != null) {
                        processNewPhoto(pendingCameraUri);
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        processNewPhoto(uri);
                    }
                });
    }

    private void launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            return;
        }

        try {
            File photoFile = File.createTempFile("avatar_capture_", ".jpg", getCacheDir());
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

    private void processNewPhoto(Uri uri) {
        boolean saved = avatarService.saveAvatarFromUri(uri);
        if (saved) {
            Bitmap newBitmap = avatarService.loadAvatar();
            avatarView.setAvatarBitmap(newBitmap);
            statusText.setText("Looking great. Tap Save to confirm.");
            Toast.makeText(this, "Photo applied!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Couldn't process photo", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission needed to take a selfie", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
