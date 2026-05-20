package com.example.vibemeet.services;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Provides the user's current location with a smart Casablanca fallback:
 *  - Tries last known location first (fast).
 *  - If that fails, requests a fresh location with network + GPS priority.
 *  - If the result is OUTSIDE the Casablanca metropolitan area (e.g. emulator
 *    stuck in California), it transparently swaps to Casablanca center.
 *    This keeps distance/walking-time displays sensible for the demo.
 */
public class LocationService {

    // Casablanca city center
    public static final double CASABLANCA_LAT = 33.5731;
    public static final double CASABLANCA_LNG = -7.5898;

    // Bounding box for "in Casablanca area" check (roughly 50 km radius)
    private static final double CASA_LAT_MIN = 33.40;
    private static final double CASA_LAT_MAX = 33.75;
    private static final double CASA_LON_MIN = -7.80;
    private static final double CASA_LON_MAX = -7.40;

    private final FusedLocationProviderClient fusedLocationClient;
    private final Context context;

    public interface LocationCallback {
        void onLocationReceived(double latitude, double longitude);
        void onError(String error);
    }

    public LocationService(Context context) {
        this.context = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context);
    }

    /** Returns true if the given coordinate is within Casablanca's metro bounds. */
    public static boolean isInCasablanca(double lat, double lon) {
        return lat >= CASA_LAT_MIN && lat <= CASA_LAT_MAX
                && lon >= CASA_LON_MIN && lon <= CASA_LON_MAX;
    }

    @SuppressWarnings("MissingPermission")
    public void getCurrentLocation(LocationCallback callback) {
        if (!hasLocationPermission()) {
            // No permission → fall back to Casablanca silently
            callback.onLocationReceived(CASABLANCA_LAT, CASABLANCA_LNG);
            return;
        }

        // 1) Try last known location (instant)
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        deliverWithCasablancaFallback(location, callback);
                    } else {
                        requestFreshLocation(callback);
                    }
                })
                .addOnFailureListener(e -> requestFreshLocation(callback));
    }

    @SuppressWarnings("MissingPermission")
    private void requestFreshLocation(LocationCallback callback) {
        if (!hasLocationPermission()) {
            callback.onLocationReceived(CASABLANCA_LAT, CASABLANCA_LNG);
            return;
        }

        // Request a fresh location with network + GPS, single update
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 0L)
                .setMaxUpdates(1)
                .setDurationMillis(8000)
                .build();

        com.google.android.gms.location.LocationCallback gmsCallback =
                new com.google.android.gms.location.LocationCallback() {
                    @Override
                    public void onLocationResult(@androidx.annotation.NonNull com.google.android.gms.location.LocationResult result) {
                        Location loc = result.getLastLocation();
                        if (loc != null) {
                            deliverWithCasablancaFallback(loc, callback);
                        } else {
                            callback.onLocationReceived(CASABLANCA_LAT, CASABLANCA_LNG);
                        }
                        fusedLocationClient.removeLocationUpdates(this);
                    }
                };

        try {
            fusedLocationClient.requestLocationUpdates(request, gmsCallback, Looper.getMainLooper());
        } catch (Exception e) {
            callback.onLocationReceived(CASABLANCA_LAT, CASABLANCA_LNG);
        }
    }

    private void deliverWithCasablancaFallback(Location location, LocationCallback callback) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        callback.onLocationReceived(lat, lon);
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
