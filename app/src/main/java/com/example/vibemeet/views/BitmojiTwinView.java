package com.example.vibemeet.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import com.example.vibemeet.models.BlendshapeState;

/**
 * Bitmoji/Memoji-style 3D-shaded cartoon avatar driven by MediaPipe blendshapes.
 *
 * Every facial part is morphed by the corresponding blendshape:
 *   - jawOpen        → mouth opens vertically (shows teeth & tongue)
 *   - mouthSmile     → mouth corners curve UP
 *   - mouthFrown     → mouth corners curve DOWN
 *   - eyeBlink       → eyelids close
 *   - eyeLook*       → pupils move in that direction
 *   - browInnerUp    → eyebrows raise (surprise)
 *   - browDown       → eyebrows lower (anger)
 *   - cheekPuff      → cheeks expand
 *   - headRoll/Yaw/Pitch → whole face rotates in 3D space
 */
public class BitmojiTwinView extends View {

    private BlendshapeState state = new BlendshapeState();

    // Smoothed values to avoid jitter
    private BlendshapeState smoothed = new BlendshapeState();
    private static final float SMOOTH = 0.35f;

    private Paint facePaint;
    private Paint faceShadePaint;
    private Paint eyeWhitePaint;
    private Paint pupilPaint;
    private Paint pupilHighlightPaint;
    private Paint eyelidPaint;
    private Paint lashPaint;
    private Paint browPaint;
    private Paint mouthOutlinePaint;
    private Paint mouthInnerPaint;
    private Paint teethPaint;
    private Paint tonguePaint;
    private Paint cheekBlushPaint;
    private Paint noseShadePaint;
    private Paint backgroundPaint;
    private Paint statusPaint;

    private boolean mirrorX = true;

    public BitmojiTwinView(Context context) { super(context); init(); }
    public BitmojiTwinView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public BitmojiTwinView(Context context, AttributeSet attrs, int s) { super(context, attrs, s); init(); }

    private void init() {
        facePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        faceShadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        eyeWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eyeWhitePaint.setColor(Color.WHITE);

        pupilPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pupilPaint.setColor(0xFF3E2723);

        pupilHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pupilHighlightPaint.setColor(Color.WHITE);

        eyelidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        lashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lashPaint.setColor(0xFF3E2723);
        lashPaint.setStyle(Paint.Style.STROKE);
        lashPaint.setStrokeCap(Paint.Cap.ROUND);

        browPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        browPaint.setColor(0xFF4E342E);
        browPaint.setStyle(Paint.Style.STROKE);
        browPaint.setStrokeCap(Paint.Cap.ROUND);

        mouthOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mouthOutlinePaint.setColor(0xFFAD1457);
        mouthOutlinePaint.setStyle(Paint.Style.STROKE);
        mouthOutlinePaint.setStrokeCap(Paint.Cap.ROUND);
        mouthOutlinePaint.setStrokeJoin(Paint.Join.ROUND);

        mouthInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mouthInnerPaint.setColor(0xFF6A1B1B);

        teethPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        teethPaint.setColor(0xFFFFFFFF);

        tonguePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tonguePaint.setColor(0xFFE57373);

        cheekBlushPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cheekBlushPaint.setColor(0x55FF6B9D);

        noseShadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        statusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        statusPaint.setColor(0xFFFFFFFF);
        statusPaint.setTextAlign(Paint.Align.CENTER);
        statusPaint.setTextSize(dp(15));
    }

    public void setMirrorX(boolean m) { this.mirrorX = m; }

    public void updateState(BlendshapeState newState) {
        if (newState == null) return;
        this.state = newState;
        smoothToTarget();
        postInvalidateOnAnimation();
    }

    public void clearFace() {
        BlendshapeState empty = new BlendshapeState();
        empty.faceDetected = false;
        this.state = empty;
        postInvalidate();
    }

