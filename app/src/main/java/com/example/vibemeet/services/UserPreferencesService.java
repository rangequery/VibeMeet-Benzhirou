package com.example.vibemeet.services;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class UserPreferencesService {
    private static final String PREFS_NAME = "VibeMeetUserPrefs";
    private static final String KEY_INTERESTS = "user_interests";
    private static final String KEY_MOOD = "current_mood";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_BUDGET = "budget_preference";
    private static final String KEY_USERNAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_AVATAR_3D_PATH = "avatar_3d_glb_path";

    private final SharedPreferences prefs;

    public UserPreferencesService(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isOnboardingCompleted() {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void setOnboardingCompleted(boolean completed) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, completed).apply();
    }

    public Set<String> getInterests() {
        return new HashSet<>(prefs.getStringSet(KEY_INTERESTS, new HashSet<>()));
    }

    public void setInterests(Set<String> interests) {
        prefs.edit().putStringSet(KEY_INTERESTS, interests).apply();
    }

    public String getCurrentMood() {
        return prefs.getString(KEY_MOOD, "neutral");
    }

    public void setCurrentMood(String mood) {
        prefs.edit().putString(KEY_MOOD, mood).apply();
    }

    public int getBudgetPreference() {
        return prefs.getInt(KEY_BUDGET, 2); // 1=$ 2=$$ 3=$$$
    }

    public void setBudgetPreference(int budget) {
        prefs.edit().putInt(KEY_BUDGET, budget).apply();
    }

    public String getUserName() {
        return prefs.getString(KEY_USERNAME, "Explorer");
    }

    public void setUserName(String name) {
        prefs.edit().putString(KEY_USERNAME, name).apply();
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public void setUserEmail(String email) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply();
    }

    /** Local path to the user's personalized 3D twin GLB file (from Avatar SDK). */
    public String getAvatar3dGlbPath() {
        return prefs.getString(KEY_AVATAR_3D_PATH, null);
    }

    public void setAvatar3dGlbPath(String path) {
        prefs.edit().putString(KEY_AVATAR_3D_PATH, path).apply();
    }

    public boolean has3dAvatar() {
        String path = getAvatar3dGlbPath();
        if (path == null) return false;
        return new java.io.File(path).exists();
    }

    public String getPreferencesAsContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("User Profile:\n");
        sb.append("- Name: ").append(getUserName()).append("\n");
        sb.append("- Current mood: ").append(getCurrentMood()).append("\n");
        sb.append("- Budget preference: ").append(getBudgetSymbol(getBudgetPreference())).append("\n");
        Set<String> interests = getInterests();
        if (!interests.isEmpty()) {
            sb.append("- Interests: ").append(String.join(", ", interests)).append("\n");
        }
        return sb.toString();
    }

    private String getBudgetSymbol(int level) {
        switch (level) {
            case 1: return "$ (budget-friendly)";
            case 2: return "$$ (mid-range)";
            case 3: return "$$$ (premium)";
            default: return "any";
        }
    }
}
