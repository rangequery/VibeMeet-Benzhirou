package com.example.vibemeet.models;

/**
 * The 18 most useful MediaPipe blendshapes for a Bitmoji-style avatar.
 *
 * MediaPipe Face Landmarker outputs 52 blendshape coefficients (0-1) — the same
 * ARKit-compatible set Apple uses for Memoji. We grab the ones that drive visible
 * expressions and feed them to the avatar renderer.
 */
public class BlendshapeState {

    // Jaw / mouth
    public float jawOpen = 0f;            // 0 = closed, 1 = wide open
    public float mouthSmileLeft = 0f;
    public float mouthSmileRight = 0f;
    public float mouthFrownLeft = 0f;
    public float mouthFrownRight = 0f;
    public float mouthPucker = 0f;        // pursed lips
    public float mouthFunnel = 0f;        // "oh" shape

    // Eyes
    public float eyeBlinkLeft = 0f;       // 0 = open, 1 = closed
    public float eyeBlinkRight = 0f;
    public float eyeLookUpLeft = 0f;
    public float eyeLookUpRight = 0f;
    public float eyeLookDownLeft = 0f;
    public float eyeLookDownRight = 0f;
    public float eyeWideLeft = 0f;
    public float eyeWideRight = 0f;

    // Eyebrows
    public float browInnerUp = 0f;        // both brows up (surprise)
    public float browDownLeft = 0f;
    public float browDownRight = 0f;
    public float browOuterUpLeft = 0f;
    public float browOuterUpRight = 0f;

    // Cheeks
    public float cheekPuff = 0f;

    // Head rotation (degrees, from facial transformation matrix or ML kit Euler)
    public float headPitch = 0f;          // up/down
    public float headYaw = 0f;            // left/right
    public float headRoll = 0f;           // tilt

    public boolean faceDetected = false;

    // Convenience: average smile
    public float smile() {
        return (mouthSmileLeft + mouthSmileRight) / 2f;
    }

    public float frown() {
        return (mouthFrownLeft + mouthFrownRight) / 2f;
    }
}
