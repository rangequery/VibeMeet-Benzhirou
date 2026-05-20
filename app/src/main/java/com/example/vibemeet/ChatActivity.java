package com.example.vibemeet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.vibemeet.adapters.ChatAdapter;
import com.example.vibemeet.models.ChatMessage;
import com.example.vibemeet.services.OpenAIService;
import com.example.vibemeet.services.UserPreferencesService;

import java.util.ArrayList;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {
    private ListView chatListView;
    private EditText messageInput;
    private Button sendButton;
    private ImageButton voiceButton;
    private ProgressBar loadingSpinner;
    private LinearLayout suggestionsContainer;
    private ChatAdapter chatAdapter;
    private java.util.List<ChatMessage> messages;
    private OpenAIService aiService;
    private UserPreferencesService prefs;
    private ActivityResultLauncher<Intent> voiceLauncher;

    private static final int RECORD_AUDIO_REQUEST = 200;
    private static final String API_KEY = BuildConfig.OPENAI_API_KEY;

    private final String[] SUGGESTIONS = {
            "Best coffee in Casablanca? ☕",
            "Romantic dinner ideas 🌹",
            "What to do this weekend? 🎉",
            "Best beach activities? 🏖️",
            "Where to see live music? 🎵",
            "Family-friendly places? 👨‍👩‍👧",
            "Hidden gems in Old Medina? 💎",
            "Best rooftop views? 🌃"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatListView = findViewById(R.id.chatListView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        voiceButton = findViewById(R.id.voiceButton);
        loadingSpinner = findViewById(R.id.loadingSpinner);
        suggestionsContainer = findViewById(R.id.suggestionsContainer);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, messages);
        chatListView.setAdapter(chatAdapter);

        aiService = new OpenAIService(API_KEY);
        prefs = new UserPreferencesService(this);

        // Greeting personalized
        String greeting = "Marhaba " + prefs.getUserName() + "! 🇲🇦 I'm your VibeMeet guide to Casablanca. " +
                "Ask me about cafes, restaurants, beaches, activities, events, or anything about the city. " +
                "I can also speak with you - tap the 🎤 to use voice!";
        ChatMessage greetingMsg = new ChatMessage(greeting, false);
        messages.add(greetingMsg);
        chatAdapter.notifyDataSetChanged();

        setupSuggestions();
        setupVoiceLauncher();

        // Handle preset query from intent
        String presetQuery = getIntent().getStringExtra("preset_query");
        if (presetQuery != null) {
            messageInput.setText(presetQuery);
            sendMessage(presetQuery);
        }

        sendButton.setOnClickListener(v -> sendMessage(messageInput.getText().toString().trim()));
        voiceButton.setOnClickListener(v -> startVoiceInput());
    }

    private void setupVoiceLauncher() {
        voiceLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        java.util.ArrayList<String> spokenText = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (spokenText != null && !spokenText.isEmpty()) {
                            String userMsg = spokenText.get(0);
                            messageInput.setText(userMsg);
                            sendMessage(userMsg);
                        }
                    }
                });
    }

    private void startVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_REQUEST);
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Voice recognition not available on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask me about Casablanca...");

        try {
            voiceLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Voice input unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceInput();
            } else {
                Toast.makeText(this, "Voice permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupSuggestions() {
        suggestionsContainer.removeAllViews();
        for (String suggestion : SUGGESTIONS) {
            Button chip = new Button(this);
            chip.setText(suggestion);
            chip.setTextSize(11);
            chip.setAllCaps(false);
            chip.setBackgroundResource(R.drawable.suggestion_chip_bg);
            chip.setTextColor(0xFFE63946);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(6, 4, 6, 4);
            chip.setLayoutParams(params);

            String suggestionText = suggestion;
            chip.setOnClickListener(v -> {
                String cleanText = suggestionText.replaceAll("[^\\x00-\\x7F]+", "").trim();
                sendMessage(cleanText);
            });
            suggestionsContainer.addView(chip);
        }
    }

    private void sendMessage(String userMessage) {
        if (userMessage.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        messageInput.setText("");
        ChatMessage userMsg = new ChatMessage(userMessage, true);
        messages.add(userMsg);
        chatAdapter.notifyDataSetChanged();
        chatListView.setSelection(chatAdapter.getCount() - 1);

        suggestionsContainer.setVisibility(View.GONE);

        loadingSpinner.setVisibility(View.VISIBLE);
        sendButton.setEnabled(false);
        voiceButton.setEnabled(false);

        // Inject user preferences as context for first message
        String contextMsg = userMessage;
        if (messages.size() == 2) { // greeting + first user msg
            contextMsg = prefs.getPreferencesAsContext() + "\n\nUser asks: " + userMessage;
        }

        final String finalMsg = contextMsg;
        new Thread(() -> {
            try {
                String response = aiService.sendMessage(finalMsg);
                runOnUiThread(() -> {
                    ChatMessage botMsg = new ChatMessage(response, false);
                    messages.add(botMsg);
                    chatAdapter.notifyDataSetChanged();
                    chatListView.setSelection(chatAdapter.getCount() - 1);
                    loadingSpinner.setVisibility(View.GONE);
                    sendButton.setEnabled(true);
                    voiceButton.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    ChatMessage botMsg = new ChatMessage(getFriendlyErrorMessage(e), false);
                    messages.add(botMsg);
                    chatAdapter.notifyDataSetChanged();
                    loadingSpinner.setVisibility(View.GONE);
                    sendButton.setEnabled(true);
                    voiceButton.setEnabled(true);
                });
            }
        }).start();
    }

    private String getFriendlyErrorMessage(Exception e) {
        String details = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.US);
        if (details.contains("longer message")) {
            return "Please send a little more detail so I can help.";
        }
        if (details.contains("quota") || details.contains("credits")) {
            return "The assistant is temporarily unavailable because the AI account needs more credits.";
        }
        if (details.contains("api key") || details.contains("unauthorized") || details.contains("401")) {
            return "The assistant is not configured correctly yet. Please check the API key.";
        }
        return "Sorry, I couldn't respond right now. Please check your connection and try again.";
    }
}
