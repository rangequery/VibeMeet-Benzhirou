package com.example.vibemeet;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class EssentialsActivity extends AppCompatActivity {
    private LinearLayout emergencyContent, currencyContent, phrasesContent, transportContent;
    private TextView emergencyTitle, currencyTitle, phrasesTitle, transportTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_essentials);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        emergencyTitle = findViewById(R.id.emergencyTitle);
        emergencyContent = findViewById(R.id.emergencyContent);
        currencyTitle = findViewById(R.id.currencyTitle);
        currencyContent = findViewById(R.id.currencyContent);
        phrasesTitle = findViewById(R.id.phrasesTitle);
        phrasesContent = findViewById(R.id.phrasesContent);
        transportTitle = findViewById(R.id.transportTitle);
        transportContent = findViewById(R.id.transportContent);

        setupEmergencySection();
        setupCurrencySection();
        setupPhrasesSection();
        setupTransportSection();

        emergencyTitle.setOnClickListener(v -> toggleSection(emergencyContent));
        currencyTitle.setOnClickListener(v -> toggleSection(currencyContent));
        phrasesTitle.setOnClickListener(v -> toggleSection(phrasesContent));
        transportTitle.setOnClickListener(v -> toggleSection(transportContent));
    }

    private void setupEmergencySection() {
        emergencyContent.removeAllViews();
        addEmergencyItem("Police", "19");
        addEmergencyItem("Ambulance / SAMU", "15");
        addEmergencyItem("Fire Department", "15");
        addEmergencyItem("Tourist Police", "+212 5-22-24-49-00");
        addEmergencyItem("SOS Médecins", "+212 5-22-98-98-98");
    }

    private void addEmergencyItem(String name, String number) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(0, 12, 0, 12);

        TextView nameText = new TextView(this);
        nameText.setText(name);
        nameText.setTextSize(16);
        nameText.setTextColor(getColor(R.color.text_primary));
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
        nameParams.weight = 1;
        nameText.setLayoutParams(nameParams);

        TextView numberText = new TextView(this);
        numberText.setText(number);
        numberText.setTextSize(16);
        numberText.setTextColor(getColor(R.color.brand_primary));
        numberText.setTypeface(null, android.graphics.Typeface.BOLD);

        item.addView(nameText);
        item.addView(numberText);
        emergencyContent.addView(item);
    }

    private void setupCurrencySection() {
        currencyContent.removeAllViews();

        TextView ratesText = new TextView(this);
        ratesText.setText("1 EUR ≈ 10.85 MAD\n1 USD ≈ 9.95 MAD");
        ratesText.setTextSize(14);
        ratesText.setTextColor(getColor(R.color.text_primary));
        ratesText.setLineSpacing(4, 1f);
        ratesText.setPadding(0, 12, 0, 12);
        currencyContent.addView(ratesText);

        TextView noteText = new TextView(this);
        noteText.setText("Rates are approximate. Tap to open a currency converter website.");
        noteText.setTextSize(12);
        noteText.setTextColor(getColor(R.color.text_secondary));
        noteText.setClickable(true);
        noteText.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://xe.com/currency_charts/MAD_overview/"));
            startActivity(browserIntent);
        });
        currencyContent.addView(noteText);
    }

    private void setupPhrasesSection() {
        phrasesContent.removeAllViews();

        String[] phrases = {
            "Shhal? = How much?",
            "Bghit... = I want...",
            "Shukran = Thank you",
            "La = No",
            "Iyeh = Yes",
            "Fin kayn...? = Where is...?",
            "Waqash katsmi hada? = How do you say this?",
            "B'saha! = Cheers! / Bon appétit!"
        };

        for (String phrase : phrases) {
            TextView phraseText = new TextView(this);
            phraseText.setText(phrase);
            phraseText.setTextSize(14);
            phraseText.setTextColor(getColor(R.color.text_primary));
            phraseText.setPadding(0, 8, 0, 8);
            phrasesContent.addView(phraseText);
        }
    }

    private void setupTransportSection() {
        transportContent.removeAllViews();

        addTransportItem("Casatram Line 1", "Sidi Moumen → Ain Diab");
        addTransportItem("Casatram Line 2", "Hassan II Mosque → Nouzha");
        addTransportItem("Petit Taxi", "Fixed meter rate, usually <30 MAD");
        addTransportItem("Grand Taxi", "To Rabat: 80-100 MAD");
        addTransportItem("Taxi Apps", "Careem, inDriver");
        addTransportItem("Train (ONCF)", "Casa-Port, Casa-Voyageurs stations");
    }

    private void addTransportItem(String name, String detail) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(0, 12, 0, 12);

        TextView nameText = new TextView(this);
        nameText.setText(name);
        nameText.setTextSize(14);
        nameText.setTextColor(getColor(R.color.text_primary));
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView detailText = new TextView(this);
        detailText.setText(detail);
        detailText.setTextSize(12);
        detailText.setTextColor(getColor(R.color.text_secondary));
        detailText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        item.addView(nameText);
        item.addView(detailText);
        transportContent.addView(item);
    }

    private void toggleSection(LinearLayout content) {
        if (content.getVisibility() == View.VISIBLE) {
            content.setVisibility(View.GONE);
        } else {
            content.setVisibility(View.VISIBLE);
        }
    }
}
