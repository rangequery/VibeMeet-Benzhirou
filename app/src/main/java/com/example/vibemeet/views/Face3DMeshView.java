package com.example.vibemeet.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

/**
 * Renders a real-time 3D face mesh from MediaPipe Face Landmarker output.
 *
 * Takes 468 normalized 3D landmarks (x, y, z all roughly 0–1, with z being depth)
 * and renders them as:
 *   - A wireframe mesh between connected landmarks
 *   - Dots for each landmark
 *   - Color/brightness driven by z-depth for a true 3D appearance
 *
 * Looks like a sci-fi face scan that perfectly tracks your face.
 */
public class Face3DMeshView extends View {

    public static class Landmark {
        public final float x, y, z;
        public Landmark(float x, float y, float z) {
            this.x = x; this.y = y; this.z = z;
        }
    }

    /** MediaPipe Face Mesh tesselation edges — pairs of landmark indices. */
    private static final int[][] FACE_OVAL = {
            {10, 338}, {338, 297}, {297, 332}, {332, 284}, {284, 251},
            {251, 389}, {389, 356}, {356, 454}, {454, 323}, {323, 361},
            {361, 288}, {288, 397}, {397, 365}, {365, 379}, {379, 378},
            {378, 400}, {400, 377}, {377, 152}, {152, 148}, {148, 176},
            {176, 149}, {149, 150}, {150, 136}, {136, 172}, {172, 58},
            {58, 132}, {132, 93}, {93, 234}, {234, 127}, {127, 162},
            {162, 21}, {21, 54}, {54, 103}, {103, 67}, {67, 109}, {109, 10}
    };

    private static final int[][] LEFT_EYE = {
            {263, 249}, {249, 390}, {390, 373}, {373, 374}, {374, 380},
            {380, 381}, {381, 382}, {382, 362}, {362, 263}
    };

    private static final int[][] RIGHT_EYE = {
            {33, 7}, {7, 163}, {163, 144}, {144, 145}, {145, 153},
            {153, 154}, {154, 155}, {155, 133}, {133, 33}
    };

    private static final int[][] LEFT_EYEBROW = {
            {336, 296}, {296, 334}, {334, 293}, {293, 300}, {300, 276},
            {276, 283}, {283, 282}, {282, 295}, {295, 285}
    };

    private static final int[][] RIGHT_EYEBROW = {
            {70, 63}, {63, 105}, {105, 66}, {66, 107}, {107, 55},
            {55, 65}, {65, 52}, {52, 53}, {53, 46}
    };

    private static final int[][] LIPS_OUTER = {
            {61, 146}, {146, 91}, {91, 181}, {181, 84}, {84, 17},
            {17, 314}, {314, 405}, {405, 321}, {321, 375}, {375, 291},
            {291, 409}, {409, 270}, {270, 269}, {269, 267}, {267, 0},
            {0, 37}, {37, 39}, {39, 40}, {40, 185}, {185, 61}
    };

    private static final int[][] LIPS_INNER = {
            {78, 95}, {95, 88}, {88, 178}, {178, 87}, {87, 14},
            {14, 317}, {317, 402}, {402, 318}, {318, 324}, {324, 308},
            {308, 415}, {415, 310}, {310, 311}, {311, 312}, {312, 13},
            {13, 82}, {82, 81}, {81, 80}, {80, 191}, {191, 78}
    };

    private static final int[][] NOSE = {
            {1, 2}, {2, 98}, {98, 327}, {327, 1},
            {1, 4}, {4, 5}, {5, 195}, {195, 197}, {197, 6}, {6, 168}
    };

    private List<Landmark> landmarks = null;
    private boolean faceDetected = false;
    private boolean mirrorX = true; // mirror for selfie camera

    private Paint linePaint;
    private Paint dotPaint;
    private Paint backgroundPaint;
    private Paint noFacePaint;
    private Paint glowPaint;
    private Paint scanLinePaint;