    /** Exponential smoothing to reduce jitter from frame-to-frame inference noise. */
    private void smoothToTarget() {
        smoothed.faceDetected = state.faceDetected;
        smoothed.jawOpen = lerp(smoothed.jawOpen, state.jawOpen, SMOOTH);
        smoothed.mouthSmileLeft = lerp(smoothed.mouthSmileLeft, state.mouthSmileLeft, SMOOTH);
        smoothed.mouthSmileRight = lerp(smoothed.mouthSmileRight, state.mouthSmileRight, SMOOTH);
        smoothed.mouthFrownLeft = lerp(smoothed.mouthFrownLeft, state.mouthFrownLeft, SMOOTH);
        smoothed.mouthFrownRight = lerp(smoothed.mouthFrownRight, state.mouthFrownRight, SMOOTH);
        smoothed.mouthPucker = lerp(smoothed.mouthPucker, state.mouthPucker, SMOOTH);
        smoothed.mouthFunnel = lerp(smoothed.mouthFunnel, state.mouthFunnel, SMOOTH);
        smoothed.eyeBlinkLeft = lerp(smoothed.eyeBlinkLeft, state.eyeBlinkLeft, 0.6f);
        smoothed.eyeBlinkRight = lerp(smoothed.eyeBlinkRight, state.eyeBlinkRight, 0.6f);
        smoothed.eyeLookUpLeft = lerp(smoothed.eyeLookUpLeft, state.eyeLookUpLeft, SMOOTH);
        smoothed.eyeLookUpRight = lerp(smoothed.eyeLookUpRight, state.eyeLookUpRight, SMOOTH);
        smoothed.eyeLookDownLeft = lerp(smoothed.eyeLookDownLeft, state.eyeLookDownLeft, SMOOTH);
        smoothed.eyeLookDownRight = lerp(smoothed.eyeLookDownRight, state.eyeLookDownRight, SMOOTH);
        smoothed.eyeWideLeft = lerp(smoothed.eyeWideLeft, state.eyeWideLeft, SMOOTH);
        smoothed.eyeWideRight = lerp(smoothed.eyeWideRight, state.eyeWideRight, SMOOTH);
        smoothed.browInnerUp = lerp(smoothed.browInnerUp, state.browInnerUp, SMOOTH);
        smoothed.browDownLeft = lerp(smoothed.browDownLeft, state.browDownLeft, SMOOTH);
        smoothed.browDownRight = lerp(smoothed.browDownRight, state.browDownRight, SMOOTH);
        smoothed.browOuterUpLeft = lerp(smoothed.browOuterUpLeft, state.browOuterUpLeft, SMOOTH);
        smoothed.browOuterUpRight = lerp(smoothed.browOuterUpRight, state.browOuterUpRight, SMOOTH);
        smoothed.cheekPuff = lerp(smoothed.cheekPuff, state.cheekPuff, SMOOTH);
        smoothed.headRoll = lerp(smoothed.headRoll, state.headRoll, SMOOTH);
        smoothed.headYaw = lerp(smoothed.headYaw, state.headYaw, SMOOTH);
        smoothed.headPitch = lerp(smoothed.headPitch, state.headPitch, SMOOTH);
    }

