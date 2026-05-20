package com.example.vibemeet.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Custom animated avatar view inspired by Bitmoji/Memoji animations.
 *
 * Features:
 *  - Continuous breathing pulse (subtle scaling)
 *  - Gentle bobbing motion (sine wave on Y)
 *  - Animated glowing ring around the avatar
 *  - Floating particles that emit upward (mood-based)
 *  - Circular crop with anti-aliased edges
 *
 * Usage:
 *   avatarView.setAvatarBitmap(bitmap);
 *   avatarView.setMood("happy");  // happy, chill, adventure, romantic
 *   avatarView.startAnimating();
 */
public class AnimatedAvatarView extends View {

    private Bitmap avatarBitmap;
    private BitmapShader shader;
    private Matrix shaderMatrix = new Matrix();

    private Paint imagePaint;
    private Paint ringPaint;
    private Paint glowPaint;
    private Paint particlePaint;

    private float pulseScale = 1f;       // breathing pulse
    private float bobOffset = 0f;        // vertical bob
    private float ringGlow = 0f;         // ring intensity 0-1
    private long startTime;

    private ValueAnimator pulseAnimator;
    private ValueAnimator bobAnimator;
    private ValueAnimator ringAnimator;
    private ValueAnimator particleTicker;

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private String mood = "happy";
    private boolean animating = false;

    // Ring colors (gradient cycle: orange → red → yellow)
    private final int[] ringColors = new int[]{
            0xFFE63946, 0xFFF77F00, 0xFFFCBF49, 0xFFE63946
    };
    private int currentRingColorIdx = 0;

    public AnimatedAvatarView(Context context) {
        super(context);
        init();
    }

