package com.example.vibemeet.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;
import android.os.SystemClock;

import androidx.camera.core.ImageProxy;

import com.example.vibemeet.models.BlendshapeState;
import com.example.vibemeet.views.Face3DMeshView;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.Category;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs MediaPipe Face Landmarker on camera frames to extract 468 3D facial landmarks.
 * Designed for real-time use (LIVE_STREAM mode) — results stream asynchronously.
 */
public class FaceLandmarkerHelper {

    public interface Listener {
        void onResult(List<Face3DMeshView.Landmark> landmarks, BlendshapeState blendshapes, long inferenceMs);
        void onNoFaceDetected();
        void onError(String error);
    }

    private FaceLandmarker faceLandmarker;
    private final Context context;
    private Listener listener;

    public FaceLandmarkerHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setup() {
        try {
            BaseOptions baseOptions = BaseOptions.builder()
                    .setDelegate(Delegate.CPU)
                    .setModelAssetPath("face_landmarker.task")
                    .build();

            FaceLandmarker.FaceLandmarkerOptions options =
                    FaceLandmarker.FaceLandmarkerOptions.builder()
                            .setBaseOptions(baseOptions)
                            .setRunningMode(RunningMode.LIVE_STREAM)
                            .setNumFaces(1)
                            .setMinFaceDetectionConfidence(0.5f)
                            .setMinTrackingConfidence(0.5f)
                            .setMinFacePresenceConfidence(0.5f)
                            .setOutputFaceBlendshapes(true)
                            .setResultListener(this::handleResult)
                            .setErrorListener(e -> {
                                if (listener != null) listener.onError(e.getMessage());
                            })
                            .build();

            faceLandmarker = FaceLandmarker.createFromOptions(context, options);
        } catch (Exception e) {
            if (listener != null) listener.onError("MediaPipe setup failed: " + e.getMessage());
        }
    }

