package com.example.projecta;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.projecta.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Load the default fragment (BrowseFragment) at start
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DiscoverFragment())
                    .commit();
        }

        // Set up listener for bottom navigation item clicks using lambda
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.navigation_discover) {
                selectedFragment = new DiscoverFragment();
            } else if (item.getItemId() == R.id.navigation_browse) {
                selectedFragment = new BrowseFragment();
            } else if (item.getItemId() == R.id.navigation_favorites) {
                selectedFragment = new FavoritesFragment();
            } else if (item.getItemId() == R.id.navigation_more) {
                selectedFragment = new MoreFragment();
            }

            if (selectedFragment != null) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, selectedFragment);
                transaction.addToBackStack(null); // Allows user to go back to previous fragment
                transaction.commit();
            }

            return true;
        });
    }
}
