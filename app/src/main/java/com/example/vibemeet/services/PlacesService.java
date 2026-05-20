package com.example.vibemeet.services;

import android.content.Context;
import android.graphics.Bitmap;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.Review;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlacesService {
    private static PlacesService instance;
    private final PlacesClient placesClient;
    private final String apiKey;

    private static final String MAPS_API_KEY = "AIzaSyAEeVbU20hYZHDQIL8ZMepXD-ZN4OEmeiQ";

    public static class GoogleReview {
        public final String authorName;
        public final String text;
        public final float rating;
        public final String relativeTime;

        public GoogleReview(String authorName, String text, float rating, String relativeTime) {
            this.authorName = authorName;
            this.text = text;
            this.rating = rating;
            this.relativeTime = relativeTime;
        }
    }

    public interface PlaceDetailsCallback {
        void onSuccess(String reviews, Bitmap photo, float rating, int reviewCount, List<GoogleReview> googleReviews);
        void onError(String errorMessage);
    }

    private PlacesService(Context context) {
        this.apiKey = MAPS_API_KEY;
        try {
            if (!Places.isInitialized()) {
                Places.initialize(context.getApplicationContext(), apiKey);
            }
            this.placesClient = Places.createClient(context.getApplicationContext());
        } catch (Exception e) {
            throw new RuntimeException("Places API initialization failed: " + e.getMessage());
        }
    }

    public static synchronized PlacesService getInstance(Context context) {
        if (instance == null) {
            instance = new PlacesService(context);
        }
        return instance;
    }

    public void fetchPlaceDetails(String venueName, double latitude, double longitude, PlaceDetailsCallback callback) {
        String searchQuery = venueName + " Casablanca Morocco";

        FindAutocompletePredictionsRequest autoRequest = FindAutocompletePredictionsRequest.builder()
                .setQuery(searchQuery)
                .build();

        placesClient.findAutocompletePredictions(autoRequest).addOnSuccessListener(response -> {
            if (response.getAutocompletePredictions() != null && !response.getAutocompletePredictions().isEmpty()) {
                String placeId = response.getAutocompletePredictions().get(0).getPlaceId();
                fetchPlaceDetailsById(placeId, callback);
            } else {
                callback.onError("Place not found on Google Maps");
            }
        }).addOnFailureListener(e -> {
            callback.onError("Places API error: " + e.getMessage());
        });
    }

    private void fetchPlaceDetailsById(String placeId, PlaceDetailsCallback callback) {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID,
                Place.Field.PHOTO_METADATAS,
                Place.Field.RATING,
                Place.Field.USER_RATINGS_TOTAL,
                Place.Field.ADDRESS,
                Place.Field.REVIEWS
        );

        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, fields);

        placesClient.fetchPlace(request).addOnSuccessListener(response -> {
            Place place = response.getPlace();

            String reviewText = "Find more reviews on Google Maps";
            List<GoogleReview> googleReviews = extractReviews(place.getReviews());
            float rating = place.getRating() != null ? place.getRating().floatValue() : 0;
            int reviewCount = place.getUserRatingsTotal() != null ? place.getUserRatingsTotal() : 0;

            if (place.getPhotoMetadatas() != null && !place.getPhotoMetadatas().isEmpty()) {
                fetchFirstPhoto(place.getPhotoMetadatas().get(0), reviewText, rating, reviewCount, googleReviews, callback);
                return;
            }

            callback.onSuccess(reviewText, null, rating, reviewCount, googleReviews);

        }).addOnFailureListener(e -> {
            callback.onError("Failed to fetch place details: " + e.getMessage());
        });
    }

    private List<GoogleReview> extractReviews(List<Review> reviews) {
        List<GoogleReview> result = new ArrayList<>();
        if (reviews == null) return result;

        for (Review review : reviews) {
            if (result.size() >= 5) break;
            String text = review.getText();
            if (text == null || text.trim().isEmpty()) text = review.getOriginalText();
            if (text == null || text.trim().isEmpty()) continue;

            String author = "Google user";
            if (review.getAuthorAttribution() != null && review.getAuthorAttribution().getName() != null) {
                author = review.getAuthorAttribution().getName();
            }
            float reviewRating = review.getRating() != null ? review.getRating().floatValue() : 0f;
            String relativeTime = review.getRelativePublishTimeDescription() != null
                    ? review.getRelativePublishTimeDescription()
                    : "Recent review";
            result.add(new GoogleReview(author, text.trim(), reviewRating, relativeTime));
        }
        return result;
    }

    private void fetchFirstPhoto(Object photoMetadata, String reviews, float rating, int reviewCount,
                                 List<GoogleReview> googleReviews, PlaceDetailsCallback callback) {
        try {
            FetchPhotoRequest photoRequest = FetchPhotoRequest.builder((com.google.android.libraries.places.api.model.PhotoMetadata) photoMetadata)
                    .setMaxWidth(1200)
                    .setMaxHeight(800)
                    .build();

            placesClient.fetchPhoto(photoRequest).addOnSuccessListener(photoResponse -> {
                Bitmap bitmap = photoResponse.getBitmap();
                callback.onSuccess(reviews, bitmap, rating, reviewCount, googleReviews);
            }).addOnFailureListener(e -> {
                callback.onSuccess(reviews, null, rating, reviewCount, googleReviews);
            });
        } catch (Exception e) {
            callback.onSuccess(reviews, null, rating, reviewCount, googleReviews);
        }
    }
}
