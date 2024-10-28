package com.example.projecta;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
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

public class ListFragment extends Fragment {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private RecyclerView recyclerView;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private List<Restaurant> restaurantList = new ArrayList<>();
    private List<Restaurant> originalRestaurantList = new ArrayList<>(); // Backup list for resetting after filtering
    private HashMap<String, Boolean> favoriteMap = new HashMap<>();
    private HashMap<String, String> restaurantPrices = new HashMap<>();
    private RestaurantListAdapter adapter;
    private HashSet<String> addedBusinessIds = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        recyclerView = view.findViewById(R.id.restaurant_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new RestaurantListAdapter(getContext(), restaurantList, restaurantPrices, favoriteMap);
        recyclerView.setAdapter(adapter);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            loadFavoritesFromFirestore();
        }

        return view;
    }

    // Load favorites from Firestore
    private void loadFavoritesFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(userId).collection("favorites")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        favoriteMap.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            favoriteMap.put(document.getId(), true);
                        }
                        getCurrentLocation();
                    } else {
                        Toast.makeText(requireContext(), "Failed to load favorites", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Request current location and fetch restaurants
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                fetchRestaurantsFromYelp(location.getLatitude(), location.getLongitude());
            } else {
                Toast.makeText(requireContext(), "Failed to get location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Fetch restaurants with pagination support
    private void fetchRestaurantsFromYelp(double latitude, double longitude) {
        fetchPaginatedRestaurantsFromYelp(latitude, longitude, 0);
    }

    private void fetchPaginatedRestaurantsFromYelp(double latitude, double longitude, int offset) {
        YelpApiService yelpApiService = getYelpApiService();
        String apiKey = "Bearer Raj1vqpkZqqenzxYM7SaeNEITjBQHCz7gCSNgqQjVKzXGd9TrNjakyhQRZRDhCZmS5CN87UZQU5v0UXNoyeWOnOfrXE8jy0_17nTPsOllvXD455mAGdzTmyLWCgVZ3Yx"; // Replace with your actual Yelp API key

        yelpApiService.getRestaurants(apiKey, latitude, longitude, "restaurants", 10000, 50, offset)
                .enqueue(new Callback<YelpResponse>() {
                    @Override
                    public void onResponse(Call<YelpResponse> call, Response<YelpResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Business> businesses = response.body().getBusinesses();

                            for (Business business : businesses) {
                                if (!addedBusinessIds.contains(business.getId())) {
                                    boolean isFavorite = favoriteMap.getOrDefault(business.getId(), false);

                                    String formattedPriceTag = getOrGeneratePrice(business.getId());

                                    Restaurant restaurant = new Restaurant(
                                            business.getName(),
                                            business.getImageUrl(),
                                            String.format(Locale.getDefault(), "%.1f km", business.getDistance() / 1000),
                                            business.getPriceRange(),
                                            business.getLocation().getAddress1(),
                                            "Today 19:00-21:00",
                                            business.getId(),
                                            business.getCoordinates().getLatitude(),
                                            business.getCoordinates().getLongitude(),
                                            formattedPriceTag,
                                            isFavorite
                                    );

                                    restaurantList.add(restaurant);
                                    originalRestaurantList.add(restaurant); // Add to backup list for filtering
                                    addedBusinessIds.add(business.getId());
                                }
                            }
                            adapter.notifyDataSetChanged();

                            if (businesses.size() == 50) {
                                fetchPaginatedRestaurantsFromYelp(latitude, longitude, offset + 50);
                            }
                        } else {
                            Toast.makeText(requireContext(), "Failed to fetch restaurants", Toast.LENGTH_SHORT).show();
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

    // Generates or retrieves the price tag for a restaurant
    private String getOrGeneratePrice(String businessId) {
        if (restaurantPrices.containsKey(businessId)) {
            return restaurantPrices.get(businessId);
        } else {
            String price = "US$ " + (2 + new Random().nextInt(8)) + ".99";
            restaurantPrices.put(businessId, price);
            return price;
        }
    }

    // Filter the list based on the search query
    public void filterList(String query) {
        if (query.isEmpty()) {
            restaurantList.clear();
            restaurantList.addAll(originalRestaurantList);
        } else {
            List<Restaurant> filteredList = new ArrayList<>();
            for (Restaurant restaurant : originalRestaurantList) {
                if (restaurant.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(restaurant);
                }
            }
            restaurantList.clear();
            restaurantList.addAll(filteredList);
        }
        adapter.notifyDataSetChanged();
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
