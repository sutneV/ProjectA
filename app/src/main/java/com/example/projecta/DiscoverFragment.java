package com.example.projecta;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DiscoverFragment extends Fragment {

    private static final long CACHE_VALIDITY_DURATION = 24 * 60 * 60 * 1000; // 24 hours
    private static final String PREF_NAME = "DiscoverCachePrefs";
    private static final String PREF_LAST_POPULAR_FETCH = "lastPopularFetch";
    private static final String PREF_LAST_NEARBY_FETCH = "lastNearbyFetch";
    private static final String PREF_LAST_DINNER_FETCH = "lastDinnerFetch";

    private SharedPreferences sharedPreferences;
    private TextView locationTextView;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private List<Restaurant> popularRestaurants;
    private List<Restaurant> nearbyRestaurants;
    private List<Restaurant> dinnerRestaurants;

    private RecyclerView section1RecyclerView;
    private RecyclerView section2RecyclerView;
    private RecyclerView section3RecyclerView;

    private HashMap<String, String> restaurantPrices = new HashMap<>();
    private HashMap<String, Boolean> favoriteMap = new HashMap<>();
    private HashSet<String> addedBusinessIds = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        locationTextView = view.findViewById(R.id.location);

        section1RecyclerView = view.findViewById(R.id.recyclerView_section_1);
        section1RecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3, GridLayoutManager.HORIZONTAL, false));

        section2RecyclerView = view.findViewById(R.id.recyclerView_section_2);
        section2RecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        section3RecyclerView = view.findViewById(R.id.recyclerView_section_3);
        section3RecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            loadFavoritesFromFirestore();
        }

        return view;
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        String locationText = address.getLocality() + ", " + address.getSubLocality() + " within 20 km";
                        locationTextView.setText(locationText);
                    } else {
                        locationTextView.setText("Location unavailable");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(requireContext(), "Unable to get location", Toast.LENGTH_SHORT).show();
                }

                fetchRestaurants(latitude, longitude);
            }
        });
    }

    private void loadFavoritesFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(userId).collection("favorites")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        favoriteMap.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            String businessId = document.getId();
                            favoriteMap.put(businessId, true);
                        }
                        getCurrentLocation();
                    } else {
                        Toast.makeText(requireContext(), "Failed to load favorites", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchRestaurants(double latitude, double longitude) {
        fetchPopularRestaurantsFromYelp(latitude, longitude);
        fetchNearbyRestaurantsFromYelp(latitude, longitude);
        fetchDinnerRestaurantsFromYelp(latitude, longitude);
    }

    private void fetchPopularRestaurantsFromYelp(double latitude, double longitude) {
        long lastFetchTime = sharedPreferences.getLong(PREF_LAST_POPULAR_FETCH, 0);
        if (System.currentTimeMillis() - lastFetchTime < CACHE_VALIDITY_DURATION && popularRestaurants != null && !popularRestaurants.isEmpty()) {
            section1RecyclerView.setAdapter(new RestaurantAdapter(getContext(), popularRestaurants, restaurantPrices));
            return;
        }

        YelpApiService yelpApiService = getYelpApiService();
        String apiKey = "Bearer YOUR_API_KEY";

        yelpApiService.getRestaurants(apiKey, latitude, longitude, "restaurants", 10000, 50)
                .enqueue(new Callback<YelpResponse>() {
                    @Override
                    public void onResponse(Call<YelpResponse> call, Response<YelpResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            popularRestaurants = new ArrayList<>();
                            for (Business business : response.body().getBusinesses()) {
                                if (business.getRating() >= 4 && !addedBusinessIds.contains(business.getId())) {

                                    // Generate or retrieve the price tag
                                    String formattedPriceTag = getOrGeneratePrice(business.getId(), "Popular Section");
                                    boolean isFavorite = favoriteMap.getOrDefault(business.getId(), false);

                                    // Extract categories as a List<String>
                                    List<String> categories = new ArrayList<>();
                                    if (business.getCategories() != null) {
                                        for (Business.Category category : business.getCategories()) {
                                            categories.add(category.getTitle());
                                        }
                                    }

                                    // Create and add the Restaurant object
                                    popularRestaurants.add(new Restaurant(
                                            business.getName(),
                                            business.getImageUrl(),
                                            String.format("%.1f km", business.getDistance() / 1000),
                                            business.getPriceRange(),
                                            business.getLocation().getAddress1(),
                                            "Today 19:00-21:00", // Default time or dynamic if available
                                            business.getId(),
                                            business.getCoordinates().getLatitude(),
                                            business.getCoordinates().getLongitude(),
                                            formattedPriceTag,
                                            isFavorite,
                                            categories // Pass the list of categories
                                    ));

                                    addedBusinessIds.add(business.getId());
                                }
                            }
                            // Set the adapter with the populated list
                            section1RecyclerView.setAdapter(new RestaurantAdapter(getContext(), popularRestaurants, restaurantPrices));
                            // Update the cache timestamp
                            sharedPreferences.edit().putLong(PREF_LAST_POPULAR_FETCH, System.currentTimeMillis()).apply();
                        }
                    }

                    @Override
                    public void onFailure(Call<YelpResponse> call, Throwable t) {
                        Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void fetchNearbyRestaurantsFromYelp(double latitude, double longitude) {
        long lastFetchTime = sharedPreferences.getLong(PREF_LAST_NEARBY_FETCH, 0);
        if (System.currentTimeMillis() - lastFetchTime < CACHE_VALIDITY_DURATION && nearbyRestaurants != null && !nearbyRestaurants.isEmpty()) {
            section2RecyclerView.setAdapter(new HorizontalRestaurantAdapter(getContext(), nearbyRestaurants, restaurantPrices, favoriteMap));
            return;
        }

        YelpApiService yelpApiService = getYelpApiService();
        String apiKey = "Bearer YOUR_API_KEY";

        yelpApiService.getRestaurants(apiKey, latitude, longitude, "restaurants", 5000, 50)
                .enqueue(new Callback<YelpResponse>() {
                    @Override
                    public void onResponse(Call<YelpResponse> call, Response<YelpResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            nearbyRestaurants = new ArrayList<>();
                            for (Business business : response.body().getBusinesses()) {
                                if (!addedBusinessIds.contains(business.getId())) {
                                    String formattedPriceTag = getOrGeneratePrice(business.getId(), "Nearby Section");
                                    boolean isFavorite = favoriteMap.getOrDefault(business.getId(), false);

                                    // Extract category names as a List of Strings
                                    List<String> categoryNames = new ArrayList<>();
                                    for (Business.Category category : business.getCategories()) {
                                        categoryNames.add(category.getTitle()); // Assuming getTitle() returns category name
                                    }

                                    // Create and add the Restaurant object to the list
                                    nearbyRestaurants.add(new Restaurant(
                                            business.getName(),
                                            business.getImageUrl(),
                                            String.format("%.1f km", business.getDistance() / 1000),
                                            business.getPriceRange(),
                                            business.getLocation().getAddress1(),
                                            "Today 14:00-14:45",
                                            business.getId(),
                                            business.getCoordinates().getLatitude(),
                                            business.getCoordinates().getLongitude(),
                                            formattedPriceTag,
                                            isFavorite,
                                            categoryNames // Pass the list of category names here
                                    ));
                                    addedBusinessIds.add(business.getId());
                                }
                            }
                            section2RecyclerView.setAdapter(new HorizontalRestaurantAdapter(getContext(), nearbyRestaurants, restaurantPrices, favoriteMap));
                            sharedPreferences.edit().putLong(PREF_LAST_NEARBY_FETCH, System.currentTimeMillis()).apply();
                        }
                    }

                    @Override
                    public void onFailure(Call<YelpResponse> call, Throwable t) {
                        Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void fetchDinnerRestaurantsFromYelp(double latitude, double longitude) {
        long lastFetchTime = sharedPreferences.getLong(PREF_LAST_DINNER_FETCH, 0);
        if (System.currentTimeMillis() - lastFetchTime < CACHE_VALIDITY_DURATION && dinnerRestaurants != null && !dinnerRestaurants.isEmpty()) {
            section3RecyclerView.setAdapter(new HorizontalRestaurantAdapter(getContext(), dinnerRestaurants, restaurantPrices, favoriteMap));
            return;
        }

        YelpApiService yelpApiService = getYelpApiService();
        String apiKey = "Bearer YOUR_API_KEY";

        yelpApiService.getRestaurants(apiKey, latitude, longitude, "restaurants", 7000, 50)
                .enqueue(new Callback<YelpResponse>() {
                    @Override
                    public void onResponse(Call<YelpResponse> call, Response<YelpResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            dinnerRestaurants = new ArrayList<>();
                            for (Business business : response.body().getBusinesses()) {
                                if (!addedBusinessIds.contains(business.getId())) {
                                    String formattedPriceTag = getOrGeneratePrice(business.getId(), "Dinner Section");
                                    boolean isFavorite = favoriteMap.getOrDefault(business.getId(), false);

                                    // Extract category names as a List of Strings
                                    List<String> categoryNames = new ArrayList<>();
                                    for (Business.Category category : business.getCategories()) {
                                        categoryNames.add(category.getTitle()); // Assuming getTitle() returns category name
                                    }

                                    // Create and add the Restaurant object to the list
                                    dinnerRestaurants.add(new Restaurant(
                                            business.getName(),
                                            business.getImageUrl(),
                                            String.format("%.1f km", business.getDistance() / 1000),
                                            business.getPriceRange(),
                                            business.getLocation().getAddress1(),
                                            "Today 19:00-21:00",
                                            business.getId(),
                                            business.getCoordinates().getLatitude(),
                                            business.getCoordinates().getLongitude(),
                                            formattedPriceTag,
                                            isFavorite,
                                            categoryNames // Pass the list of category names here
                                    ));
                                    addedBusinessIds.add(business.getId());
                                }
                            }
                            section3RecyclerView.setAdapter(new HorizontalRestaurantAdapter(getContext(), dinnerRestaurants, restaurantPrices, favoriteMap));
                            sharedPreferences.edit().putLong(PREF_LAST_DINNER_FETCH, System.currentTimeMillis()).apply();
                        }
                    }

                    @Override
                    public void onFailure(Call<YelpResponse> call, Throwable t) {
                        Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private YelpApiService getYelpApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.yelp.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(YelpApiService.class);
    }

    private String getOrGeneratePrice(String businessId, String sectionName) {
        if (restaurantPrices.containsKey(businessId)) {
            return restaurantPrices.get(businessId);
        } else {
            String price = "MYR " + "14.99";
            restaurantPrices.put(businessId, price);
            return price;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFavoritesFromFirestore();
            } else {
                Toast.makeText(requireContext(), "Location permission is needed to show your location", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

/*
Raj1vqpkZqqenzxYM7SaeNEITjBQHCz7gCSNgqQjVKzXGd9TrNjakyhQRZRDhCZmS5CN87UZQU5v0UXNoyeWOnOfrXE8jy0_17nTPsOllvXD455mAGdzTmyLWCgVZ3Yx*/