    public AnimatedAvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedAvatarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dpToPx(4));
        ringPaint.setColor(0xFFE63946);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.FILL);

        particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particlePaint.setTextAlign(Paint.Align.CENTER);

        startTime = System.currentTimeMillis();
    }

    public void setAvatarBitmap(Bitmap bitmap) {
        this.avatarBitmap = bitmap;
        if (bitmap != null) {
            shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            imagePaint.setShader(shader);
        } else {
            shader = null;
            imagePaint.setShader(null);
        }
        invalidate();
    }

    public void setMood(String mood) {
        if (mood == null) mood = "happy";
        this.mood = mood;
        invalidate();
    }

    public void startAnimating() {
        if (animating) return;
        animating = true;

        // 1) Breathing pulse: 1.0 → 1.06 → 1.0 every 2.4s
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.06f, 1f);
        pulseAnimator.setDuration(2400);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        pulseAnimator.addUpdateListener(a -> {
            pulseScale = (float) a.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();

        // 2) Vertical bob: gentle sine wave (-4dp ↔ +4dp every 3s)
        bobAnimator = ValueAnimator.ofFloat(0f, 1f);
        bobAnimator.setDuration(3000);
        bobAnimator.setRepeatCount(ValueAnimator.INFINITE);
        bobAnimator.setInterpolator(new LinearInterpolator());
        bobAnimator.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            bobOffset = (float) (Math.sin(t * Math.PI * 2) * dpToPx(4));
        });
        bobAnimator.start();

        // 3) Ring glow + color cycling
        ringAnimator = ValueAnimator.ofFloat(0f, 1f);
        ringAnimator.setDuration(1800);
        ringAnimator.setRepeatCount(ValueAnimator.INFINITE);
        ringAnimator.setInterpolator(new LinearInterpolator());
        ringAnimator.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            ringGlow = (float) (Math.sin(t * Math.PI * 2) * 0.5 + 0.5);
            currentRingColorIdx = (int) (t * (ringColors.length - 1)) % ringColors.length;
        });
        ringAnimator.start();

        // 4) Particle emitter — spawn a new particle every ~250ms
        particleTicker = ValueAnimator.ofInt(0, 1);
        particleTicker.setDuration(250);
        particleTicker.setRepeatCount(ValueAnimator.INFINITE);
        particleTicker.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(Animator animation) {
                spawnParticle();
            }
        });
        particleTicker.addUpdateListener(a -> updateAndInvalidate());
        particleTicker.start();
    }

    public void stopAnimating() {
        animating = false;
        if (pulseAnimator != null) pulseAnimator.cancel();
        if (bobAnimator != null) bobAnimator.cancel();
        if (ringAnimator != null) ringAnimator.cancel();
        if (particleTicker != null) particleTicker.cancel();
        particles.clear();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimating();
    }

    private void updateAndInvalidate() {
        // Update particle positions
        long now = System.currentTimeMillis();
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            float age = (now - p.birthTime) / 1000f;
            if (age > p.lifetime) {
                particles.remove(i);
                continue;
            }
            p.x += p.vx;
            p.y += p.vy;
            p.vy -= 0.1f; // gravity upward (floating up)
            p.alpha = 1f - (age / p.lifetime);
        }
        invalidate();
    }

    private void spawnParticle() {
        if (!animating) return;
        if (getWidth() == 0) return;

        Particle p = new Particle();
        p.emoji = getParticleEmojiForMood(mood);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = getAvatarRadius();
        // Spawn around the edge of the avatar at random angle
        double angle = random.nextDouble() * Math.PI * 2;
        p.x = (float) (cx + r * Math.cos(angle));
        p.y = (float) (cy + r * Math.sin(angle));
        p.vx = (random.nextFloat() - 0.5f) * 1.5f;
        p.vy = -(random.nextFloat() * 1.5f + 1f);
        p.size = dpToPx(14 + random.nextInt(6));
        p.lifetime = 1.5f + random.nextFloat();
        p.birthTime = System.currentTimeMillis();
        p.alpha = 1f;
        particles.add(p);

        // Cap particle count
        if (particles.size() > 16) particles.remove(0);
    }

    private String getParticleEmojiForMood(String mood) {
        switch (mood.toLowerCase()) {
            case "romantic": return "❤️";
            case "chill": return "✨";
            case "adventure": return "🧭";
            case "party": return "🎉";
            case "coffee": return "☕";
            case "foodie": return "🍽️";
            default: return "✨";
        }
    }

    private float getAvatarRadius() {
        return Math.min(getWidth(), getHeight()) / 2f * 0.78f;
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        if (w == 0 || h == 0) return;

        float cx = w / 2f;
        float cy = h / 2f + bobOffset;
        float baseRadius = getAvatarRadius();
        float scaledRadius = baseRadius * pulseScale;

        // 1) Soft outer glow (radial gradient)
        float glowRadius = scaledRadius * 1.35f;
        int glowColor = adjustAlpha(ringColors[currentRingColorIdx], (int) (60 * ringGlow + 30));
        glowPaint.setShader(new RadialGradient(cx, cy, glowRadius,
                glowColor, 0x00000000, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, glowRadius, glowPaint);

        // 2) Animated ring with gradient color
        ringPaint.setColor(ringColors[currentRingColorIdx]);
        ringPaint.setAlpha((int) (255 * (0.6f + 0.4f * ringGlow)));
        ringPaint.setStrokeWidth(dpToPx(4 + 2 * ringGlow));
        canvas.drawCircle(cx, cy, scaledRadius + dpToPx(6), ringPaint);

        // 3) Inner white border (separates ring from photo)
        Paint whiteBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteBorder.setColor(Color.WHITE);
        whiteBorder.setStyle(Paint.Style.STROKE);
        whiteBorder.setStrokeWidth(dpToPx(2));
        canvas.drawCircle(cx, cy, scaledRadius, whiteBorder);

        // 4) Avatar photo (circular, scaled)
        if (avatarBitmap != null && shader != null) {
            shaderMatrix.reset();
            float scale = (scaledRadius * 2f) / avatarBitmap.getWidth();
            shaderMatrix.setScale(scale, scale);
            shaderMatrix.postTranslate(cx - scaledRadius, cy - scaledRadius);
            shader.setLocalMatrix(shaderMatrix);
            canvas.drawCircle(cx, cy, scaledRadius - dpToPx(1), imagePaint);
        } else {
            // Placeholder gradient circle if no avatar
            Paint placeholder = new Paint(Paint.ANTI_ALIAS_FLAG);
            placeholder.setShader(new RadialGradient(cx, cy, scaledRadius,
                    0xFFFFB6C1, 0xFFE63946, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, scaledRadius, placeholder);

            // Draw a smiley emoji
            Paint emojiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            emojiPaint.setTextAlign(Paint.Align.CENTER);
            emojiPaint.setTextSize(scaledRadius * 0.9f);
            canvas.drawText("😊", cx, cy + scaledRadius * 0.3f, emojiPaint);
        }

        // 5) Floating particles (drawn on top)
        for (Particle p : particles) {
            particlePaint.setTextSize(p.size);
            particlePaint.setAlpha((int) (255 * p.alpha));
            canvas.drawText(p.emoji, p.x, p.y, particlePaint);
        }
    }

    private int adjustAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static class Particle {
        String emoji;
        float x, y;
        float vx, vy;
        float size;
        float alpha;
        float lifetime;
        long birthTime;
    }
}
