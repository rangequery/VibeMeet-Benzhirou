package com.example.vibemeet.fragments;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.vibemeet.MainActivity;
import com.example.vibemeet.R;
import com.example.vibemeet.adapters.ChatAdapter;
import com.example.vibemeet.models.ChatMessage;
import com.example.vibemeet.services.OpenAIService;
import com.example.vibemeet.services.UserPreferencesService;
import com.example.vibemeet.services.VoiceInputManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatFragment extends Fragment {

    private ListView chatListView;
    private EditText messageInput;
    private Button sendButton;
    private ImageButton voiceButton, clearButton;
    private TextView typingIndicator;
    private LinearLayout suggestionsContainer;
    private LinearLayout voiceOverlay;
    private TextView voiceTranscriptText;
    private View voiceMicCircle;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    private OpenAIService aiService;
    private UserPreferencesService prefs;
    private VoiceInputManager voiceManager;
    private ValueAnimator micPulseAnimator;

    private static final int RECORD_AUDIO_REQUEST = 200;

    private static final String API_KEY = BuildConfig.OPENAI_API_KEY;

    private final String[] SUGGESTIONS = {
            "Where should I eat tonight?",
            "Best café right now",
            "What's open near me?",
            "Plan my day in Casablanca",
            "Hidden gems in the Medina",
            "Where to watch the sunset",
            "Cheap eats nearby",
            "Rooftop bars in Maarif"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chatListView = view.findViewById(R.id.chatListView);
        messageInput = view.findViewById(R.id.messageInput);
        sendButton = view.findViewById(R.id.sendButton);
        voiceButton = view.findViewById(R.id.voiceButton);
        clearButton = view.findViewById(R.id.clearButton);
        typingIndicator = view.findViewById(R.id.typingIndicator);
        suggestionsContainer = view.findViewById(R.id.suggestionsContainer);
        voiceOverlay = view.findViewById(R.id.voiceOverlay);
        voiceTranscriptText = view.findViewById(R.id.voiceTranscriptText);
        voiceMicCircle = view.findViewById(R.id.voiceMicCircle);

        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(requireContext(), messages);
        chatListView.setAdapter(chatAdapter);

        aiService = new OpenAIService(API_KEY);
        prefs = new UserPreferencesService(requireContext());
        voiceManager = new VoiceInputManager(requireContext());

        addInitialGreeting();

        setupSuggestions();
        setupInputListeners();

        sendButton.setOnClickListener(v -> sendMessage(messageInput.getText().toString().trim()));
        voiceButton.setOnClickListener(v -> toggleVoiceInput());

        if (clearButton != null) {
            clearButton.setOnClickListener(v -> clearConversation());
        }

        // Tap on overlay backdrop to cancel
        if (voiceOverlay != null) {
            voiceOverlay.setOnClickListener(v -> cancelVoiceInput());
        }

        updateSendButtonState();
        handleInitialPrompt();
    }

    private void handleInitialPrompt() {
        Bundle args = getArguments();
        if (args == null) return;

        String prompt = args.getString(MainActivity.EXTRA_CHAT_PROMPT);
        if (prompt != null && !prompt.trim().isEmpty()) {
            args.remove(MainActivity.EXTRA_CHAT_PROMPT);
            sendMessage(prompt.trim());
        }
    }

    private void addInitialGreeting() {
        String name = prefs.getUserName();
        String greeting = "Salam " + name + "\n\n" +
                "I'm your local Casablanca guide. Ask me where to eat, what to do, hidden gems, routes, or full-day plans.\n\n" +
                "You can also use voice input instead of typing.";
        messages.add(new ChatMessage(greeting, false));
        chatAdapter.notifyDataSetChanged();
    }

    private void setupInputListeners() {
        messageInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateSendButtonState(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage(messageInput.getText().toString().trim());
                return true;
            }
            return false;
        });
    }

    private void updateSendButtonState() {
        boolean hasText = !messageInput.getText().toString().trim().isEmpty();
        sendButton.setAlpha(hasText ? 1f : 0.4f);
        sendButton.setEnabled(hasText);
    }

    // ==================== VOICE INPUT ====================

    private void toggleVoiceInput() {
        if (voiceManager.isListening()) {
            // Tap mic again to stop and process whatever was said
            voiceManager.stop();
            return;
        }

        // Check permission first
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_REQUEST);
            return;
        }

        if (!voiceManager.isAvailable()) {
            Toast.makeText(requireContext(),
                    "Speech recognition not available on this device. Try a real phone.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        startVoiceRecognition();
    }

    private void startVoiceRecognition() {
        showVoiceOverlay(true);
        voiceTranscriptText.setText("Listening...");
        startMicPulse();

        voiceManager.start(new VoiceInputManager.Listener() {
            @Override
            public void onReadyForSpeech() {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    voiceTranscriptText.setText("Speak now...");
                });
            }

            @Override
            public void onPartialResult(String text) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    if (text != null && !text.isEmpty()) {
                        voiceTranscriptText.setText("\"" + text + "\"");
                    }
                });
            }

            @Override
            public void onFinalResult(String text) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    stopMicPulse();
                    showVoiceOverlay(false);
                    if (text != null && !text.trim().isEmpty()) {
                        messageInput.setText(text);
                        sendMessage(text.trim());
                    } else {
                        Toast.makeText(requireContext(), "No speech detected", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onAmplitudeChanged(float rms) {
                if (getActivity() == null) return;
                // Scale the mic circle based on volume (0.9 - 1.4)
                float normalized = Math.max(0f, Math.min(1f, (rms + 2f) / 10f));
                float scale = 0.9f + normalized * 0.5f;
                requireActivity().runOnUiThread(() -> {
                    if (voiceMicCircle != null) {
                        voiceMicCircle.setScaleX(scale);
                        voiceMicCircle.setScaleY(scale);
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    stopMicPulse();
                    showVoiceOverlay(false);
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onEndOfSpeech() {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    voiceTranscriptText.setText("Processing...");
                });
            }
        });
    }

    private void cancelVoiceInput() {
        if (voiceManager.isListening()) {
            voiceManager.cancel();
        }
        stopMicPulse();
        showVoiceOverlay(false);
    }

    private void showVoiceOverlay(boolean show) {
        if (voiceOverlay != null) {
            voiceOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void startMicPulse() {
        if (voiceMicCircle == null) return;
        stopMicPulse();
        micPulseAnimator = ValueAnimator.ofFloat(1f, 1.15f);
        micPulseAnimator.setDuration(700);
        micPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        micPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        micPulseAnimator.start();
    }

    private void stopMicPulse() {
        if (micPulseAnimator != null) {
            micPulseAnimator.cancel();
            micPulseAnimator = null;
        }
        if (voiceMicCircle != null) {
            voiceMicCircle.setScaleX(1f);
            voiceMicCircle.setScaleY(1f);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognition();
            } else {
                Toast.makeText(requireContext(), "Mic permission needed for voice input", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopMicPulse();
        if (voiceManager != null) voiceManager.cancel();
    }

    // ==================== SUGGESTIONS ====================

    private void setupSuggestions() {
        suggestionsContainer.removeAllViews();
        for (String suggestion : SUGGESTIONS) {
            Button chip = new Button(requireContext());
            chip.setText(suggestion);
            chip.setTextSize(13);
            chip.setAllCaps(false);
            chip.setBackgroundResource(R.drawable.suggestion_chip_bg);
            chip.setTextColor(0xFF243B6B);
            chip.setMinHeight(0);
            chip.setMinWidth(0);
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(dp(4), dp(4), dp(4), dp(4));
            chip.setLayoutParams(params);

            String suggestionText = suggestion;
            chip.setOnClickListener(v -> {
                String cleanText = suggestionText.replaceAll("[^\\x00-\\x7F]+", "").trim();
                sendMessage(cleanText);
            });
            suggestionsContainer.addView(chip);
        }
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void clearConversation() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Clear conversation?")
                .setMessage("Start a fresh chat with the assistant.")
                .setPositiveButton("Clear", (d, w) -> {
                    aiService.clearHistory();
                    messages.clear();
                    addInitialGreeting();
                    chatAdapter.notifyDataSetChanged();
                    suggestionsContainer.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(), "Conversation cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendMessage(String userMessage) {
        if (userMessage.isEmpty()) return;

        messageInput.setText("");
        messages.add(new ChatMessage(userMessage, true));
        chatAdapter.notifyDataSetChanged();
        chatListView.setSelection(chatAdapter.getCount() - 1);

        suggestionsContainer.setVisibility(View.GONE);

        showTypingIndicator(true);
        sendButton.setEnabled(false);
        voiceButton.setEnabled(false);

        String contextMsg = userMessage;
        if (messages.size() == 2) {
            contextMsg = prefs.getPreferencesAsContext() + "\n\nUser asks: " + userMessage;
        }

        final String finalMsg = contextMsg;
        new Thread(() -> {
            try {
                String response = aiService.sendMessage(finalMsg);
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    messages.add(new ChatMessage(response, false));
                    chatAdapter.notifyDataSetChanged();
                    chatListView.setSelection(chatAdapter.getCount() - 1);
                    showTypingIndicator(false);
                    sendButton.setEnabled(true);
                    voiceButton.setEnabled(true);
                    updateSendButtonState();
                });
            } catch (Exception e) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    messages.add(new ChatMessage(getFriendlyErrorMessage(e), false));
                    chatAdapter.notifyDataSetChanged();
                    showTypingIndicator(false);
                    sendButton.setEnabled(true);
                    voiceButton.setEnabled(true);
                    updateSendButtonState();
                });
            }
        }).start();
    }

    private void showTypingIndicator(boolean show) {
        if (typingIndicator == null) return;
        typingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            animateTypingDots();
        }
    }

    private final Runnable typingTick = new Runnable() {
        int step = 0;
        @Override
        public void run() {
            if (typingIndicator == null || typingIndicator.getVisibility() != View.VISIBLE) return;
            String[] frames = {"Assistant is typing", "Assistant is typing.", "Assistant is typing..", "Assistant is typing..."};
            typingIndicator.setText(frames[step % frames.length]);
            step++;
            typingIndicator.postDelayed(this, 400);
        }
    };

    private void animateTypingDots() {
        if (typingIndicator != null) {
            typingIndicator.removeCallbacks(typingTick);
            typingIndicator.post(typingTick);
        }
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
