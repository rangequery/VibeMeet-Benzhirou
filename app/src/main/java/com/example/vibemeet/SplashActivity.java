package com.example.vibemeet;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibemeet.services.UserPreferencesService;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 1800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splashLogo);
        TextView appName = findViewById(R.id.splashAppName);
        TextView tagline = findViewById(R.id.splashTagline);

        // Animations
        logo.setAlpha(0f);
        logo.setScaleX(0.5f);
        logo.setScaleY(0.5f);
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(900)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        appName.setAlpha(0f);
        appName.setTranslationY(40f);
        appName.animate().alpha(1f).translationY(0f)
                .setDuration(700)
                .setStartDelay(500)
                .start();

        tagline.setAlpha(0f);
        tagline.animate().alpha(1f)
                .setDuration(700)
                .setStartDelay(900)
                .start();

        new Handler().postDelayed(this::navigateNext, SPLASH_DURATION);
    }

    private void navigateNext() {
        UserPreferencesService prefs = new UserPreferencesService(this);

        Intent intent;
        boolean hasAccount = !prefs.getUserEmail().isEmpty() || !prefs.getUserName().equals("Explorer");
        boolean onboardingDone = prefs.isOnboardingCompleted();

        if (hasAccount && onboardingDone) {
            // Returning user → straight to main
            intent = new Intent(this, MainActivity.class);
        } else if (hasAccount) {
            // Has account but didn't finish onboarding
            intent = new Intent(this, OnboardingActivity.class);
        } else {
            // New user → login
            intent = new Intent(this, LoginActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
