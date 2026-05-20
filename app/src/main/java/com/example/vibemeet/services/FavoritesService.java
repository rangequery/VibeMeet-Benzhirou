package com.example.vibemeet.services;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class FavoritesService {
    private static final String PREFS_NAME = "VibeMeetFavorites";
    private static final String KEY_FAVORITES = "favorite_venue_ids";

    private final SharedPreferences prefs;

    public FavoritesService(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isFavorite(String venueId) {
        if (venueId == null) return false;
        Set<String> favorites = prefs.getStringSet(KEY_FAVORITES, new HashSet<>());
        return favorites.contains(venueId);
    }

    public void toggleFavorite(String venueId) {
        if (venueId == null) return;
        Set<String> favorites = new HashSet<>(prefs.getStringSet(KEY_FAVORITES, new HashSet<>()));
        if (favorites.contains(venueId)) {
            favorites.remove(venueId);
        } else {
            favorites.add(venueId);
        }
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply();
    }

    public void addFavorite(String venueId) {
        if (venueId == null) return;
        Set<String> favorites = new HashSet<>(prefs.getStringSet(KEY_FAVORITES, new HashSet<>()));
        favorites.add(venueId);
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply();
    }

    public void removeFavorite(String venueId) {
        if (venueId == null) return;
        Set<String> favorites = new HashSet<>(prefs.getStringSet(KEY_FAVORITES, new HashSet<>()));
        favorites.remove(venueId);
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply();
    }

    public Set<String> getFavoriteIds() {
        return new HashSet<>(prefs.getStringSet(KEY_FAVORITES, new HashSet<>()));
    }

    public int getFavoriteCount() {
        return prefs.getStringSet(KEY_FAVORITES, new HashSet<>()).size();
    }
}
