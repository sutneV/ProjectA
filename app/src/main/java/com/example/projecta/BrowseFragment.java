package com.example.projecta;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BrowseFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private TextView locationTextView;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_browse, container, false);

        // Initialize UI components
        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);
        locationTextView = view.findViewById(R.id.location);  // TextView to show current location

        // Set up the ViewPager with a FragmentPagerAdapter
        setupViewPager(viewPager);
        tabLayout.setupWithViewPager(viewPager); // Link the TabLayout with ViewPager

        // Initialize FusedLocationProviderClient to get user location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Check location permission
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permission if not granted
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission is already granted, get the location
            getCurrentLocation();
        }

        return view;
    }

    // Helper method to set up the ViewPager with Fragments
    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getChildFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        adapter.addFragment(new ListFragment(), "List"); // First tab: List
        adapter.addFragment(new MapFragment(), "Map");   // Second tab: Map
        viewPager.setAdapter(adapter);
    }

    // Get the user's current location
    private void getCurrentLocation() {
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    // Get the latitude and longitude
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    // Reverse geocoding to get the address
                    Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                    try {
                        List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            Address address = addresses.get(0);
                            String locationText = address.getLocality() + ", " + address.getSubLocality() + " within 10 km";
                            locationTextView.setText(locationText);
                        } else {
                            locationTextView.setText("Location unavailable");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(requireContext(), "Unable to get location", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, fetch location
                getCurrentLocation();
            } else {
                // Permission denied, show a message
                Toast.makeText(requireContext(), "Location permission is needed to show your location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Custom ViewPagerAdapter class to manage Fragments
    private static class ViewPagerAdapter extends FragmentPagerAdapter {

        private final List<Fragment> fragmentList = new ArrayList<>();
        private final List<String> fragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentTitleList.get(position);
        }

        // Method to add fragments and their titles
        public void addFragment(Fragment fragment, String title) {
            fragmentList.add(fragment);
            fragmentTitleList.add(title);
        }
    }
}
