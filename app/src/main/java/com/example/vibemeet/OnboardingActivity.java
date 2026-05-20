package com.example.vibemeet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibemeet.services.UserPreferencesService;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class OnboardingActivity extends AppCompatActivity {

    private TextView stepIndicator;
    private LinearLayout step1, step2, step3;
    private EditText nameInput;
    private LinearLayout interestChips;
    private RadioGroup budgetGroup;
    private Button btnNext, btnBack;

    private int currentStep = 1;
    private final int TOTAL_STEPS = 3;
    private Set<String> selectedInterests = new HashSet<>();
    private boolean editMode = false;

    private final String[] INTERESTS = {
            "Coffee", "Food", "Nightlife", "Outdoor",
            "Culture", "Shopping", "Art", "Music",
            "Beach", "Sports", "Family", "Photography"
    };

    private UserPreferencesService prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        prefs = new UserPreferencesService(this);
        editMode = prefs.isOnboardingCompleted();

        stepIndicator = findViewById(R.id.stepIndicator);

        TextView headerTitle = findViewById(R.id.headerTitle);
        if (editMode) headerTitle.setText("Edit your preferences");

        step1 = findViewById(R.id.step1);
        step2 = findViewById(R.id.step2);
        step3 = findViewById(R.id.step3);
        nameInput = findViewById(R.id.nameInput);
        interestChips = findViewById(R.id.interestChips);
        budgetGroup = findViewById(R.id.budgetGroup);
        btnNext = findViewById(R.id.btnNext);
        btnBack = findViewById(R.id.btnBack);

        // Pre-fill with existing name
        nameInput.setText(prefs.getUserName().equals("Explorer") ? "" : prefs.getUserName());

        // In edit mode, pre-select existing interests
        if (editMode) {
            selectedInterests.addAll(prefs.getInterests());
        }

        setupInterestChips();
        showStep(1);

        btnNext.setOnClickListener(v -> {
            if (currentStep == 1) {
                String name = nameInput.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(this, "Tell us your name", Toast.LENGTH_SHORT).show();
                    return;
                }
                prefs.setUserName(name);
                showStep(2);
            } else if (currentStep == 2) {
                if (selectedInterests.isEmpty()) {
                    Toast.makeText(this, "Pick at least one interest!", Toast.LENGTH_SHORT).show();
                    return;
                }
                prefs.setInterests(selectedInterests);
                showStep(3);
            } else {
                int budget = 2;
                int selectedId = budgetGroup.getCheckedRadioButtonId();
                if (selectedId == R.id.budget_low) budget = 1;
                else if (selectedId == R.id.budget_high) budget = 3;
                prefs.setBudgetPreference(budget);
                prefs.setOnboardingCompleted(true);

                if (editMode) {
                    Toast.makeText(this, "Preferences updated", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "All set. Let's explore Casablanca", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        });

        btnBack.setOnClickListener(v -> {
            if (currentStep > 1) {
                showStep(currentStep - 1);
            } else if (editMode) {
                // Edit mode → just go back without saving
                finish();
            } else {
                // First-time skip → mark done and go to main
                prefs.setOnboardingCompleted(true);
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void setupInterestChips() {
        interestChips.removeAllViews();

        LinearLayout currentRow = null;
        int chipsPerRow = 3;

        for (int i = 0; i < INTERESTS.length; i++) {
            if (i % chipsPerRow == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, 4, 0, 4);
                currentRow.setLayoutParams(rowParams);
                interestChips.addView(currentRow);
            }

            String interest = INTERESTS[i];
            String cleanCheck = interest.replaceAll("[^\\x00-\\x7F]+", "").trim();
            boolean preSelected = selectedInterests.contains(cleanCheck);

            Button chip = new Button(this);
            chip.setText(interest);
            chip.setTextSize(13);
            chip.setAllCaps(false);
            chip.setMinHeight(dpToPx(42));
            chip.setBackgroundResource(preSelected ? R.drawable.chip_selected_bg : R.drawable.chip_unselected_bg);
            chip.setTextColor(preSelected ? 0xFFFFFFFF : 0xFF64748B);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f);
            params.setMargins(4, 4, 4, 4);
            chip.setLayoutParams(params);

            String cleanInterest = interest.replaceAll("[^\\x00-\\x7F]+", "").trim();
            chip.setOnClickListener(v -> {
                if (selectedInterests.contains(cleanInterest)) {
                    selectedInterests.remove(cleanInterest);
                    chip.setBackgroundResource(R.drawable.chip_unselected_bg);
                    chip.setTextColor(0xFF64748B);
                } else {
                    selectedInterests.add(cleanInterest);
                    chip.setBackgroundResource(R.drawable.chip_selected_bg);
                    chip.setTextColor(0xFFFFFFFF);
                }
            });

            currentRow.addView(chip);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void showStep(int step) {
        currentStep = step;
        stepIndicator.setText("Step " + step + " of " + TOTAL_STEPS);

        step1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        step2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        step3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);

        if (editMode) {
            btnBack.setText(step == 1 ? "Cancel" : "Back");
            btnNext.setText(step == TOTAL_STEPS ? "Save changes" : "Next");
        } else {
            btnBack.setText(step == 1 ? "Skip" : "Back");
            btnNext.setText(step == TOTAL_STEPS ? "Get started" : "Next");
        }
    }
}
