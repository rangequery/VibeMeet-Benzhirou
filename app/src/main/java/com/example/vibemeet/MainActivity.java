package com.example.vibemeet;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.vibemeet.fragments.ChatFragment;
import com.example.vibemeet.fragments.DiscoverFragment;
import com.example.vibemeet.fragments.MapFragment;
import com.example.vibemeet.fragments.SavedFragment;
import com.example.vibemeet.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_TAB = "open_tab";
    public static final String EXTRA_CHAT_PROMPT = "chat_prompt";
    public static final String TAB_CHAT = "chat";

    private BottomNavigationView bottomNav;
    private String pendingChatPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_nav);

        bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            int id = item.getItemId();
            if (id == R.id.nav_discover) selected = new DiscoverFragment();
            else if (id == R.id.nav_map) selected = new MapFragment();
            else if (id == R.id.nav_chat) {
                selected = new ChatFragment();
                if (pendingChatPrompt != null && !pendingChatPrompt.trim().isEmpty()) {
                    Bundle args = new Bundle();
                    args.putString(EXTRA_CHAT_PROMPT, pendingChatPrompt);
                    selected.setArguments(args);
                    pendingChatPrompt = null;
                }
            }
            else if (id == R.id.nav_saved) selected = new SavedFragment();
            else if (id == R.id.nav_profile) selected = new ProfileFragment();
            return selected != null && loadFragment(selected);
        });

        if (savedInstanceState == null) {
            if (TAB_CHAT.equals(getIntent().getStringExtra(EXTRA_OPEN_TAB))) {
                pendingChatPrompt = getIntent().getStringExtra(EXTRA_CHAT_PROMPT);
                bottomNav.setSelectedItemId(R.id.nav_chat);
            } else {
                bottomNav.setSelectedItemId(R.id.nav_discover);
            }
        }
    }

    public void openChatTab(String prompt) {
        pendingChatPrompt = prompt;
        bottomNav.setSelectedItemId(R.id.nav_chat);
    }

    private boolean loadFragment(Fragment fragment) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.fragmentContainer, fragment);
        tx.commit();
        return true;
    }
}