    private long lastDrawTime = 0;
    private float scanLineY = 0f;

    public Face3DMeshView(Context context) {
        super(context);
        init();
    }

    public Face3DMeshView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Face3DMeshView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);

        scanLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scanLinePaint.setStyle(Paint.Style.STROKE);
        scanLinePaint.setStrokeWidth(dp(2));

        noFacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        noFacePaint.setColor(0xFF999999);
        noFacePaint.setTextAlign(Paint.Align.CENTER);
        noFacePaint.setTextSize(dp(14));
    }

    public void setMirrorX(boolean mirror) {
        this.mirrorX = mirror;
    }

    public void updateLandmarks(List<Landmark> landmarks) {
        this.landmarks = landmarks;
        this.faceDetected = landmarks != null && !landmarks.isEmpty();
        postInvalidate();
    }

    public void clearFace() {
        this.faceDetected = false;
        this.landmarks = null;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        if (w == 0 || h == 0) return;

        // Cyber background gradient
        backgroundPaint.setShader(new LinearGradient(0, 0, 0, h,
                0xFF0A0A1A, 0xFF1A0A2E, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, backgroundPaint);

        if (!faceDetected || landmarks == null || landmarks.size() < 100) {
            noFacePaint.setColor(0xFFFFFFFF);
            canvas.drawText("✨ Initializing 3D scan...", w / 2f, h / 2f, noFacePaint);
            noFacePaint.setColor(0xAAFFFFFF);
            canvas.drawText("Show your face to the camera", w / 2f, h / 2f + dp(24), noFacePaint);
            return;
        }

        // Compute landmark bounds to center & scale the mesh
        float minX = 1f, maxX = 0f, minY = 1f, maxY = 0f;
        float minZ = 1f, maxZ = -1f;
        for (Landmark lm : landmarks) {
            if (lm.x < minX) minX = lm.x;
            if (lm.x > maxX) maxX = lm.x;
            if (lm.y < minY) minY = lm.y;
            if (lm.y > maxY) maxY = lm.y;
            if (lm.z < minZ) minZ = lm.z;
            if (lm.z > maxZ) maxZ = lm.z;
        }

        float rangeX = Math.max(0.0001f, maxX - minX);
        float rangeY = Math.max(0.0001f, maxY - minY);
        float rangeZ = Math.max(0.0001f, maxZ - minZ);

        // Fit the face to the view with padding
        float padding = dp(20);
        float viewW = w - 2 * padding;
        float viewH = h - 2 * padding;
        float scale = Math.min(viewW / rangeX, viewH / rangeY) * 0.9f;
        float offsetX = (w - rangeX * scale) / 2f - minX * scale;
        float offsetY = (h - rangeY * scale) / 2f - minY * scale;

        // Animated scan line that sweeps top to bottom
        long now = System.currentTimeMillis();
        if (lastDrawTime > 0) {
            scanLineY += (now - lastDrawTime) * 0.0006f;
            if (scanLineY > 1.2f) scanLineY = -0.2f;
        }
        lastDrawTime = now;

        // ----- Draw mesh edges -----
        // Group by region for different colors
        drawConnections(canvas, FACE_OVAL, 0xFF00E5FF, dp(1.8f), landmarks, minZ, rangeZ,
                scale, offsetX, offsetY, w);
        drawConnections(canvas, LEFT_EYE, 0xFFFF6B9D, dp(2.2f), landmarks, minZ, rangeZ,
                scale, offsetX, offsetY, w);
        drawConnections(canvas, RIGHT_EYE, 0xFFFF6B9D, dp(2.2f), landmarks, minZ, rangeZ,
                scale, offsetX, offsetY, w);
        drawConnections(canvas, LEFT_EYEBROW, 0xFFFFEB3B, dp(2f), landmarks, minZ, rangeZ,
                scale, offsetX, offsetY, w);
        drawConnections(canvas, RIGHT_EYEBROW, 0xFFFFEB3B, dp(2f), landmarks, minZ, rangeZ,
                scale, offsetX, offsetY, w);
        drawConnections(canvas, LIPS_OUTER, 0xFFFF1744, dp(2.4f), landmarks, minZ, rangeZ,
                scale, offsetX, offsetY, w);
        drawConnections(canvas, LIPS_INNER, 0xFFFF1744, dp(1.6f), landmarks, minZ, rangeZ,
                scale, offsetX, offsetY, w);
        drawConnections(canvas, NOSE, 0xFF76FF03, dp(2f), landmarks, minZ, rangeZ,
                scale, offsetX, offsetY, w);

        // ----- Draw landmark dots (subsampled, depth-shaded) -----
        for (int i = 0; i < landmarks.size(); i += 3) {
            Landmark lm = landmarks.get(i);
            float px = lm.x * scale + offsetX;
            float py = lm.y * scale + offsetY;
            if (mirrorX) px = w - px;
            float depthFactor = (lm.z - minZ) / rangeZ;
            int alpha = (int) (255 * (1f - depthFactor * 0.6f));
            int size = (int) dp(1.5f + (1f - depthFactor) * 1.5f);
            // Cyan-to-magenta gradient by depth
            int color = blendColor(0xFF00E5FF, 0xFFE040FB, depthFactor);
            dotPaint.setColor(color);
            dotPaint.setAlpha(alpha);
            canvas.drawCircle(px, py, size, dotPaint);
        }

        // ----- Animated scan line -----
        float scanY = scanLineY * h;
        scanLinePaint.setShader(new LinearGradient(0, scanY - dp(20), 0, scanY + dp(20),
                new int[]{0x0000E5FF, 0xFF00E5FF, 0x0000E5FF},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawLine(0, scanY, w, scanY, scanLinePaint);

        // Keep redrawing for animation
        postInvalidateOnAnimation();
    }

    private void drawConnections(Canvas canvas, int[][] edges, int baseColor, float strokeWidth,
                                 List<Landmark> lms, float minZ, float rangeZ,
                                 float scale, float offsetX, float offsetY, float viewWidth) {
        for (int[] edge : edges) {
            if (edge[0] >= lms.size() || edge[1] >= lms.size()) continue;
            Landmark a = lms.get(edge[0]);
            Landmark b = lms.get(edge[1]);

            float ax = a.x * scale + offsetX;
            float ay = a.y * scale + offsetY;
            float bx = b.x * scale + offsetX;
            float by = b.y * scale + offsetY;

            if (mirrorX) {
                ax = viewWidth - ax;
                bx = viewWidth - bx;
            }

            // Depth shading
            float avgZ = (a.z + b.z) / 2f;
            float depthFactor = (avgZ - minZ) / rangeZ;
            int alpha = (int) (255 * (0.5f + (1f - depthFactor) * 0.5f));

            // Glow underlayer
            glowPaint.setColor(baseColor);
            glowPaint.setStrokeWidth(strokeWidth * 2.5f);
            glowPaint.setAlpha(alpha / 3);
            canvas.drawLine(ax, ay, bx, by, glowPaint);

            // Main line
            linePaint.setColor(baseColor);
            linePaint.setStrokeWidth(strokeWidth);
            linePaint.setAlpha(alpha);
            canvas.drawLine(ax, ay, bx, by, linePaint);
        }
    }

    private int blendColor(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r1 = Color.red(c1), g1 = Color.green(c1), b1 = Color.blue(c1);
        int r2 = Color.red(c2), g2 = Color.green(c2), b2 = Color.blue(c2);
        return Color.rgb(
                (int) (r1 + (r2 - r1) * t),
                (int) (g1 + (g2 - g1) * t),
                (int) (b1 + (b2 - b1) * t));
    }

    private float dp(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
