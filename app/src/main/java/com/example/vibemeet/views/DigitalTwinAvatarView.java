package com.example.vibemeet.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * Digital Twin Avatar — A real-time procedural face that mirrors the user.
 *
 * Driven by ML Kit Face Detection. Updates 30+ times per second:
 *   - Mouth shape morphs based on smile probability (0-1)
 *   - Eyelids close based on eye-open probabilities
 *   - Head tilts based on euler angles (X, Y, Z)
 *   - Cheek blush appears when smiling broadly
 */
public class DigitalTwinAvatarView extends View {

    // ===== Animation state (driven by ML Kit) =====
    private float smileProbability = 0.3f;
    private float leftEyeOpen = 1f;
    private float rightEyeOpen = 1f;
    private float headTiltZ = 0f;  // roll (-45 to 45 degrees)
    private float headRotateY = 0f; // yaw (-30 to 30 degrees)
    private float headRotateX = 0f; // pitch (-30 to 30 degrees)
    private boolean faceDetected = false;

    // ===== Paints =====
    private Paint facePaint;
    private Paint eyePaint;
    private Paint eyelidPaint;
    private Paint pupilPaint;
    private Paint mouthPaint;
    private Paint cheekPaint;
    private Paint outlinePaint;
    private Paint backgroundPaint;
    private Paint noFacePaint;

    public DigitalTwinAvatarView(Context context) {
        super(context);
        init();
    }

    public DigitalTwinAvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DigitalTwinAvatarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        facePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        facePaint.setColor(0xFFFFD89B);
        facePaint.setStyle(Paint.Style.FILL);

        eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eyePaint.setColor(Color.WHITE);

        eyelidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eyelidPaint.setColor(0xFFFFD89B);

        pupilPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pupilPaint.setColor(0xFF1A1A2E);

        mouthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mouthPaint.setStyle(Paint.Style.STROKE);
        mouthPaint.setStrokeCap(Paint.Cap.ROUND);
        mouthPaint.setColor(0xFFC62828);

        cheekPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cheekPaint.setColor(0x60FF8A80);

        outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setColor(0xFFE5BB7A);

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        noFacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        noFacePaint.setColor(0xFF999999);
        noFacePaint.setTextAlign(Paint.Align.CENTER);
        noFacePaint.setTextSize(dp(14));
    }

    // ===== Public API for the AI model to update state =====

    public void updateFaceState(boolean detected,
                                float smile, float leftEye, float rightEye,
                                float pitch, float yaw, float roll) {
        this.faceDetected = detected;
        this.smileProbability = clamp(smile, 0f, 1f);
        this.leftEyeOpen = clamp(leftEye, 0f, 1f);
        this.rightEyeOpen = clamp(rightEye, 0f, 1f);
        this.headRotateX = pitch;
        this.headRotateY = yaw;
        this.headTiltZ = roll;
        postInvalidate();
    }

    public void clearFace() {
        this.faceDetected = false;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        if (w == 0 || h == 0) return;

        float cx = w / 2f;
        float cy = h / 2f;
        float faceRadius = Math.min(w, h) / 2f * 0.7f;

        // Background gradient
        backgroundPaint.setShader(new RadialGradient(cx, cy, faceRadius * 2f,
                0xFFFFE0B2, 0xFFFAFAFA, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, backgroundPaint);

        if (!faceDetected) {
            canvas.drawText("👀  Looking for your face...", cx, cy, noFacePaint);
            return;
        }

        // ----- Apply head transformations -----
        canvas.save();

        // Roll (tilt) - rotate the canvas
        canvas.rotate(headTiltZ, cx, cy);

        // Yaw (left/right) - horizontal offset of features
        float yawShift = headRotateY * 0.8f;

        // Pitch (up/down) - vertical offset
        float pitchShift = headRotateX * 0.6f;

        // ----- Face oval -----
        outlinePaint.setStrokeWidth(dp(3));
        canvas.drawCircle(cx, cy, faceRadius, facePaint);
        canvas.drawCircle(cx, cy, faceRadius, outlinePaint);

        // ----- Eyes -----
        float eyeOffsetX = faceRadius * 0.35f;
        float eyeOffsetY = -faceRadius * 0.12f + pitchShift;
        float eyeBaseRadius = faceRadius * 0.13f;

        // Left eye (user's right, mirrored)
        drawEye(canvas, cx - eyeOffsetX + yawShift, cy + eyeOffsetY, eyeBaseRadius, rightEyeOpen);
        // Right eye (user's left, mirrored)
        drawEye(canvas, cx + eyeOffsetX + yawShift, cy + eyeOffsetY, eyeBaseRadius, leftEyeOpen);

        // ----- Eyebrows -----
        Paint browPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        browPaint.setColor(0xFF5D4037);
        browPaint.setStyle(Paint.Style.STROKE);
        browPaint.setStrokeWidth(dp(4));
        browPaint.setStrokeCap(Paint.Cap.ROUND);
        float browY = cy + eyeOffsetY - eyeBaseRadius - dp(8);
        float browLen = eyeBaseRadius * 1.4f;
        // Raise brows slightly when smiling
        float browLift = smileProbability * dp(3);
        canvas.drawLine(cx - eyeOffsetX - browLen / 2 + yawShift, browY - browLift,
                cx - eyeOffsetX + browLen / 2 + yawShift, browY - browLift / 2, browPaint);
        canvas.drawLine(cx + eyeOffsetX - browLen / 2 + yawShift, browY - browLift / 2,
                cx + eyeOffsetX + browLen / 2 + yawShift, browY - browLift, browPaint);

        // ----- Nose -----
        Paint nosePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nosePaint.setColor(0xFFE5BB7A);
        nosePaint.setStyle(Paint.Style.STROKE);
        nosePaint.setStrokeWidth(dp(2.5f));
        nosePaint.setStrokeCap(Paint.Cap.ROUND);
        float noseX = cx + yawShift;
        float noseTop = cy + eyeOffsetY + eyeBaseRadius + dp(6);
        float noseBottom = cy + faceRadius * 0.1f + pitchShift;
        canvas.drawLine(noseX, noseTop, noseX - dp(4), noseBottom, nosePaint);
        canvas.drawLine(noseX - dp(4), noseBottom, noseX + dp(4), noseBottom, nosePaint);

        // ----- Mouth (morphs from frown to wide smile) -----
        float mouthY = cy + faceRadius * 0.35f + pitchShift;
        float mouthWidth = faceRadius * (0.5f + 0.2f * smileProbability);
        float mouthCurve = (smileProbability - 0.3f) * faceRadius * 0.35f; // negative = frown
        mouthPaint.setStrokeWidth(dp(4 + smileProbability * 2));

        Path mouthPath = new Path();
        mouthPath.moveTo(cx - mouthWidth / 2 + yawShift, mouthY);
        mouthPath.quadTo(cx + yawShift, mouthY + mouthCurve,
                cx + mouthWidth / 2 + yawShift, mouthY);
        canvas.drawPath(mouthPath, mouthPaint);

        // Open mouth slightly when smiling broadly
        if (smileProbability > 0.7f) {
            Paint mouthFill = new Paint(Paint.ANTI_ALIAS_FLAG);
            mouthFill.setColor(0xFF8B2222);
            Path innerMouth = new Path();
            float innerWidth = mouthWidth * 0.6f * smileProbability;
            float innerHeight = dp(6 * smileProbability);
            innerMouth.moveTo(cx - innerWidth / 2 + yawShift, mouthY + dp(2));
            innerMouth.quadTo(cx + yawShift, mouthY + mouthCurve * 0.7f + innerHeight,
                    cx + innerWidth / 2 + yawShift, mouthY + dp(2));
            innerMouth.quadTo(cx + yawShift, mouthY + dp(1),
                    cx - innerWidth / 2 + yawShift, mouthY + dp(2));
            canvas.drawPath(innerMouth, mouthFill);
        }

        // ----- Cheeks (blush when smiling) -----
        if (smileProbability > 0.4f) {
            float blushAlpha = (smileProbability - 0.4f) * 1.6f;
            cheekPaint.setAlpha((int) (180 * Math.min(blushAlpha, 1f)));
            float cheekY = cy + faceRadius * 0.15f + pitchShift;
            canvas.drawCircle(cx - eyeOffsetX * 1.05f + yawShift, cheekY, faceRadius * 0.12f, cheekPaint);
            canvas.drawCircle(cx + eyeOffsetX * 1.05f + yawShift, cheekY, faceRadius * 0.12f, cheekPaint);
        }

        canvas.restore();
    }

    private void drawEye(Canvas canvas, float cx, float cy, float radius, float openness) {
        // Eye white
        canvas.drawCircle(cx, cy, radius, eyePaint);
        outlinePaint.setStrokeWidth(dp(1.5f));
        canvas.drawCircle(cx, cy, radius, outlinePaint);

        // Pupil
        canvas.drawCircle(cx, cy, radius * 0.5f, pupilPaint);

        // Highlight on pupil
        Paint hl = new Paint(Paint.ANTI_ALIAS_FLAG);
        hl.setColor(Color.WHITE);
        canvas.drawCircle(cx - radius * 0.15f, cy - radius * 0.15f, radius * 0.15f, hl);

        // Eyelid (covers eye based on openness)
        if (openness < 0.95f) {
            float closedFraction = 1f - openness;
            // Top eyelid
            Path lid = new Path();
            float lidY = cy - radius + closedFraction * radius * 2f;
            lid.moveTo(cx - radius - dp(2), cy - radius - dp(2));
            lid.lineTo(cx + radius + dp(2), cy - radius - dp(2));
            lid.lineTo(cx + radius + dp(2), lidY);
            lid.lineTo(cx - radius - dp(2), lidY);
            lid.close();
            canvas.drawPath(lid, eyelidPaint);

            // Eyelash line if mostly closed
            if (closedFraction > 0.7f) {
                Paint lashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                lashPaint.setColor(0xFF3E2723);
                lashPaint.setStyle(Paint.Style.STROKE);
                lashPaint.setStrokeWidth(dp(2));
                lashPaint.setStrokeCap(Paint.Cap.ROUND);
                canvas.drawLine(cx - radius, lidY, cx + radius, lidY, lashPaint);
            }
        }
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private float dp(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
