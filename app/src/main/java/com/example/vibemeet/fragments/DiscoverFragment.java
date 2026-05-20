package com.example.vibemeet.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.vibemeet.ItineraryActivity;
import com.example.vibemeet.MainActivity;
import com.example.vibemeet.R;
import com.example.vibemeet.EssentialsActivity;
import com.example.vibemeet.adapters.VenueAdapter;
import com.example.vibemeet.models.Venue;
import com.example.vibemeet.services.LocationService;
import com.example.vibemeet.services.UserPreferencesService;
import com.example.vibemeet.services.VenueService;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscoverFragment extends Fragment {

    private LocationService locationService;
    private VenueService venueService;
    private UserPreferencesService prefs;
    private ListView venueListView;
    private ProgressBar loadingSpinner;
    private TextView greetingTextView, subGreetingTextView, resultCountText;
    private EditText searchInput;
    private LinearLayout categoryChipsRow;
    private Button btnOpenNow, btnTopRated;
    private SwipeRefreshLayout swipeRefreshLayout;

    private double currentLat = VenueService.CASABLANCA_LAT;
    private double currentLon = VenueService.CASABLANCA_LNG;
    private String currentCategory = "All";
    private boolean filterOpenNow = false;
    private boolean filterTopRated = false;

    private final Map<String, Button> categoryChipMap = new HashMap<>();

    private static final String[][] CATEGORIES = {
            {"All", "All"},
            {"Restaurant", "Eats"},
            {"Cafe", "Cafes"},
            {"Bar", "Bars"},
            {"Activity", "To Do"},
            {"Park", "Parks"},
            {"Shopping", "Shop"}
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discover, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        greetingTextView = view.findViewById(R.id.greetingTextView);
        subGreetingTextView = view.findViewById(R.id.subGreetingTextView);
        venueListView = view.findViewById(R.id.venueListView);
        loadingSpinner = view.findViewById(R.id.loadingSpinner);
        searchInput = view.findViewById(R.id.searchInput);
        categoryChipsRow = view.findViewById(R.id.categoryChipsRow);
        btnOpenNow = view.findViewById(R.id.btnOpenNow);
        btnTopRated = view.findViewById(R.id.btnTopRated);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        resultCountText = view.findViewById(R.id.resultCountText);

        locationService = new LocationService(requireContext());
        venueService = new VenueService();
        prefs = new UserPreferencesService(requireContext());

        String userName = prefs.getUserName();
        greetingTextView.setText(getTimeBasedGreeting() + ", " + userName);
        subGreetingTextView.setText("CASABLANCA");

        buildCategoryChips();
        setupSearchListener();
        setupQuickFilters();

        androidx.cardview.widget.CardView essentialsCard = view.findViewById(R.id.essentialsCard);
        essentialsCard.setOnClickListener(v -> {
            startActivity(new android.content.Intent(requireContext(), EssentialsActivity.class));
        });

        TextView btnChatGuide = view.findViewById(R.id.btnChatGuide);
        TextView btnPlanDay = view.findViewById(R.id.btnPlanDay);
        btnChatGuide.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openChatTab(
                        "I want to discover Casablanca today. Suggest places based on food, culture, and nightlife.");
            }
        });
        btnPlanDay.setOnClickListener(v -> startActivity(new Intent(requireContext(), ItineraryActivity.class)));

        swipeRefreshLayout.setColorSchemeColors(0xFF243B6B, 0xFF0F9F9A);
        swipeRefreshLayout.setOnRefreshListener(this::loadNearbyVenues);

        checkLocationPermissionAndLoadVenues();
    }

    private String getTimeBasedGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good morning";
        else if (hour < 18) return "Good afternoon";
        else return "Good evening";
    }

    private void buildCategoryChips() {
        categoryChipsRow.removeAllViews();
        categoryChipMap.clear();

        for (String[] cat : CATEGORIES) {
            String value = cat[0];
            String label = cat[1];

            Button chip = new Button(requireContext());
            chip.setText(label);
            chip.setTextSize(13);
            chip.setAllCaps(false);
            chip.setMinWidth(0);
            chip.setMinHeight(0);
            chip.setPadding(dp(18), dp(8), dp(18), dp(8));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dp(42));
            params.setMargins(dp(4), 0, dp(4), 0);
            chip.setLayoutParams(params);

            boolean selected = value.equals(currentCategory);
            chip.setBackgroundResource(selected ? R.drawable.filter_chip_on : R.drawable.filter_chip_off);
            chip.setTextColor(selected ? 0xFFFFFFFF : 0xFF64748B);

            chip.setOnClickListener(v -> {
                currentCategory = value;
                refreshChipStyles();
                filterVenues();
            });

            categoryChipsRow.addView(chip);
            categoryChipMap.put(value, chip);
        }
    }

    private void refreshChipStyles() {
        for (Map.Entry<String, Button> entry : categoryChipMap.entrySet()) {
            boolean selected = entry.getKey().equals(currentCategory);
            entry.getValue().setBackgroundResource(
                    selected ? R.drawable.filter_chip_on : R.drawable.filter_chip_off);
            entry.getValue().setTextColor(selected ? 0xFFFFFFFF : 0xFF666666);
        }
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void setupSearchListener() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterVenues(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupQuickFilters() {
        btnOpenNow.setOnClickListener(v -> {
            filterOpenNow = !filterOpenNow;
            updateQuickFilterUI();
            filterVenues();
        });
        btnTopRated.setOnClickListener(v -> {
            filterTopRated = !filterTopRated;
            updateQuickFilterUI();
            filterVenues();
        });
    }

    private void updateQuickFilterUI() {
        btnOpenNow.setBackgroundResource(filterOpenNow ? R.drawable.filter_chip_on : R.drawable.filter_chip_off);
        btnOpenNow.setTextColor(filterOpenNow ? 0xFFFFFFFF : 0xFF666666);
        btnTopRated.setBackgroundResource(filterTopRated ? R.drawable.filter_chip_on : R.drawable.filter_chip_off);
        btnTopRated.setTextColor(filterTopRated ? 0xFFFFFFFF : 0xFF666666);
    }

    private void filterVenues() {
        String query = searchInput.getText().toString().trim();
        List<Venue> filtered;

        if (!query.isEmpty()) {
            filtered = venueService.searchVenues(query, currentLat, currentLon);
        } else {
            filtered = venueService.getAllVenuesSortedByDistance(currentLat, currentLon);
        }

        if (!currentCategory.equals("All")) {
            filtered.removeIf(v -> !v.getType().equalsIgnoreCase(currentCategory));
        }
        if (filterOpenNow) {
            filtered.removeIf(v -> !v.isOpenNow());
        }
        if (filterTopRated) {
            filtered.removeIf(v -> v.getRating() < 4.5);
        }

        VenueAdapter adapter = new VenueAdapter(requireContext(), filtered);
        venueListView.setAdapter(adapter);
        resultCountText.setText(filtered.size() + " places near you");
    }

    private void checkLocationPermissionAndLoadVenues() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            loadNearbyVenues();
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            loadDefaultVenues();
        }
    }

    private void loadNearbyVenues() {
        loadingSpinner.setVisibility(View.VISIBLE);
        locationService.getCurrentLocation(new LocationService.LocationCallback() {
            @Override
            public void onLocationReceived(double latitude, double longitude) {
                currentLat = latitude;
                currentLon = longitude;
                filterVenues();
                loadingSpinner.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onError(String error) {
                loadDefaultVenues();
                loadingSpinner.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void loadDefaultVenues() {
        currentLat = VenueService.CASABLANCA_LAT;
        currentLon = VenueService.CASABLANCA_LNG;
        filterVenues();
    }
}
