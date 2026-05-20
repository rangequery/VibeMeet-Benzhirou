package com.example.vibemeet.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vibemeet.Avatar3DCreatorActivity;
import com.example.vibemeet.AvatarSetupActivity;
import com.example.vibemeet.DigitalTwinActivity;
import com.example.vibemeet.LoginActivity;
import com.example.vibemeet.OnboardingActivity;
import com.example.vibemeet.R;
import com.example.vibemeet.services.AvatarService;
import com.example.vibemeet.services.FavoritesService;
import com.example.vibemeet.services.UserPreferencesService;
import com.example.vibemeet.services.VisitHistoryService;
import com.example.vibemeet.views.AnimatedAvatarView;

import java.util.Map;
import java.util.Set;

public class ProfileFragment extends Fragment {

    private TextView userName, userEmail, statVisits, statSaved, statTopType, statMood;
    private LinearLayout interestsContainer, recentVisitsContainer;
    private Button btnEditPrefs, btnClearHistory, btnLogout, btnCustomizeAvatar, btnDigitalTwin, btnCreate3DTwin;
    private AnimatedAvatarView avatarView;

    private UserPreferencesService prefs;
    private FavoritesService favoritesService;
    private VisitHistoryService visitHistoryService;
    private AvatarService avatarService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = new UserPreferencesService(requireContext());
        favoritesService = new FavoritesService(requireContext());
        visitHistoryService = new VisitHistoryService(requireContext());
        avatarService = new AvatarService(requireContext());

        userName = view.findViewById(R.id.userName);
        userEmail = view.findViewById(R.id.userEmail);
        statVisits = view.findViewById(R.id.statVisits);
        statSaved = view.findViewById(R.id.statSaved);
        statTopType = view.findViewById(R.id.statTopType);
        statMood = view.findViewById(R.id.statMood);
        interestsContainer = view.findViewById(R.id.interestsContainer);
        recentVisitsContainer = view.findViewById(R.id.recentVisitsContainer);
        btnEditPrefs = view.findViewById(R.id.btnEditPrefs);
        btnClearHistory = view.findViewById(R.id.btnClearHistory);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnCustomizeAvatar = view.findViewById(R.id.btnCustomizeAvatar);
        btnDigitalTwin = view.findViewById(R.id.btnDigitalTwin);
        btnCreate3DTwin = view.findViewById(R.id.btnCreate3DTwin);
        avatarView = view.findViewById(R.id.avatarView);

        btnCustomizeAvatar.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AvatarSetupActivity.class)));

        btnDigitalTwin.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), DigitalTwinActivity.class)));

        btnCreate3DTwin.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), Avatar3DCreatorActivity.class)));

        // Tap avatar to customize
        avatarView.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AvatarSetupActivity.class));
        });
        TextView tapHint = view.findViewById(R.id.tapToEditHint);
        if (tapHint != null) {
            tapHint.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), AvatarSetupActivity.class));
            });
        }

        btnEditPrefs.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), OnboardingActivity.class);
            startActivity(intent);
        });

        btnClearHistory.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Clear history?")
                    .setMessage("This will remove all your check-ins. Saved favorites stay.")
                    .setPositiveButton("Clear", (dialog, which) -> {
                        visitHistoryService.clearHistory();
                        Toast.makeText(requireContext(), "History cleared ✓", Toast.LENGTH_SHORT).show();
                        loadProfileData();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnLogout.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Logout?")
                    .setMessage("Your saved venues and visit history will stay on this device.")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        // Clear all user data (all prefs files)
                        requireContext().getSharedPreferences("VibeMeetUserPrefs", 0).edit().clear().apply();
                        requireContext().getSharedPreferences("VibeMeetPrefs", 0).edit().clear().apply();
                        Intent intent = new Intent(requireContext(), LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileData();
        refreshAvatar();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (avatarView != null) avatarView.stopAnimating();
    }

    private void refreshAvatar() {
        if (avatarView == null) return;
        android.graphics.Bitmap bmp = avatarService.loadAvatar();
        avatarView.setAvatarBitmap(bmp);
        avatarView.setMood(prefs.getCurrentMood());
        avatarView.startAnimating();
    }

    private void loadProfileData() {
        userName.setText(prefs.getUserName());
        userEmail.setText(prefs.getUserEmail().isEmpty() ? "Casablanca Explorer" : prefs.getUserEmail());

        statVisits.setText(String.valueOf(visitHistoryService.getTotalVisits()));
        statSaved.setText(String.valueOf(favoritesService.getFavoriteCount()));
        statTopType.setText(visitHistoryService.getFavoriteType());
        statMood.setText(capitalize(prefs.getCurrentMood()));

        // Interests
        interestsContainer.removeAllViews();
        Set<String> interests = prefs.getInterests();
        if (interests.isEmpty()) {
            TextView noInterests = new TextView(requireContext());
            noInterests.setText("No interests set yet - tap edit to add some!");
            noInterests.setTextSize(13);
            noInterests.setTextColor(0xFF999999);
            interestsContainer.addView(noInterests);
        } else {
            LinearLayout row = null;
            int count = 0;
            for (String interest : interests) {
                if (count % 3 == 0) {
                    row = new LinearLayout(requireContext());
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    interestsContainer.addView(row, rowParams);
                }

                TextView chip = new TextView(requireContext());
                chip.setText(interest);
                chip.setTextSize(11);
                chip.setPadding(20, 8, 20, 8);
                chip.setBackgroundResource(R.drawable.chip_unselected_bg);
                chip.setTextColor(0xFF243B6B);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                params.setMargins(4, 4, 4, 4);
                chip.setLayoutParams(params);
                chip.setGravity(android.view.Gravity.CENTER);
                row.addView(chip);
                count++;
            }
        }

        // Recent visits
        recentVisitsContainer.removeAllViews();
        var recentVisits = visitHistoryService.getRecentVisits(5);
        if (recentVisits.isEmpty()) {
            TextView noVisits = new TextView(requireContext());
            noVisits.setText("No visits yet - explore some venues!");
            noVisits.setTextSize(13);
            noVisits.setTextColor(0xFF999999);
            recentVisitsContainer.addView(noVisits);
        } else {
            for (var v : recentVisits) {
                TextView item = new TextView(requireContext());
                item.setText(v.venueName + "  ·  " + formatTime(v.timestamp));
                item.setTextSize(13);
                item.setTextColor(0xFF1A1A2E);
                item.setPadding(0, 6, 0, 6);
                recentVisitsContainer.addView(item);
            }
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private String formatTime(long timestamp) {
        long diff = (System.currentTimeMillis() - timestamp) / 1000;
        if (diff < 60) return "just now";
        if (diff < 3600) return (diff / 60) + "m ago";
        if (diff < 86400) return (diff / 3600) + "h ago";
        return (diff / 86400) + "d ago";
    }

    private void unusedLegacyAvatar() {
        // (kept empty - animation now handled by AnimatedAvatarView)
    }
}
