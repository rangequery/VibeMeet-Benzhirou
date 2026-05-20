package com.example.vibemeet.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vibemeet.R;
import com.example.vibemeet.adapters.VenueAdapter;
import com.example.vibemeet.models.Venue;
import com.example.vibemeet.services.FavoritesService;
import com.example.vibemeet.services.VenueService;
import com.example.vibemeet.services.VisitHistoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SavedFragment extends Fragment {

    private ListView favoritesList;
    private TextView emptyText, savedCount, visitedCount;
    private FavoritesService favoritesService;
    private VenueService venueService;
    private VisitHistoryService visitHistoryService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        favoritesList = view.findViewById(R.id.favoritesList);
        emptyText = view.findViewById(R.id.emptyText);
        savedCount = view.findViewById(R.id.savedCount);
        visitedCount = view.findViewById(R.id.visitedCount);

        favoritesService = new FavoritesService(requireContext());
        venueService = new VenueService();
        visitHistoryService = new VisitHistoryService(requireContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void loadFavorites() {
        Set<String> favIds = favoritesService.getFavoriteIds();
        List<Venue> favorites = new ArrayList<>();

        for (String id : favIds) {
            Venue v = venueService.getVenueById(id);
            if (v != null) favorites.add(v);
        }

        savedCount.setText(favorites.size() + " Saved");
        visitedCount.setText(visitHistoryService.getUniqueVenueCount() + " Visited");

        if (favorites.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            favoritesList.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            favoritesList.setVisibility(View.VISIBLE);
            VenueAdapter adapter = new VenueAdapter(requireContext(), favorites);
            favoritesList.setAdapter(adapter);
        }
    }
}
