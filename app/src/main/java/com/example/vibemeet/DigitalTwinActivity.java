package com.example.vibemeet;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebViewAssetLoader;

import com.example.vibemeet.models.BlendshapeState;
import com.example.vibemeet.services.FaceLandmarkerHelper;
import com.example.vibemeet.services.UserPreferencesService;
import com.example.vibemeet.views.Face3DMeshView;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Personal Real-Time 3D Digital Twin (MVP5).
 *
 * Architecture:
 *   1. CameraX provides a live front-camera feed.
 *   2. MediaPipe Face Landmarker (Google AI, on-device) extracts 52 ARKit
 *      blendshape coefficients per frame (smile, jawOpen, eyeBlink, etc).
 *   3. A 3D avatar (Ready Player Me .glb) rendered in a WebView with Three.js
 *      applies those blendshapes to morph targets in real time.
 */
public class DigitalTwinActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final long MIN_UPDATE_INTERVAL_MS = 33; // ~30 Hz

    private PreviewView cameraPreview;
    private WebView avatarWebView;
    private TextView statusLabel, expressionLabel;
    private ImageButton btnBack;

    private FaceLandmarkerHelper landmarkerHelper;
    private ExecutorService analysisExecutor;
    private ProcessCameraProvider cameraProvider;
    private boolean isFrontCamera = true;
    private boolean avatarReady = false;
    private long lastWebViewUpdate = 0;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digital_twin);

        cameraPreview = findViewById(R.id.cameraPreview);
        avatarWebView = findViewById(R.id.avatarWebView);
        statusLabel = findViewById(R.id.statusLabel);
        expressionLabel = findViewById(R.id.expressionLabel);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        setupWebView();

        analysisExecutor = Executors.newSingleThreadExecutor();
        landmarkerHelper = new FaceLandmarkerHelper(this);
        landmarkerHelper.setListener(landmarkerListener);
        analysisExecutor.execute(() -> landmarkerHelper.setup());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private void setupWebView() {
        avatarWebView.setBackgroundColor(0x00000000);
        android.webkit.WebSettings s = avatarWebView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        // Always load fresh content (don't cache the HTML/JS)
        s.setCacheMode(android.webkit.WebSettings.LOAD_NO_CACHE);
        avatarWebView.clearCache(true);
        avatarWebView.clearHistory();

        // Use WebViewAssetLoader to serve assets via a virtual https:// URL.
        // This allows fetch(), modules, and CORS to work correctly.
        // /assets/ → bundled APK assets (HTML, JS, fallback GLB)
        // /twins/  → user's generated Avatar SDK twin (in app's filesDir)
        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .addPathHandler("/twins/", new WebViewAssetLoader.InternalStoragePathHandler(
                        this, getFilesDir()))
                .build();

        avatarWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage cm) {
                android.util.Log.d("AvatarWebView",
                        cm.message() + " @ " + cm.sourceId() + ":" + cm.lineNumber());
                return true;
            }
        });

        avatarWebView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                statusLabel.setText("3D avatar loading…");
            }
        });

        avatarWebView.addJavascriptInterface(new AvatarBridge(), "AndroidBridge");
        // Append timestamp to bypass any cached version
        avatarWebView.loadUrl("https://appassets.androidplatform.net/assets/avatar.html?v=" + System.currentTimeMillis());
    }

    private class AvatarBridge {
        @JavascriptInterface
        public void onAvatarReady() {
            runOnUiThread(() -> {
                avatarReady = true;
                statusLabel.setText("✓ 3D twin live · MediaPipe AI");
            });
        }

        /** Called by avatar.html to find out which GLB to load.
         *  Returns the user's personalized Avatar SDK twin URL (served via WebViewAssetLoader),
         *  or empty string to use the bundled facecap.glb fallback. */
        @JavascriptInterface
        public String getAvatarGlbUrl() {
            UserPreferencesService prefs = new UserPreferencesService(DigitalTwinActivity.this);
            if (prefs.has3dAvatar()) {
                // Serve the local GLB via the virtual /twins/ path set up in setupWebView()
                java.io.File f = new java.io.File(prefs.getAvatar3dGlbPath());
                return "https://appassets.androidplatform.net/twins/" + f.getName();
            }
            return "";
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraFuture =
                ProcessCameraProvider.getInstance(this);
        cameraFuture.addListener(() -> {
            try {
                cameraProvider = cameraFuture.get();
                bindCameraUseCases();
            } catch (Exception e) {
                Toast.makeText(this, "Camera failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build();

        analysis.setAnalyzer(analysisExecutor, this::analyzeFrame);

        CameraSelector cameraSelector;
        try {
            if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                isFrontCamera = true;
            } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                isFrontCamera = false;
                cameraPreview.setScaleX(1f);
            } else {
                Toast.makeText(this, "No camera available", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis);
        } catch (Exception e) {
            Toast.makeText(this, "Could not start camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void analyzeFrame(@NonNull ImageProxy imageProxy) {
        if (landmarkerHelper == null) {
            imageProxy.close();
            return;
        }
        landmarkerHelper.detectLiveStream(imageProxy, isFrontCamera);
    }

    private final FaceLandmarkerHelper.Listener landmarkerListener = new FaceLandmarkerHelper.Listener() {
        @Override
        public void onResult(List<Face3DMeshView.Landmark> landmarks,
                             BlendshapeState b,
                             long inferenceMs) {
            long now = System.currentTimeMillis();
            if (now - lastWebViewUpdate < MIN_UPDATE_INTERVAL_MS) return;
            lastWebViewUpdate = now;

            runOnUiThread(() -> {
                if (avatarReady) sendBlendshapesToAvatar(b);
                expressionLabel.setText(describeExpression(b));
            });
        }

        @Override
        public void onNoFaceDetected() {
            runOnUiThread(() -> {
                expressionLabel.setText("Looking for face...");
                if (avatarReady) avatarWebView.evaluateJavascript("window.resetFace();", null);
            });
        }

        @Override
        public void onError(String error) {
            runOnUiThread(() -> statusLabel.setText(error));
        }
    };

    /** Build a JS object literal and send it to the avatar in the WebView. */
    private void sendBlendshapesToAvatar(BlendshapeState b) {
        if (b == null || !b.faceDetected) return;

        String js = String.format(Locale.US,
                "window.applyBlendshapes({" +
                        "faceDetected:true," +
                        "jawOpen:%.3f," +
                        "mouthSmileLeft:%.3f,mouthSmileRight:%.3f," +
                        "mouthFrownLeft:%.3f,mouthFrownRight:%.3f," +
                        "mouthPucker:%.3f,mouthFunnel:%.3f," +
                        "eyeBlinkLeft:%.3f,eyeBlinkRight:%.3f," +
                        "eyeLookUpLeft:%.3f,eyeLookUpRight:%.3f," +
                        "eyeLookDownLeft:%.3f,eyeLookDownRight:%.3f," +
                        "eyeWideLeft:%.3f,eyeWideRight:%.3f," +
                        "browInnerUp:%.3f," +
                        "browDownLeft:%.3f,browDownRight:%.3f," +
                        "browOuterUpLeft:%.3f,browOuterUpRight:%.3f," +
                        "cheekPuff:%.3f," +
                        "headPitch:%.2f,headYaw:%.2f,headRoll:%.2f" +
                        "});",
                b.jawOpen,
                b.mouthSmileLeft, b.mouthSmileRight,
                b.mouthFrownLeft, b.mouthFrownRight,
                b.mouthPucker, b.mouthFunnel,
                b.eyeBlinkLeft, b.eyeBlinkRight,
                b.eyeLookUpLeft, b.eyeLookUpRight,
                b.eyeLookDownLeft, b.eyeLookDownRight,
                b.eyeWideLeft, b.eyeWideRight,
                b.browInnerUp,
                b.browDownLeft, b.browDownRight,
                b.browOuterUpLeft, b.browOuterUpRight,
                b.cheekPuff,
                b.headPitch, b.headYaw, b.headRoll);

        avatarWebView.evaluateJavascript(js, null);
    }

    private String describeExpression(BlendshapeState b) {
        if (!b.faceDetected) return "—";
        StringBuilder sb = new StringBuilder();
        if (b.jawOpen > 0.4f) sb.append("Mouth open  ");
        if (b.smile() > 0.4f) sb.append("Smile  ");
        if (b.frown() > 0.4f) sb.append("Frown  ");
        if (b.eyeBlinkLeft > 0.6f && b.eyeBlinkRight > 0.6f) sb.append("Eyes closed  ");
        else if (b.eyeBlinkLeft > 0.6f || b.eyeBlinkRight > 0.6f) sb.append("Wink  ");
        if (b.browInnerUp > 0.4f) sb.append("Surprise  ");
        if (b.cheekPuff > 0.3f) sb.append("Cheek puff  ");
        if (sb.length() == 0) sb.append("Neutral");
        return sb.toString().trim();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission needed", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) cameraProvider.unbindAll();
        if (landmarkerHelper != null) landmarkerHelper.close();
        if (analysisExecutor != null) analysisExecutor.shutdown();
        if (avatarWebView != null) avatarWebView.destroy();
    }
}
