package com.example.vibemeet.services;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VisitHistoryService {
    private static final String PREFS_NAME = "VibeMeetVisits";
    private static final String KEY_VISITS = "visits_json";

    private final SharedPreferences prefs;

    public static class Visit {
        public String venueId;
        public String venueName;
        public String venueType;
        public long timestamp;

        public Visit(String id, String name, String type, long timestamp) {
            this.venueId = id;
            this.venueName = name;
            this.venueType = type;
            this.timestamp = timestamp;
        }
    }

    public VisitHistoryService(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void checkIn(String venueId, String venueName, String venueType) {
        try {
            JSONArray visits = getVisitsArray();
            JSONObject visit = new JSONObject();
            visit.put("id", venueId);
            visit.put("name", venueName);
            visit.put("type", venueType);
            visit.put("timestamp", System.currentTimeMillis());
            visits.put(visit);
            prefs.edit().putString(KEY_VISITS, visits.toString()).apply();
        } catch (Exception ignored) {}
    }

    public List<Visit> getRecentVisits(int limit) {
        List<Visit> result = new ArrayList<>();
        try {
            JSONArray arr = getVisitsArray();
            int start = Math.max(0, arr.length() - limit);
            for (int i = arr.length() - 1; i >= start; i--) {
                JSONObject o = arr.getJSONObject(i);
                result.add(new Visit(
                        o.optString("id"),
                        o.optString("name"),
                        o.optString("type"),
                        o.optLong("timestamp")));
            }
        } catch (Exception ignored) {}
        return result;
    }

    public List<Visit> getAllVisits() {
        return getRecentVisits(Integer.MAX_VALUE);
    }

    public int getTotalVisits() {
        try {
            return getVisitsArray().length();
        } catch (Exception e) {
            return 0;
        }
    }

    public Map<String, Integer> getVisitsByType() {
        Map<String, Integer> counts = new HashMap<>();
        try {
            JSONArray arr = getVisitsArray();
            for (int i = 0; i < arr.length(); i++) {
                String type = arr.getJSONObject(i).optString("type", "Unknown");
                counts.put(type, counts.getOrDefault(type, 0) + 1);
            }
        } catch (Exception ignored) {}
        return counts;
    }

    public int getUniqueVenueCount() {
        java.util.Set<String> unique = new java.util.HashSet<>();
        try {
            JSONArray arr = getVisitsArray();
            for (int i = 0; i < arr.length(); i++) {
                unique.add(arr.getJSONObject(i).optString("id"));
            }
        } catch (Exception ignored) {}
        return unique.size();
    }

    public String getFavoriteType() {
        Map<String, Integer> counts = getVisitsByType();
        String top = "None yet";
        int max = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                top = e.getKey();
            }
        }
        return top;
    }

    public void clearHistory() {
        prefs.edit().remove(KEY_VISITS).apply();
    }

    private JSONArray getVisitsArray() throws Exception {
        String json = prefs.getString(KEY_VISITS, "[]");
        return new JSONArray(json);
    }
}
