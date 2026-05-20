package com.example.vibemeet;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibemeet.models.Venue;
import com.example.vibemeet.services.OpenAIService;
import com.example.vibemeet.services.LocationService;
import com.example.vibemeet.services.UserPreferencesService;
import com.example.vibemeet.services.VenueService;

import java.util.List;

/**
 * AI-powered Itinerary Generator
 * User describes their day → Claude builds a custom plan using real Casablanca venues
 */
public class ItineraryActivity extends AppCompatActivity {

    private EditText queryInput;
    private Button generateBtn, clearBtn;
    private TextView resultText;
    private ProgressBar loading;
    private LinearLayout suggestionsRow;
    private ScrollView resultScroll;

    private OpenAIService claude;
    private VenueService venueService;
    private LocationService locationService;
    private UserPreferencesService prefs;

    private double currentLat = VenueService.CASABLANCA_LAT;
    private double currentLon = VenueService.CASABLANCA_LNG;

    private static final String API_KEY = BuildConfig.OPENAI_API_KEY;

    private static final String[] PRESET_QUERIES = {
            "Plan me a romantic evening",
            "First time in Casablanca - 1 day plan",
            "Family Sunday with kids",
            "Best food tour for a foodie",
            "Solo cultural exploration",
            "Budget day under 200 MAD"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itinerary);

        queryInput = findViewById(R.id.queryInput);
        generateBtn = findViewById(R.id.generateBtn);
        clearBtn = findViewById(R.id.clearBtn);
        resultText = findViewById(R.id.resultText);
        loading = findViewById(R.id.loading);
        suggestionsRow = findViewById(R.id.suggestionsRow);
        resultScroll = findViewById(R.id.resultScroll);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        claude = new OpenAIService(API_KEY);
        venueService = new VenueService();
        locationService = new LocationService(this);
        prefs = new UserPreferencesService(this);

        resultText.setMovementMethod(LinkMovementMethod.getInstance());

        setupPresetChips();
        loadUserLocation();

        generateBtn.setOnClickListener(v -> {
            String query = queryInput.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(this, "Describe what kind of plan you want!", Toast.LENGTH_SHORT).show();
                return;
            }
            generateItinerary(query);
        });

        clearBtn.setOnClickListener(v -> {
            queryInput.setText("");
            resultText.setText("");
            suggestionsRow.setVisibility(View.VISIBLE);
        });
    }

    private void loadUserLocation() {
        locationService.getCurrentLocation(new LocationService.LocationCallback() {
            @Override
            public void onLocationReceived(double latitude, double longitude) {
                currentLat = latitude;
                currentLon = longitude;
            }

            @Override
            public void onError(String error) {
                // Use Casablanca default
            }
        });
    }

    private void setupPresetChips() {
        suggestionsRow.removeAllViews();
        for (String preset : PRESET_QUERIES) {
            Button chip = new Button(this);
            chip.setText(preset);
            chip.setTextSize(11);
            chip.setAllCaps(false);
            chip.setBackgroundResource(R.drawable.suggestion_chip_bg);
            chip.setTextColor(0xFFE63946);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(6, 4, 6, 4);
            chip.setLayoutParams(params);

            chip.setOnClickListener(v -> {
                queryInput.setText(preset);
                generateItinerary(preset);
            });
            suggestionsRow.addView(chip);
        }
    }

    private void generateItinerary(String userQuery) {
        loading.setVisibility(View.VISIBLE);
        generateBtn.setEnabled(false);
        resultText.setText("");
        suggestionsRow.setVisibility(View.GONE);

        // Build venue catalog for Claude
        String venueCatalog = buildVenueCatalog();

        String fullPrompt =
                "Based on the user's request and the venue catalog below, create a detailed itinerary " +
                "for Casablanca. " + prefs.getPreferencesAsContext() + "\n\n" +
                "User's request: " + userQuery + "\n\n" +
                "Available venues in Casablanca:\n" + venueCatalog + "\n\n" +
                "Create a clear itinerary with:\n" +
                "- 3-6 specific venues from the catalog above (use EXACT names)\n" +
                "- Suggested time for each stop\n" +
                "- Brief reason why each place\n" +
                "- Estimated total budget in MAD\n" +
                "- Practical tip at the end\n\n" +
                "Format as: '⏰ TIME - VENUE NAME (district)\\nReason' for each stop. Be concise.";

        new Thread(() -> {
            try {
                claude.clearHistory(); // fresh context per itinerary
                String response = claude.sendMessage(fullPrompt);
                runOnUiThread(() -> {
                    resultText.setText(response);
                    loading.setVisibility(View.GONE);
                    generateBtn.setEnabled(true);
                    resultScroll.post(() -> resultScroll.scrollTo(0, 0));
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    resultText.setText("Sorry, couldn't generate the plan. Error: " + e.getMessage());
                    loading.setVisibility(View.GONE);
                    generateBtn.setEnabled(true);
                });
            }
        }).start();
    }

    private String buildVenueCatalog() {
        StringBuilder sb = new StringBuilder();
        List<Venue> all = venueService.getAllVenuesSortedByDistance(currentLat, currentLon);
        for (Venue v : all) {
            sb.append("- ").append(v.getName())
              .append(" [").append(v.getType()).append("]")
              .append(" in ").append(v.getDistrict())
              .append(" - ").append(v.getPriceLevelText())
              .append(" - rating ").append(v.getRating())
              .append(" - ").append(v.getDescription())
              .append("\n");
        }
        return sb.toString();
    }
}
