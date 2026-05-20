package com.example.vibemeet.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Inline speech recognition — no popup, live transcript.
 *
 * Tap mic → starts listening immediately.
 * As you speak, partial results stream back via {@link Listener#onPartialResult}.
 * When you stop, the final transcript is delivered via {@link Listener#onFinalResult}.
 *
 * Handles error states and amplitude updates for a pulsing mic animation.
 */
public class VoiceInputManager {

    private static final String TAG = "VoiceInputManager";

    public interface Listener {
        void onReadyForSpeech();
        void onPartialResult(String text);
        void onFinalResult(String text);
        void onAmplitudeChanged(float rms);  // -2 to 10 typical range
        void onError(String message);
        void onEndOfSpeech();
    }

    private final Context context;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private Listener listener;
    private StringBuilder accumulatedText = new StringBuilder();

    public VoiceInputManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean isAvailable() {
        return SpeechRecognizer.isRecognitionAvailable(context);
    }

    public boolean isListening() {
        return isListening;
    }

    public void start(Listener listener) {
        if (isListening) {
            stop();
            return;
        }
        this.listener = listener;
        accumulatedText.setLength(0);

        if (!isAvailable()) {
            listener.onError("Speech recognition not available on this device");
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(recognitionListener);

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);

        try {
            speechRecognizer.startListening(intent);
            isListening = true;
        } catch (Exception e) {
            listener.onError("Failed to start recognition: " + e.getMessage());
            cleanup();
        }
    }

    public void stop() {
        if (speechRecognizer != null && isListening) {
            try {
                speechRecognizer.stopListening();
            } catch (Exception ignored) {}
        }
        cleanup();
    }

    public void cancel() {
        if (speechRecognizer != null) {
            try {
                speechRecognizer.cancel();
            } catch (Exception ignored) {}
        }
        cleanup();
    }

    private void cleanup() {
        isListening = false;
        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {}
            speechRecognizer = null;
        }
    }

    private final RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            if (listener != null) listener.onReadyForSpeech();
        }

        @Override
        public void onBeginningOfSpeech() {}

        @Override
        public void onRmsChanged(float rmsdB) {
            if (listener != null) listener.onAmplitudeChanged(rmsdB);
        }

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {
            if (listener != null) listener.onEndOfSpeech();
        }

        @Override
        public void onError(int error) {
            String msg = errorMessage(error);
            Log.w(TAG, "Recognition error " + error + ": " + msg);
            if (listener != null) listener.onError(msg);
            cleanup();
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String text = matches.get(0);
                if (listener != null) listener.onFinalResult(text);
            } else if (accumulatedText.length() > 0) {
                if (listener != null) listener.onFinalResult(accumulatedText.toString());
            } else {
                if (listener != null) listener.onError("Didn't catch that — try again");
            }
            cleanup();
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> matches = partialResults.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String text = matches.get(0);
                accumulatedText.setLength(0);
                accumulatedText.append(text);
                if (listener != null) listener.onPartialResult(text);
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {}
    };

    private String errorMessage(int code) {
        switch (code) {
            case SpeechRecognizer.ERROR_AUDIO: return "Audio error";
            case SpeechRecognizer.ERROR_CLIENT: return "Client error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Microphone permission denied";
            case SpeechRecognizer.ERROR_NETWORK: return "Network error — voice recognition needs internet";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH: return "Didn't catch that — try speaking clearly";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Recognizer busy — wait a moment";
            case SpeechRecognizer.ERROR_SERVER: return "Recognition server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No speech detected";
            default: return "Recognition failed";
        }
    }
}