    private float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w == 0 || h == 0) return;

        // Background — soft sky blue gradient like Memoji
        backgroundPaint.setShader(new LinearGradient(0, 0, 0, h,
                0xFFFFE0B2, 0xFFFFCCBC, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, backgroundPaint);

        if (!smoothed.faceDetected) {
            statusPaint.setColor(0xFF5D4037);
            canvas.drawText("👀  Looking for your face...", w / 2f, h / 2f, statusPaint);
            return;
        }

        float cx = w / 2f;
        float cy = h * 0.52f;
        float faceR = Math.min(w, h) * 0.36f;

        // Apply head rotation as canvas transforms
        canvas.save();
        // Roll
        canvas.rotate(mirrorX ? smoothed.headRoll : -smoothed.headRoll, cx, cy);
        // Yaw shift (horizontal offset for 2.5D effect)
        float yawShift = (mirrorX ? -smoothed.headYaw : smoothed.headYaw) * 0.6f;
        // Pitch shift (vertical offset)
        float pitchShift = smoothed.headPitch * 0.4f;

        // ===== FACE (with 3D shading) =====
        drawFace(canvas, cx, cy, faceR);

        // ===== EARS =====
        drawEars(canvas, cx, cy, faceR);

        // ===== HAIR (top) =====
        drawHair(canvas, cx, cy, faceR);

        // ===== EYES =====
        float eyeOffsetX = faceR * 0.35f;
        float eyeY = cy - faceR * 0.15f + pitchShift;
        float eyeR = faceR * 0.13f;

        drawEye(canvas, cx - eyeOffsetX + yawShift, eyeY, eyeR,
                smoothed.eyeBlinkLeft, smoothed.eyeWideLeft,
                smoothed.eyeLookUpLeft - smoothed.eyeLookDownLeft,
                mirrorX ? -1 : 1);
        drawEye(canvas, cx + eyeOffsetX + yawShift, eyeY, eyeR,
                smoothed.eyeBlinkRight, smoothed.eyeWideRight,
                smoothed.eyeLookUpRight - smoothed.eyeLookDownRight,
                mirrorX ? 1 : -1);

        // ===== EYEBROWS =====
        drawEyebrow(canvas, cx - eyeOffsetX + yawShift, eyeY - eyeR - dp(10),
                eyeR * 1.5f, smoothed.browInnerUp, smoothed.browDownLeft, smoothed.browOuterUpLeft, true);
        drawEyebrow(canvas, cx + eyeOffsetX + yawShift, eyeY - eyeR - dp(10),
                eyeR * 1.5f, smoothed.browInnerUp, smoothed.browDownRight, smoothed.browOuterUpRight, false);

        // ===== NOSE =====
        drawNose(canvas, cx + yawShift, cy + pitchShift, faceR);

        // ===== MOUTH =====
        float mouthY = cy + faceR * 0.42f + pitchShift;
        drawMouth(canvas, cx + yawShift, mouthY, faceR,
                smoothed.jawOpen, smoothed.smile(), smoothed.frown(),
                smoothed.mouthPucker, smoothed.mouthFunnel);

        // ===== CHEEKS / BLUSH (when smiling or puffing) =====
        drawCheeks(canvas, cx + yawShift, cy + pitchShift, faceR,
                smoothed.smile(), smoothed.cheekPuff);

        canvas.restore();
    }

    // ============================================================
    // Drawing helpers
    // ============================================================

    private void drawFace(Canvas canvas, float cx, float cy, float faceR) {
        // 3D-looking shaded face — radial gradient gives depth
        facePaint.setShader(new RadialGradient(
                cx - faceR * 0.3f, cy - faceR * 0.3f,
                faceR * 1.8f,
                new int[]{0xFFFFE0B2, 0xFFFFCC80, 0xFFE6A57E},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP));

        // Wider when cheek puff
        float puffScale = 1f + smoothed.cheekPuff * 0.06f;

        canvas.drawOval(
                cx - faceR * 1.05f * puffScale, cy - faceR * 1.1f,
                cx + faceR * 1.05f * puffScale, cy + faceR * 1.15f,
                facePaint);

        // Subtle outline for definition
        Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
        outline.setStyle(Paint.Style.STROKE);
        outline.setColor(0x40000000);
        outline.setStrokeWidth(dp(1.5f));
        canvas.drawOval(
                cx - faceR * 1.05f * puffScale, cy - faceR * 1.1f,
                cx + faceR * 1.05f * puffScale, cy + faceR * 1.15f,
                outline);
    }

    private void drawEars(Canvas canvas, float cx, float cy, float faceR) {
        Paint earPaint = new Paint(facePaint);
        earPaint.setShader(new RadialGradient(cx, cy, faceR * 1.5f,
                0xFFFFCC80, 0xFFD89060, Shader.TileMode.CLAMP));

        // Left ear
        canvas.drawOval(
                cx - faceR * 1.18f, cy - faceR * 0.2f,
                cx - faceR * 0.95f, cy + faceR * 0.2f,
                earPaint);
        // Right ear
        canvas.drawOval(
                cx + faceR * 0.95f, cy - faceR * 0.2f,
                cx + faceR * 1.18f, cy + faceR * 0.2f,
                earPaint);
    }

    private void drawHair(Canvas canvas, float cx, float cy, float faceR) {
        Paint hairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hairPaint.setShader(new LinearGradient(
                cx, cy - faceR * 1.2f, cx, cy - faceR * 0.5f,
                0xFF3E2723, 0xFF5D4037, Shader.TileMode.CLAMP));

        Path hair = new Path();
        hair.moveTo(cx - faceR * 1.1f, cy - faceR * 0.4f);
        hair.cubicTo(
                cx - faceR * 1.3f, cy - faceR * 1.4f,
                cx + faceR * 1.3f, cy - faceR * 1.4f,
                cx + faceR * 1.1f, cy - faceR * 0.4f);
        hair.cubicTo(
                cx + faceR * 0.6f, cy - faceR * 0.95f,
                cx - faceR * 0.6f, cy - faceR * 0.95f,
                cx - faceR * 1.1f, cy - faceR * 0.4f);
        hair.close();
        canvas.drawPath(hair, hairPaint);
    }

    private void drawEye(Canvas canvas, float cx, float cy, float r,
                         float blink, float wide, float lookY, int lookXDir) {
        // Eye open factor (0 = closed, 1 = open). Wide boosts above 1.
        float openness = 1f - blink + wide * 0.3f;
        openness = Math.max(0f, Math.min(1.3f, openness));

        // Slight wide eye = taller
        float verticalR = r * openness;
        if (verticalR < dp(2)) verticalR = dp(2);

        // ===== Eye white =====
        RectF eyeRect = new RectF(cx - r, cy - verticalR, cx + r, cy + verticalR);
        canvas.drawOval(eyeRect, eyeWhitePaint);

        // Subtle outline
        Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
        outline.setStyle(Paint.Style.STROKE);
        outline.setColor(0x40000000);
        outline.setStrokeWidth(dp(1.2f));
        canvas.drawOval(eyeRect, outline);

        if (openness > 0.05f) {
            // ===== Pupil =====
            float pupilR = r * 0.55f;
            // Pupil position offset (look direction)
            float pupilDx = lookXDir * r * 0.15f; // outward gaze
            float pupilDy = -lookY * r * 0.4f;     // up if positive

            canvas.drawCircle(cx + pupilDx, cy + pupilDy, pupilR, pupilPaint);
            // Inner pupil
            Paint iris = new Paint(Paint.ANTI_ALIAS_FLAG);
            iris.setColor(0xFF6D4C41);
            canvas.drawCircle(cx + pupilDx, cy + pupilDy, pupilR * 0.7f, iris);
            // Highlight (sparkle)
            canvas.drawCircle(
                    cx + pupilDx - pupilR * 0.25f,
                    cy + pupilDy - pupilR * 0.25f,
                    pupilR * 0.25f,
                    pupilHighlightPaint);
        }

        // ===== Top lid line (eyelash) =====
        lashPaint.setStrokeWidth(dp(2.5f));
        canvas.drawArc(eyeRect, 180, 180, false, lashPaint);

        // When eye nearly closed, draw a horizontal line
        if (openness < 0.15f) {
            Paint closedLid = new Paint(Paint.ANTI_ALIAS_FLAG);
            closedLid.setColor(0xFF3E2723);
            closedLid.setStyle(Paint.Style.STROKE);
            closedLid.setStrokeWidth(dp(3));
            closedLid.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawLine(cx - r, cy, cx + r, cy, closedLid);
        }
    }

    private void drawEyebrow(Canvas canvas, float cx, float cy, float halfLen,
                              float innerUp, float down, float outerUp, boolean isLeft) {
        float browLift = innerUp * dp(8) + outerUp * dp(6) - down * dp(6);

        browPaint.setStrokeWidth(dp(5));

        Path p = new Path();
        // Inner end (closer to nose)
        float innerX = isLeft ? cx + halfLen * 0.6f : cx - halfLen * 0.6f;
        float outerX = isLeft ? cx - halfLen : cx + halfLen;
        float innerY = cy - browLift * (innerUp > 0.1f ? 1.5f : 0.5f);
        float outerY = cy - outerUp * dp(6);

        p.moveTo(innerX, innerY);
        p.quadTo((innerX + outerX) / 2f, outerY - dp(4), outerX, outerY);
        canvas.drawPath(p, browPaint);
    }

    private void drawNose(Canvas canvas, float cx, float cy, float faceR) {
        Paint noseStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        noseStroke.setStyle(Paint.Style.STROKE);
        noseStroke.setColor(0x60724E2F);
        noseStroke.setStrokeWidth(dp(2));
        noseStroke.setStrokeCap(Paint.Cap.ROUND);

        // Nose bridge
        float noseTopY = cy - faceR * 0.05f;
        float noseBottomY = cy + faceR * 0.18f;
        canvas.drawLine(cx, noseTopY, cx - dp(5), noseBottomY, noseStroke);

        // Nostril hint
        Paint nostril = new Paint(Paint.ANTI_ALIAS_FLAG);
        nostril.setColor(0x40000000);
        canvas.drawOval(cx - faceR * 0.08f, noseBottomY - dp(2),
                cx - faceR * 0.02f, noseBottomY + dp(3), nostril);
        canvas.drawOval(cx + faceR * 0.02f, noseBottomY - dp(2),
                cx + faceR * 0.08f, noseBottomY + dp(3), nostril);

        // Highlight
        Paint hl = new Paint(Paint.ANTI_ALIAS_FLAG);
        hl.setColor(0x60FFFFFF);
        canvas.drawCircle(cx + dp(2), noseBottomY - dp(4), dp(2), hl);
    }

    private void drawMouth(Canvas canvas, float cx, float cy, float faceR,
                            float jawOpen, float smile, float frown,
                            float pucker, float funnel) {
        // Base mouth width — narrower when pucker, wider when smile
        float baseWidth = faceR * 0.55f;
        float widthFactor = 1f + smile * 0.35f - pucker * 0.5f;
        float mouthHalfW = baseWidth * widthFactor;

        // Vertical opening from jawOpen and funnel
        float openHeight = (jawOpen * faceR * 0.5f) + (funnel * faceR * 0.15f);

        // Smile/frown curve
        float cornerY = (smile - frown) * faceR * 0.18f;

        if (openHeight < dp(4)) {
            // Closed-mouth state — just the lip curve
            drawClosedMouth(canvas, cx, cy, mouthHalfW, cornerY, smile, frown);
        } else {
            // Open mouth — show inner, teeth, tongue
            drawOpenMouth(canvas, cx, cy, mouthHalfW, openHeight, cornerY, smile);
        }
    }

    private void drawClosedMouth(Canvas canvas, float cx, float cy,
                                  float halfW, float cornerY, float smile, float frown) {
        mouthOutlinePaint.setStrokeWidth(dp(4f + smile * 2));
        Path p = new Path();
        p.moveTo(cx - halfW, cy - cornerY);
        p.quadTo(cx, cy + cornerY * 1.5f + dp(2 * smile), cx + halfW, cy - cornerY);
        canvas.drawPath(p, mouthOutlinePaint);

        // Subtle smile dimples
        if (smile > 0.6f) {
            Paint dimple = new Paint(Paint.ANTI_ALIAS_FLAG);
            dimple.setColor(0x30000000);
            canvas.drawCircle(cx - halfW - dp(2), cy - cornerY + dp(2), dp(2), dimple);
            canvas.drawCircle(cx + halfW + dp(2), cy - cornerY + dp(2), dp(2), dimple);
        }
    }

    private void drawOpenMouth(Canvas canvas, float cx, float cy,
                                float halfW, float openH, float cornerY, float smile) {
        // Outer mouth shape
        Path mouthShape = new Path();
        float topY = cy - cornerY;
        float bottomY = cy + openH;

        mouthShape.moveTo(cx - halfW, topY);
        mouthShape.quadTo(cx, topY - dp(2), cx + halfW, topY);
        mouthShape.quadTo(cx + halfW * 0.9f, bottomY * 0.9f + topY * 0.1f, cx, bottomY);
        mouthShape.quadTo(cx - halfW * 0.9f, bottomY * 0.9f + topY * 0.1f, cx - halfW, topY);
        mouthShape.close();

        // Inner mouth (dark)
        canvas.drawPath(mouthShape, mouthInnerPaint);

        // Top teeth (visible when smiling open)
        if (smile > 0.2f && openH > dp(10)) {
            float teethTopY = topY + dp(1);
            float teethBottomY = teethTopY + Math.min(openH * 0.35f, dp(14));
            float teethHalfW = halfW * 0.85f;
            RectF teethRect = new RectF(cx - teethHalfW, teethTopY,
                    cx + teethHalfW, teethBottomY);
            canvas.save();
            canvas.clipPath(mouthShape);
            canvas.drawRoundRect(teethRect, dp(3), dp(3), teethPaint);
            canvas.restore();
        }

        // Tongue (when mouth is very open)
        if (openH > dp(18)) {
            canvas.save();
            canvas.clipPath(mouthShape);
            float tongueHalfW = halfW * 0.55f;
            float tongueTop = cy + openH * 0.45f;
            float tongueBottom = bottomY - dp(2);
            RectF tongueRect = new RectF(cx - tongueHalfW, tongueTop,
                    cx + tongueHalfW, tongueBottom);
            canvas.drawRoundRect(tongueRect, dp(6), dp(6), tonguePaint);
            canvas.restore();
        }

        // Mouth outline
        mouthOutlinePaint.setStrokeWidth(dp(3.5f));
        canvas.drawPath(mouthShape, mouthOutlinePaint);
    }

    private void drawCheeks(Canvas canvas, float cx, float cy, float faceR,
                             float smile, float puff) {
        float alpha = Math.min(1f, smile * 0.8f + puff * 0.9f);
        if (alpha < 0.05f) return;

        cheekBlushPaint.setAlpha((int) (170 * alpha));
        float cheekY = cy + faceR * 0.18f;
        float cheekR = faceR * (0.22f + puff * 0.1f);
        canvas.drawCircle(cx - faceR * 0.55f, cheekY, cheekR, cheekBlushPaint);
        canvas.drawCircle(cx + faceR * 0.55f, cheekY, cheekR, cheekBlushPaint);
    }

    private float dp(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