    /**
     * Process an ImageProxy frame from CameraX.
     * Rotates the image to portrait, converts to MPImage, sends async to the model.
     */
    public void detectLiveStream(ImageProxy imageProxy, boolean isFrontCamera) {
        if (faceLandmarker == null) {
            imageProxy.close();
            return;
        }

        long frameTime = SystemClock.uptimeMillis();

        try {
            // Convert ImageProxy → Bitmap
            Bitmap bitmap = imageProxyToBitmap(imageProxy);
            if (bitmap == null) {
                imageProxy.close();
                return;
            }

            // Rotate to match display orientation
            Matrix matrix = new Matrix();
            matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());
            if (isFrontCamera) {
                matrix.postScale(-1f, 1f, bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);
            }
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            MPImage mpImage = new BitmapImageBuilder(rotated).build();
            faceLandmarker.detectAsync(mpImage, frameTime);
        } catch (Exception e) {
            // Silently skip frame on error
        } finally {
            imageProxy.close();
        }
    }

    private void handleResult(FaceLandmarkerResult result, MPImage input) {
        if (listener == null) return;

        long inferenceTime = SystemClock.uptimeMillis() - result.timestampMs();

        if (result.faceLandmarks().isEmpty()) {
            listener.onNoFaceDetected();
            return;
        }

        // First detected face
        List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> mpLandmarks =
                result.faceLandmarks().get(0);

        List<Face3DMeshView.Landmark> landmarks = new ArrayList<>(mpLandmarks.size());
        for (com.google.mediapipe.tasks.components.containers.NormalizedLandmark lm : mpLandmarks) {
            landmarks.add(new Face3DMeshView.Landmark(lm.x(), lm.y(), lm.z()));
        }

        // Extract blendshapes (52 categories like jawOpen, mouthSmile, eyeBlink…)
        BlendshapeState state = extractBlendshapes(result);
        state.faceDetected = true;

        // Compute head rotation from landmarks (approximate but works smoothly)
        computeHeadRotation(landmarks, state);

        listener.onResult(landmarks, state, inferenceTime);
    }

    private BlendshapeState extractBlendshapes(FaceLandmarkerResult result) {
        BlendshapeState s = new BlendshapeState();
        if (result.faceBlendshapes().isEmpty()) return s;
        if (!result.faceBlendshapes().isPresent()) return s;

        List<Category> blendshapes = result.faceBlendshapes().get().get(0);
        for (Category c : blendshapes) {
            String name = c.categoryName();
            float score = c.score();
            switch (name) {
                case "jawOpen": s.jawOpen = score; break;
                case "mouthSmileLeft": s.mouthSmileLeft = score; break;
                case "mouthSmileRight": s.mouthSmileRight = score; break;
                case "mouthFrownLeft": s.mouthFrownLeft = score; break;
                case "mouthFrownRight": s.mouthFrownRight = score; break;
                case "mouthPucker": s.mouthPucker = score; break;
                case "mouthFunnel": s.mouthFunnel = score; break;
                case "eyeBlinkLeft": s.eyeBlinkLeft = score; break;
                case "eyeBlinkRight": s.eyeBlinkRight = score; break;
                case "eyeLookUpLeft": s.eyeLookUpLeft = score; break;
                case "eyeLookUpRight": s.eyeLookUpRight = score; break;
                case "eyeLookDownLeft": s.eyeLookDownLeft = score; break;
                case "eyeLookDownRight": s.eyeLookDownRight = score; break;
                case "eyeWideLeft": s.eyeWideLeft = score; break;
                case "eyeWideRight": s.eyeWideRight = score; break;
                case "browInnerUp": s.browInnerUp = score; break;
                case "browDownLeft": s.browDownLeft = score; break;
                case "browDownRight": s.browDownRight = score; break;
                case "browOuterUpLeft": s.browOuterUpLeft = score; break;
                case "browOuterUpRight": s.browOuterUpRight = score; break;
                case "cheekPuff": s.cheekPuff = score; break;
            }
        }
        return s;
    }

    /**
     * Approximate head rotation from key landmark positions.
     * Roll (Z): from line between eyes.
     * Yaw (Y): from horizontal offset of nose tip relative to face center.
     * Pitch (X): from vertical offset of nose tip.
     */
    private void computeHeadRotation(List<Face3DMeshView.Landmark> lms, BlendshapeState s) {
        if (lms.size() < 400) return;

        // Landmark indices (MediaPipe Face Mesh)
        Face3DMeshView.Landmark leftEye = lms.get(33);   // outer left eye
        Face3DMeshView.Landmark rightEye = lms.get(263); // outer right eye
        Face3DMeshView.Landmark noseTip = lms.get(1);
        Face3DMeshView.Landmark chin = lms.get(152);
        Face3DMeshView.Landmark forehead = lms.get(10);

        // Roll: angle of eye line
        double dx = rightEye.x - leftEye.x;
        double dy = rightEye.y - leftEye.y;
        s.headRoll = (float) Math.toDegrees(Math.atan2(dy, dx));

        // Yaw: nose horizontal position vs eye center
        float eyeCenterX = (leftEye.x + rightEye.x) / 2f;
        float eyeWidth = Math.abs(rightEye.x - leftEye.x);
        if (eyeWidth > 0) {
            s.headYaw = ((noseTip.x - eyeCenterX) / eyeWidth) * 90f;
        }

        // Pitch: nose vertical position between forehead and chin
        float faceHeight = chin.y - forehead.y;
        if (faceHeight > 0) {
            float noseNorm = (noseTip.y - forehead.y) / faceHeight;
            // Normalize around 0.5 (center)
            s.headPitch = (noseNorm - 0.5f) * 90f;
        }
    }

    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        try {
            Image image = imageProxy.getImage();
            if (image == null) return null;

            // For YUV_420_888, use the helper conversion
            if (image.getFormat() == android.graphics.ImageFormat.YUV_420_888) {
                return yuvToBitmap(image);
            }

            // Fallback for other formats
            ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap yuvToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        android.graphics.YuvImage yuvImage = new android.graphics.YuvImage(
                nv21, android.graphics.ImageFormat.NV21,
                image.getWidth(), image.getHeight(), null);

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        yuvImage.compressToJpeg(new android.graphics.Rect(0, 0,
                image.getWidth(), image.getHeight()), 80, out);
        byte[] jpeg = out.toByteArray();
        return android.graphics.BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
    }

    public void close() {
        if (faceLandmarker != null) {
            try {
                faceLandmarker.close();
            } catch (Exception ignored) {}
            faceLandmarker = null;
        }
    }
}
