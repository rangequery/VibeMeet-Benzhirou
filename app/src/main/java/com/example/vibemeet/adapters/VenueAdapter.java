package com.example.vibemeet.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.vibemeet.R;
import com.example.vibemeet.VenueDetailActivity;
import com.example.vibemeet.models.Venue;
import com.example.vibemeet.services.FavoritesService;

import java.util.List;

public class VenueAdapter extends BaseAdapter {
    private final Context context;
    private final List<Venue> venues;
    private final FavoritesService favoritesService;

    public VenueAdapter(Context context, List<Venue> venues) {
        this.context = context;
        this.venues = venues;
        this.favoritesService = new FavoritesService(context);
    }

    @Override public int getCount() { return venues.size(); }
    @Override public Object getItem(int position) { return venues.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_venue, parent, false);
        }

        Venue venue = venues.get(position);

        ImageView photoView = convertView.findViewById(R.id.venuePhoto);
        TextView venueName = convertView.findViewById(R.id.venueName);
        TextView venuePrice = convertView.findViewById(R.id.venuePrice);
        TextView venueRating = convertView.findViewById(R.id.venueRatingText);
        TextView venueDistrict = convertView.findViewById(R.id.venueDistrict);
        TextView venueTypeLabel = convertView.findViewById(R.id.venueTypeLabel);
        TextView walkTime = convertView.findViewById(R.id.venueWalkTime);
        LinearLayout openBadge = convertView.findViewById(R.id.openBadgeContainer);
        ImageButton favoriteBtn = convertView.findViewById(R.id.favoriteBtn);
        Button directionsBtn = convertView.findViewById(R.id.directionsBtn);

        // Load photo with Glide
        String imageUrl = venue.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.color.surface_variant)
                    .into(photoView);
        } else {
            photoView.setImageResource(R.color.surface_variant);
        }

        venueName.setText(venue.getName());
        venuePrice.setText(venue.getPriceLevelText());
        venueRating.setText(String.format("%.1f (%d)", venue.getRating(), venue.getReviewCount()));
        venueDistrict.setText(venue.getDistrict() != null ? venue.getDistrict() : "Casablanca");
        venueTypeLabel.setText(venue.getType().toUpperCase());
        openBadge.setVisibility(venue.isOpenNow() ? View.VISIBLE : View.GONE);

        // Distance + walking/driving time (only meaningful if user is in Casablanca)
        double distanceKm = venue.getDistanceKm();
        if (distanceKm < 0.05) {
            // Very close
            walkTime.setText("Less than 1 min away");
        } else if (distanceKm <= 2.5) {
            // Walking distance
            int walkMin = venue.getWalkingMinutes();
            walkTime.setText(String.format("%d min walk · %.1f km", walkMin, distanceKm));
        } else if (distanceKm <= 50) {
            // Drive within metro area
            int driveMin = venue.getDrivingMinutes();
            walkTime.setText(String.format("%d min drive · %.1f km", driveMin, distanceKm));
        } else {
            // Too far to be meaningful — just show district info
            walkTime.setText(venue.getDistrict() != null ? venue.getDistrict() : "In Casablanca");
        }

        // Favorite
        updateFavoriteIcon(favoriteBtn, venue.getId());
        favoriteBtn.setOnClickListener(v -> {
            favoritesService.toggleFavorite(venue.getId());
            updateFavoriteIcon(favoriteBtn, venue.getId());
        });

        // Directions
        directionsBtn.setOnClickListener(v -> {
            String uri = "geo:" + venue.getLatitude() + "," + venue.getLongitude()
                    + "?q=" + Uri.encode(venue.getName() + ", " + (venue.getAddress() != null ? venue.getAddress() : "Casablanca"));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            context.startActivity(mapIntent);
        });

        // Card click → details
        convertView.setOnClickListener(v -> openDetail(venue));

        return convertView;
    }

    private void openDetail(Venue venue) {
        Intent intent = new Intent(context, VenueDetailActivity.class);
        intent.putExtra("venue_id", venue.getId());
        intent.putExtra("venue_name", venue.getName());
        intent.putExtra("venue_description", venue.getDescription());
        intent.putExtra("venue_type", venue.getType());
        intent.putExtra("venue_lat", venue.getLatitude());
        intent.putExtra("venue_lon", venue.getLongitude());
        intent.putExtra("venue_address", venue.getAddress());
        intent.putExtra("venue_rating", venue.getRating());
        intent.putExtra("venue_reviews", venue.getReviewCount());
        intent.putExtra("venue_district", venue.getDistrict());
        intent.putExtra("venue_price", venue.getPriceLevel());
        intent.putExtra("venue_open", venue.getOpenHour());
        intent.putExtra("venue_close", venue.getCloseHour());
        intent.putExtra("venue_image_url", venue.getImageUrl());
        intent.putExtra("venue_top_review", venue.getTopReview());
        intent.putExtra("venue_top_review_author", venue.getTopReviewAuthor());
        context.startActivity(intent);
    }

    private void updateFavoriteIcon(ImageButton btn, String venueId) {
        boolean isFav = favoritesService.isFavorite(venueId);
        btn.setImageResource(isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline_dark);
    }
}
