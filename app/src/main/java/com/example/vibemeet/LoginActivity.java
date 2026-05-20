package com.example.vibemeet;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibemeet.services.UserPreferencesService;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton, guestButton;
    private UserPreferencesService prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = new UserPreferencesService(this);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        guestButton = findViewById(R.id.guestButton);
        TextView registerLink = findViewById(R.id.registerLink);

        registerLink.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 4) {
                Toast.makeText(this, "Password too short", Toast.LENGTH_SHORT).show();
                return;
            }

            loginUser(email);
        });

        guestButton.setOnClickListener(v -> {
            // Continue as guest with a friendly default name
            prefs.setUserName("Guest");
            prefs.setUserEmail("guest@vibemeet.app");
            Toast.makeText(this, "Welcome, Guest", Toast.LENGTH_SHORT).show();
            goToOnboarding();
        });
    }

    private void loginUser(String email) {
        String userName = email.split("@")[0];
        // Capitalize first letter
        if (!userName.isEmpty()) {
            userName = userName.substring(0, 1).toUpperCase() + userName.substring(1);
        }

        prefs.setUserName(userName);
        prefs.setUserEmail(email);

        Toast.makeText(this, "Welcome, " + userName, Toast.LENGTH_SHORT).show();
        goToOnboarding();
    }

    private void goToOnboarding() {
        Intent intent;
        if (!prefs.isOnboardingCompleted()) {
            intent = new Intent(this, OnboardingActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
