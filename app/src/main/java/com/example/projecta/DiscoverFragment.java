package com.example.projecta;

import android.Manifest;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DiscoverFragment extends Fragment {

    private TextView locationTextView;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    // Separate lists for each section
    private List<Restaurant> popularRestaurants;
    private List<Restaurant> nearbyRestaurants;
    private List<Restaurant> dinnerRestaurants;

    private RecyclerView section1RecyclerView;
    private RecyclerView section2RecyclerView;
    private RecyclerView section3RecyclerView;

    private HashMap<String, String> restaurantPrices = new HashMap<>();
    private HashMap<String, Boolean> favoriteMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        // Initialize UI components
        locationTextView = view.findViewById(R.id.location);

        section1RecyclerView = view.findViewById(R.id.recyclerView_section_1);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 3, GridLayoutManager.HORIZONTAL, false);
        section1RecyclerView.setLayoutManager(gridLayoutManager);

        section2RecyclerView = view.findViewById(R.id.recyclerView_section_2);
        section2RecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        section3RecyclerView = view.findViewById(R.id.recyclerView_section_3);
        section3RecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // Initialize FusedLocationProviderClient to get user location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Check location permission
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }

        return view;
    }

    // Get the user's current location
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
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

                    // Fetch data for each section
                    fetchPopularRestaurantsFromYelp(latitude, longitude);  // Section 1: Popular
                    fetchNearbyRestaurantsFromYelp(latitude, longitude);   // Section 2: Nearby
                    fetchDinnerRestaurantsFromYelp(latitude, longitude);   // Section 3: Dinner
                }
            }
        });
    }

    private String getOrGeneratePrice(String businessId, String sectionName) {
        if (restaurantPrices.containsKey(businessId)) {
            Log.d("PriceCheck", "Using existing price for business ID: " + businessId + " in section: " + sectionName);
            return restaurantPrices.get(businessId);
        } else {
            Random random = new Random();
            // Generate random integer between 2 and 9 and append .99
            int randomInt = 2 + random.nextInt(8);  // Generate random number between 2 and 9
            String formattedPriceTag = String.format("US$ %d.99", randomInt);
            restaurantPrices.put(businessId, formattedPriceTag);
            Log.d("PriceCheck", "Generated new price for business ID: " + businessId + " in section: " + sectionName + " Price: " + formattedPriceTag);
            return formattedPriceTag;
        }
    }


    // Section 1: Fetch popular restaurants
    private void fetchPopularRestaurantsFromYelp(double latitude, double longitude) {
        YelpApiService yelpApiService = getYelpApiService();

        String apiKey = "Bearer Raj1vqpkZqqenzxYM7SaeNEITjBQHCz7gCSNgqQjVKzXGd9TrNjakyhQRZRDhCZmS5CN87UZQU5v0UXNoyeWOnOfrXE8jy0_17nTPsOllvXD455mAGdzTmyLWCgVZ3Yx";  // Replace with your actual Yelp API key

        yelpApiService.getRestaurants(apiKey, latitude, longitude, "restaurants", 10000, 25)
                .enqueue(new Callback<YelpResponse>() {
                    @Override
                    public void onResponse(Call<YelpResponse> call, Response<YelpResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            popularRestaurants = new ArrayList<>();
                            Random random = new Random();
                            for (Business business : response.body().getBusinesses()) {
                                // Filter based on high ratings
                                if (business.getRating() >= 4) {
                                    String formattedPriceTag = getOrGeneratePrice(business.getId(), "Popular Section");
                                    popularRestaurants.add(new Restaurant(
                                            business.getName(),
                                            business.getImageUrl(),
                                            String.format("%.1f km", business.getDistance() / 1000),
                                            business.getPriceRange(),
                                            business.getLocation().getAddress1(),
                                            "Today 19:00-21:00",  // Placeholder for time
                                            business.getId(),
                                            business.getCoordinates().getLatitude(),  // Pass latitude
                                            business.getCoordinates().getLongitude(), // Pass longitude
                                            formattedPriceTag,
                                            false
                                    ));
                                }
                            }
                            section1RecyclerView.setAdapter(new RestaurantAdapter(getContext(), popularRestaurants, restaurantPrices));
                        }
                    }

                    @Override
                    public void onFailure(Call<YelpResponse> call, Throwable t) {
                        Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Section 2: Fetch nearby restaurants
    private void fetchNearbyRestaurantsFromYelp(double latitude, double longitude) {
        YelpApiService yelpApiService = getYelpApiService();

        String apiKey = "Bearer Raj1vqpkZqqenzxYM7SaeNEITjBQHCz7gCSNgqQjVKzXGd9TrNjakyhQRZRDhCZmS5CN87UZQU5v0UXNoyeWOnOfrXE8jy0_17nTPsOllvXD455mAGdzTmyLWCgVZ3Yx";  // Replace with your actual Yelp API key

        yelpApiService.getRestaurants(apiKey, latitude, longitude, "restaurants", 5000, 25)
                .enqueue(new Callback<YelpResponse>() {
                    @Override
                    public void onResponse(Call<YelpResponse> call, Response<YelpResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            nearbyRestaurants = new ArrayList<>();
                            Random random = new Random();
                            for (Business business : response.body().getBusinesses()) {
                                String formattedPriceTag = getOrGeneratePrice(business.getId(), "Popular Section");
                                boolean isFavorite = favoriteMap.containsKey(business.getId()) && favoriteMap.get(business.getId());


                                nearbyRestaurants.add(new Restaurant(
                                        business.getName(),
                                        business.getImageUrl(),
                                        String.format("%.1f km", business.getDistance() / 1000),
                                        business.getPriceRange(),
                                        business.getLocation().getAddress1(),
                                        "Today 14:00-14:45", // Example time
                                        business.getId(),
                                        business.getCoordinates().getLatitude(),  // Pass latitude
                                        business.getCoordinates().getLongitude(), // Pass longitude
                                        formattedPriceTag,
                                        isFavorite
                                ));
                            }
                            section2RecyclerView.setAdapter(new HorizontalRestaurantAdapter(getContext(), nearbyRestaurants, restaurantPrices, favoriteMap));
                        }
                    }

                    @Override
                    public void onFailure(Call<YelpResponse> call, Throwable t) {
                        Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Section 3: Fetch dinner restaurants
    private void fetchDinnerRestaurantsFromYelp(double latitude, double longitude) {
        YelpApiService yelpApiService = getYelpApiService();

        String apiKey = "Bearer Raj1vqpkZqqenzxYM7SaeNEITjBQHCz7gCSNgqQjVKzXGd9TrNjakyhQRZRDhCZmS5CN87UZQU5v0UXNoyeWOnOfrXE8jy0_17nTPsOllvXD455mAGdzTmyLWCgVZ3Yx";  // Replace with your actual Yelp API key

        yelpApiService.getRestaurants(apiKey, latitude, longitude, "restaurants", 7000, 25)
                .enqueue(new Callback<YelpResponse>() {
                    @Override
                    public void onResponse(Call<YelpResponse> call, Response<YelpResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            dinnerRestaurants = new ArrayList<>();
                            Random random = new Random();
                            for (Business business : response.body().getBusinesses()) {
                                String formattedPriceTag = getOrGeneratePrice(business.getId(), "Popular Section");
                                boolean isFavorite = favoriteMap.containsKey(business.getId()) && favoriteMap.get(business.getId());
                                // Filter by categories or time (this is an example, you could customize this logic)
                                dinnerRestaurants.add(new Restaurant(
                                        business.getName(),
                                        business.getImageUrl(),
                                        String.format("%.1f km", business.getDistance() / 1000),
                                        business.getPriceRange(),
                                        business.getLocation().getAddress1(),
                                        "Today 19:00-21:00",// Example time
                                        business.getId(),
                                        business.getCoordinates().getLatitude(),  // Pass latitude
                                        business.getCoordinates().getLongitude(), // Pass longitude
                                        formattedPriceTag,
                                        isFavorite
                                ));
                            }
                            section3RecyclerView.setAdapter(new HorizontalRestaurantAdapter(getContext(), dinnerRestaurants, restaurantPrices, favoriteMap));
                        }
                    }

                    @Override
                    public void onFailure(Call<YelpResponse> call, Throwable t) {
                        Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Yelp API Service setup
    private YelpApiService getYelpApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.yelp.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(YelpApiService.class);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(requireContext(), "Location permission is needed to show your location", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
