package com.example.vibemeet.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.content.Intent;
import android.net.Uri;

import com.bumptech.glide.Glide;
import com.example.vibemeet.R;
import com.example.vibemeet.VenueDetailActivity;
import com.example.vibemeet.models.Venue;
import com.example.vibemeet.services.LocationService;
import com.example.vibemeet.services.VenueService;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationService locationService;
    private VenueService venueService;
    private Spinner categoryFilter;
    private TextView venueCount;
    private Button btnRecenter, btnStyleToggle;
    private double currentLat = VenueService.CASABLANCA_LAT;
    private double currentLon = VenueService.CASABLANCA_LNG;
    private String selectedCategory = "All";
    private boolean nightMode = false;
    private final Map<Marker, Venue> markerVenueMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        locationService = new LocationService(requireContext());
        venueService = new VenueService();

        categoryFilter = view.findViewById(R.id.mapCategoryFilter);
        venueCount = view.findViewById(R.id.venueCount);
        btnRecenter = view.findViewById(R.id.btnRecenter);
        btnStyleToggle = view.findViewById(R.id.btnStyleToggle);
        FloatingActionButton btnMyLocation = view.findViewById(R.id.btnMyLocation);

        setupCategoryFilter();

        btnMyLocation.setOnClickListener(v -> goToUserLocation());
        btnRecenter.setOnClickListener(v -> recenterMap());
        btnStyleToggle.setOnClickListener(v -> toggleMapStyle());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupCategoryFilter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, VenueService.getCategories());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilter.setAdapter(adapter);

        categoryFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = VenueService.getCategories()[position];
                if (mMap != null) refreshMapMarkers();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        LatLng casablanca = new LatLng(VenueService.CASABLANCA_LAT, VenueService.CASABLANCA_LNG);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(casablanca, 12f));

        LatLngBounds casablancaBounds = new LatLngBounds(
                new LatLng(33.4900, -7.7500),
                new LatLng(33.6500, -7.5000));
        mMap.setLatLngBoundsForCameraTarget(casablancaBounds);
        mMap.setMinZoomPreference(10f);

        mMap.setOnMarkerClickListener(marker -> {
            Venue venue = markerVenueMap.get(marker);
            if (venue != null) {
                showVenueBottomSheet(venue);
            }
            return true;
        });

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            loadMapWithUserLocation();
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 20);
            refreshMapMarkers();
        }
    }

    private void loadMapWithUserLocation() {
        locationService.getCurrentLocation(new LocationService.LocationCallback() {
            @Override
            public void onLocationReceived(double latitude, double longitude) {
                currentLat = latitude;
                currentLon = longitude;
                LatLng userLocation = new LatLng(latitude, longitude);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14f));
                refreshMapMarkers();
            }

            @Override
            public void onError(String error) {
                refreshMapMarkers();
            }
        });
    }

    private void refreshMapMarkers() {
        mMap.clear();
        markerVenueMap.clear();

        List<Venue> venues = venueService.getVenuesByType(selectedCategory, currentLat, currentLon);

        for (Venue venue : venues) {
            LatLng venuePos = new LatLng(venue.getLatitude(), venue.getLongitude());
            float markerColor = getMarkerColorForType(venue.getType());

            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(venuePos)
                    .title(venue.getName())
                    .snippet(venue.getType() + " · ★" + venue.getRating() + " · " +
                            String.format("%.1f km · tap for details", venue.getDistanceKm()))
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

            if (marker != null) {
                markerVenueMap.put(marker, venue);
            }
        }

        venueCount.setText(venues.size() + "\nvenues");
    }

    private void goToUserLocation() {
        locationService.getCurrentLocation(new LocationService.LocationCallback() {
            @Override
            public void onLocationReceived(double latitude, double longitude) {
                LatLng userLoc = new LatLng(latitude, longitude);
                if (mMap != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLoc, 15f));
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), "Could not get location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void recenterMap() {
        LatLng casablanca = new LatLng(VenueService.CASABLANCA_LAT, VenueService.CASABLANCA_LNG);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(casablanca, 12f));
    }

    private void toggleMapStyle() {
        nightMode = !nightMode;
        try {
            if (nightMode) {
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_night));
                btnStyleToggle.setText("Day");
            } else {
                mMap.setMapStyle(null);
                btnStyleToggle.setText("Night");
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Style error", Toast.LENGTH_SHORT).show();
        }
    }

    private float getMarkerColorForType(String type) {
        if (type == null) return BitmapDescriptorFactory.HUE_RED;
        switch (type.toLowerCase()) {
            case "cafe": return BitmapDescriptorFactory.HUE_YELLOW;
            case "restaurant": return BitmapDescriptorFactory.HUE_ORANGE;
            case "activity": return BitmapDescriptorFactory.HUE_GREEN;
            case "bar": return BitmapDescriptorFactory.HUE_VIOLET;
            case "park": return BitmapDescriptorFactory.HUE_CYAN;
            case "shopping": return BitmapDescriptorFactory.HUE_MAGENTA;
            default: return BitmapDescriptorFactory.HUE_RED;
        }
    }

    private void showVenueBottomSheet(Venue venue) {
        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(requireContext());
        android.view.View sheetView = inflater.inflate(R.layout.bottom_sheet_venue, null);

        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        dialog.setContentView(sheetView);

        TextView venueName = sheetView.findViewById(R.id.sheetVenueName);
        TextView venueType = sheetView.findViewById(R.id.sheetVenueType);
        TextView venueDistance = sheetView.findViewById(R.id.sheetVenueDistance);
        android.widget.ImageView venuePhoto = sheetView.findViewById(R.id.sheetVenuePhoto);
        Button viewDetailsBtn = sheetView.findViewById(R.id.sheetViewDetails);
        Button navigateBtn = sheetView.findViewById(R.id.sheetNavigate);

        venueName.setText(venue.getName());
        venueType.setText(venue.getType() + " · " + String.format("%.1f", venue.getRating()) + " rating");
        venueDistance.setText(String.format("%.1f km away", venue.getDistanceKm()));

        if (venue.getImageUrl() != null) {
            com.bumptech.glide.Glide.with(this).load(venue.getImageUrl())
                    .centerCrop()
                    .into(venuePhoto);
        }

        viewDetailsBtn.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(requireContext(), VenueDetailActivity.class);
            intent.putExtra("venue_name", venue.getName());
            intent.putExtra("venue_description", venue.getDescription());
            intent.putExtra("venue_type", venue.getType());
            intent.putExtra("venue_lat", venue.getLatitude());
            intent.putExtra("venue_lon", venue.getLongitude());
            intent.putExtra("venue_address", venue.getAddress());
            intent.putExtra("venue_rating", venue.getRating());
            intent.putExtra("venue_reviews", venue.getReviewCount());
            intent.putExtra("venue_id", venue.getId());
            intent.putExtra("venue_district", venue.getDistrict());
            intent.putExtra("venue_price", venue.getPriceLevel());
            intent.putExtra("venue_open", venue.getOpenHour());
            intent.putExtra("venue_close", venue.getCloseHour());
            intent.putExtra("venue_image_url", venue.getImageUrl());
            intent.putExtra("venue_top_review", venue.getTopReview());
            intent.putExtra("venue_top_review_author", venue.getTopReviewAuthor());
            startActivity(intent);
            dialog.dismiss();
        });

        navigateBtn.setOnClickListener(v -> {
            String uri = "google.navigation:q=" + venue.getLatitude() + "," + venue.getLongitude() +
                    "&mode=d";
            android.content.Intent navigationIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri));
            navigationIntent.setPackage("com.google.android.apps.maps");
            if (navigationIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(navigationIntent);
            } else {
                String geoUri = "geo:" + venue.getLatitude() + "," + venue.getLongitude() +
                        "?q=" + android.net.Uri.encode(venue.getName());
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(geoUri));
                startActivity(mapIntent);
            }
            dialog.dismiss();
        });

        dialog.show();
    }
}
