package com.example.vibemeet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.vibemeet.services.FavoritesService;
import com.example.vibemeet.services.LocationService;
import com.example.vibemeet.services.PlacesService;
import com.example.vibemeet.services.VisitHistoryService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

public class VenueDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TextView venueName, venueType, venueAddress, venueDescription, venueRatingText,
            venueDistrict, venuePrice, venueHours, openStatus, topReviewText, topReviewAuthor;
    private RatingBar ratingBar;
    private ImageView heroImage;
    private LinearLayout reviewSection, googleReviewsSection, reviewsList;
    private Button btnGetDirections, btnChat, btnCheckIn;
    private ImageButton btnFavorite, btnBack;
    private MapView mapView;
    private GoogleMap googleMap;
    private Polyline currentRoute;

    private double venueLat, venueLon;
    private String venueAddr, venueNameStr, venueId, venueTypeStr;
    private boolean isFavorite;

    private FavoritesService favoritesService;
    private VisitHistoryService visitHistoryService;
    private LocationService locationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_venue_detail);

        favoritesService = new FavoritesService(this);
        visitHistoryService = new VisitHistoryService(this);
        locationService = new LocationService(this);

        // Wire up views
        heroImage = findViewById(R.id.heroImage);
        venueName = findViewById(R.id.venueName);
        venueType = findViewById(R.id.venueType);
        venueAddress = findViewById(R.id.venueAddress);
        venueDescription = findViewById(R.id.venueDescription);
        venueRatingText = findViewById(R.id.venueRatingText);
        venueDistrict = findViewById(R.id.venueDistrict);
        venuePrice = findViewById(R.id.venuePrice);
        venueHours = findViewById(R.id.venueHours);
        openStatus = findViewById(R.id.openStatus);
        topReviewText = findViewById(R.id.topReviewText);
        topReviewAuthor = findViewById(R.id.topReviewAuthor);
        reviewSection = findViewById(R.id.reviewSection);
        googleReviewsSection = findViewById(R.id.googleReviewsSection);
        reviewsList = findViewById(R.id.reviewsList);
        ratingBar = findViewById(R.id.venueRating);
        btnGetDirections = findViewById(R.id.btnGetDirections);
        btnChat = findViewById(R.id.btnChat);
        btnCheckIn = findViewById(R.id.btnCheckIn);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnBack = findViewById(R.id.btnBack);
        mapView = findViewById(R.id.mapFragment);

        btnBack.setOnClickListener(v -> finish());

        // Read intent
        Intent intent = getIntent();
        venueId = intent.getStringExtra("venue_id");
        venueNameStr = intent.getStringExtra("venue_name");
        venueTypeStr = intent.getStringExtra("venue_type");
        String address = intent.getStringExtra("venue_address");
        String description = intent.getStringExtra("venue_description");
        String district = intent.getStringExtra("venue_district");
        String imageUrl = intent.getStringExtra("venue_image_url");
        String topReview = intent.getStringExtra("venue_top_review");
        String topReviewAuth = intent.getStringExtra("venue_top_review_author");
        float rating = intent.getFloatExtra("venue_rating", 0);
        int reviews = intent.getIntExtra("venue_reviews", 0);
        int price = intent.getIntExtra("venue_price", 0);
        int openHour = intent.getIntExtra("venue_open", 0);
        int closeHour = intent.getIntExtra("venue_close", 0);
        venueLat = intent.getDoubleExtra("venue_lat", 0);
        venueLon = intent.getDoubleExtra("venue_lon", 0);
        venueAddr = address;

        if (venueId == null && venueNameStr != null) {
            venueId = venueNameStr.toLowerCase().replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "");
        }

        // Hero image
        if (!TextUtils.isEmpty(imageUrl)) {
            Glide.with(this).load(imageUrl).centerCrop().into(heroImage);
        } else {
            heroImage.setImageResource(R.color.surface_variant);
        }

        venueName.setText(venueNameStr);
        venueType.setText(venueTypeStr != null ? venueTypeStr.toUpperCase() : "");
        venueAddress.setText(address);
        venueDescription.setText(description);
        venueDistrict.setText(district != null ? district : "Casablanca");
        venuePrice.setText(getPriceText(price));
        venueHours.setText(String.format("%02d:00 - %02d:00", openHour, closeHour));
        boolean isOpen = checkIsOpen(openHour, closeHour);
        openStatus.setText(isOpen ? "Open now" : "Closed");
        if (isOpen) {
            openStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
            openStatus.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            openStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
            openStatus.setTextColor(Color.parseColor("#C62828"));
        }
        ratingBar.setRating(rating);
        venueRatingText.setText(String.format("%.1f (%d)", rating, reviews));

        // Top review
        if (!TextUtils.isEmpty(topReview)) {
            topReviewText.setText("\"" + topReview + "\"");
            topReviewAuthor.setText("— " + (topReviewAuth != null ? topReviewAuth : "Anonymous"));
            reviewSection.setVisibility(View.VISIBLE);
        }

        updateFavoriteIcon();

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        fetchGooglePlacesReviews();

        btnGetDirections.setOnClickListener(v -> {
            String uri = "geo:" + venueLat + "," + venueLon + "?q=" +
                    Uri.encode(venueNameStr + ", " + venueAddr);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(mapIntent);
        });

        btnChat.setOnClickListener(v -> {
            Intent chatIntent = new Intent(this, ChatActivity.class);
            chatIntent.putExtra("preset_query", "Tell me more about " + venueNameStr + " in Casablanca");
            startActivity(chatIntent);
        });

        btnCheckIn.setOnClickListener(v -> {
            visitHistoryService.checkIn(venueId, venueNameStr, venueTypeStr);
            Toast.makeText(this, "Checked in at " + venueNameStr, Toast.LENGTH_SHORT).show();
            btnCheckIn.setText("Checked in");
            btnCheckIn.setEnabled(false);
        });

        btnFavorite.setOnClickListener(v -> {
            favoritesService.toggleFavorite(venueId);
            updateFavoriteIcon();
            Toast.makeText(this,
                    favoritesService.isFavorite(venueId) ? "Saved" : "Removed",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void updateFavoriteIcon() {
        isFavorite = favoritesService.isFavorite(venueId);
        btnFavorite.setImageResource(isFavorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline_dark);
    }

    private boolean checkIsOpen(int openH, int closeH) {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (openH <= closeH) return hour >= openH && hour < closeH;
        return hour >= openH || hour < closeH;
    }

    private String getPriceText(int price) {
        switch (price) {
            case 1: return "$";
            case 2: return "$$";
            case 3: return "$$$";
            default: return "";
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        LatLng venueLocation = new LatLng(venueLat, venueLon);
        googleMap.addMarker(new MarkerOptions()
                .position(venueLocation)
                .title(venueNameStr)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(venueLocation, 15));
        googleMap.getUiSettings().setZoomControlsEnabled(false);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationService.getCurrentLocation(new LocationService.LocationCallback() {
                @Override
                public void onLocationReceived(double latitude, double longitude) {
                    tryDrawRouteIfNearby(latitude, longitude);
                }

                @Override public void onError(String error) {}
            });
        }
    }

    /**
     * Only draws a route from the user → venue when the user is actually in Casablanca area.
     * If the device is far away (e.g., an emulator stuck in California), skips the route and
     * just keeps the map focused on the venue.
     */
    private void tryDrawRouteIfNearby(double startLat, double startLon) {
        // Casablanca bounding box (roughly 50km radius around city center)
        boolean userIsInCasablanca =
                startLat >= 33.40 && startLat <= 33.75 &&
                startLon >= -7.80 && startLon <= -7.40;

        if (!userIsInCasablanca) {
            // User is too far away — don't draw a transatlantic line.
            // Just keep the camera on the venue.
            return;
        }

        LatLng start = new LatLng(startLat, startLon);
        LatLng end = new LatLng(venueLat, venueLon);

        googleMap.addMarker(new MarkerOptions()
                .position(start)
                .title("You are here")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        if (currentRoute != null) currentRoute.remove();

        currentRoute = googleMap.addPolyline(new PolylineOptions()
                .add(start)
                .add(end)
                .width(8f)
                .color(Color.parseColor("#C62828"))
                .geodesic(true));

        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(start)
                .include(end)
                .build();
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
    }

    private void fetchGooglePlacesReviews() {
        PlacesService.getInstance(this).fetchPlaceDetails(venueNameStr, venueLat, venueLon,
                new PlacesService.PlaceDetailsCallback() {
                    @Override
                    public void onSuccess(String reviews, android.graphics.Bitmap photo, float rating, int reviewCount,
                                          List<PlacesService.GoogleReview> googleReviews) {
                        if (photo != null) {
                            heroImage.setImageBitmap(photo);
                        }
                        displayGoogleReviews(reviews, rating, reviewCount, googleReviews);
                    }

                    @Override
                    public void onError(String errorMessage) {
                    }
                });
    }

    private void displayGoogleReviews(String reviewText, float rating, int reviewCount,
                                      List<PlacesService.GoogleReview> googleReviews) {
        if (rating <= 0 && reviewCount <= 0 && (googleReviews == null || googleReviews.isEmpty())) {
            googleReviewsSection.setVisibility(View.GONE);
            return;
        }

        reviewsList.removeAllViews();

        LinearLayout summaryCard = new LinearLayout(this);
        summaryCard.setOrientation(LinearLayout.VERTICAL);
        summaryCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        summaryCard.setBackground(ContextCompat.getDrawable(this, R.drawable.info_strip_bg));
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        summaryParams.setMargins(0, 0, 0, dp(10));
        summaryCard.setLayoutParams(summaryParams);

        TextView ratingText = new TextView(this);
        String ratingLabel = rating > 0
                ? "Google Maps · " + String.format("%.1f", rating) + " stars from " + reviewCount + " reviews"
                : "Google Maps reviews";
        ratingText.setText(ratingLabel);
        ratingText.setTextSize(14);
        ratingText.setTextColor(getColor(R.color.text_primary));
        ratingText.setTypeface(null, android.graphics.Typeface.BOLD);
        ratingText.setLineSpacing(2, 1.2f);
        summaryCard.addView(ratingText);
        reviewsList.addView(summaryCard);

        if (googleReviews != null && !googleReviews.isEmpty()) {
            int count = 0;
            for (PlacesService.GoogleReview review : googleReviews) {
                if (count >= 5) break;
                reviewsList.addView(buildGoogleReviewCard(review));
                count++;
            }
        } else if (!TextUtils.isEmpty(reviewText) && !reviewText.equals("Find more reviews on Google Maps")) {
            TextView reviewsText = new TextView(this);
            reviewsText.setText(reviewText);
            reviewsText.setTextSize(13);
            reviewsText.setTextColor(getColor(R.color.text_secondary));
            reviewsText.setLineSpacing(3, 1.3f);
            reviewsList.addView(reviewsText);
        } else {
            TextView hint = new TextView(this);
            hint.setText("Tap directions to open Google Maps for the latest detailed reviews.");
            hint.setTextSize(13);
            hint.setTextColor(getColor(R.color.text_secondary));
            hint.setLineSpacing(3, 1.3f);
            reviewsList.addView(hint);
        }

        googleReviewsSection.setVisibility(View.VISIBLE);
    }

    private View buildGoogleReviewCard(PlacesService.GoogleReview review) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(ContextCompat.getDrawable(this, R.drawable.google_review_card_bg));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(params);

        TextView meta = new TextView(this);
        String stars = review.rating > 0 ? String.format("%.1f stars", review.rating) : "Google review";
        meta.setText(stars + " · " + review.authorName + " · " + review.relativeTime);
        meta.setTextSize(12);
        meta.setTextColor(getColor(R.color.brand_accent));
        meta.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(meta);

        TextView body = new TextView(this);
        body.setText("\"" + trimReview(review.text) + "\"");
        body.setTextSize(14);
        body.setTextColor(getColor(R.color.text_primary));
        body.setLineSpacing(4, 1.15f);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        bodyParams.topMargin = dp(8);
        body.setLayoutParams(bodyParams);
        card.addView(body);

        return card;
    }

    private String trimReview(String review) {
        if (review == null) return "";
        String clean = review.trim().replaceAll("\\s+", " ");
        return clean.length() > 240 ? clean.substring(0, 237) + "..." : clean;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); if (mapView != null) mapView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); if (mapView != null) mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }
}
