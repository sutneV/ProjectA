package com.example.projecta;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap gMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private HashMap<String, Restaurant> restaurantMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);

        mapView = rootView.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        return rootView;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        gMap.setMyLocationEnabled(true);

        // Set the custom InfoWindowAdapter
        gMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(getContext()));

        gMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return true;
        });

        gMap.setOnInfoWindowClickListener(marker -> {
            Restaurant restaurant = (Restaurant) marker.getTag();
            if (restaurant != null) {
                Intent intent = new Intent(getContext(), RestaurantDetailsActivity.class);
                intent.putExtra("name", restaurant.getName());
                intent.putExtra("distance", restaurant.getDistance());
                intent.putExtra("priceRange", restaurant.getPriceRange());
                intent.putExtra("imageUrl", restaurant.getImageUrl());
                intent.putExtra("time", restaurant.getTime());
                intent.putExtra("businessId", restaurant.getBusinessId());
                intent.putExtra("latitude", restaurant.getLatitude());
                intent.putExtra("longitude", restaurant.getLongitude());
                startActivity(intent);
            }
        });

        getDeviceLocationAndShowNearbyRestaurants();
    }

    private void getDeviceLocationAndShowNearbyRestaurants() {
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14));
                fetchRestaurantsFromYelp(userLocation.latitude, userLocation.longitude);
            }
        });
    }

    private void fetchRestaurantsFromYelp(double latitude, double longitude) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            OkHttpClient client = new OkHttpClient();
            String apiKey = "Bearer 8nYrKNweT6ZiF8CLMnWfoINveoarRjAZkLYQjEUF519En4I2gSJgZVir26RVjdtZSUpwRYV2VgsauJqx6zKfaWTYsxrkePzHUUvy8iSflo8P5wkdIDoiEtGqdvswZ3Yx"; // Replace with your Yelp API key
            int offset = 0;
            int limit = 50; // Yelp API's max limit per request
            boolean moreResults = true;

            while (moreResults) {
                String requestUrl = String.format(Locale.getDefault(),
                        "https://api.yelp.com/v3/businesses/search?latitude=%f&longitude=%f&radius=40000&categories=restaurants&limit=%d&offset=%d",
                        latitude, longitude, limit, offset);

                Request request = new Request.Builder()
                        .url(requestUrl)
                        .addHeader("Authorization", apiKey)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        JSONObject json = new JSONObject(responseData);
                        JSONArray businesses = json.getJSONArray("businesses");

                        if (businesses.length() < limit) {
                            moreResults = false; // No more results if the response is smaller than the limit
                        }

                        requireActivity().runOnUiThread(() -> {
                            for (int i = 0; i < businesses.length(); i++) {
                                try {
                                    JSONObject business = businesses.getJSONObject(i);
                                    String name = business.getString("name");
                                    String id = business.getString("id"); // Yelp business ID
                                    String price = business.optString("price", "N/A"); // Get price or default to N/A

                                    // Extracting the list of category names
                                    JSONArray categoryArray = business.getJSONArray("categories");
                                    List<String> categories = new ArrayList<>();
                                    for (int j = 0; j < categoryArray.length(); j++) {
                                        JSONObject categoryObj = categoryArray.getJSONObject(j);
                                        categories.add(categoryObj.getString("title"));
                                    }

                                    JSONObject coordinates = business.getJSONObject("coordinates");
                                    double lat = coordinates.getDouble("latitude");
                                    double lng = coordinates.getDouble("longitude");

                                    // Create a Restaurant object and store it in the map
                                    Restaurant restaurant = new Restaurant(
                                            name,
                                            business.getString("image_url"),
                                            String.format("%.1f km", business.getDouble("distance") / 1000),
                                            price,
                                            business.getJSONObject("location").getString("address1"),
                                            "Today 19:00-21:00", // Default timing as shown in your example
                                            id,
                                            lat,
                                            lng,
                                            price,
                                            false,
                                            categories
                                    );

                                    restaurantMap.put(id, restaurant);

                                    // Add a marker with the restaurant data as tag
                                    LatLng restaurantLocation = new LatLng(lat, lng);
                                    Marker marker = gMap.addMarker(new MarkerOptions()
                                            .position(restaurantLocation)
                                            .title(name) // Title for info window display
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                                    marker.setTag(restaurant); // Tag marker with restaurant data
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        // Increment the offset for the next batch of results
                        offset += limit;
                    } else {
                        moreResults = false; // Stop if the response is unsuccessful
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    moreResults = false;
                }
            }
        });
    }


    public void filterMarkers(String query) {
        if (gMap == null) return;

        gMap.clear();  // Clear all existing markers

        if (query.isEmpty()) {
            // If query is empty, show all markers
            for (Restaurant restaurant : restaurantMap.values()) {
                addMarker(restaurant);
            }
        } else {
            // Otherwise, show only the markers that match the query
            for (Restaurant restaurant : restaurantMap.values()) {
                if (restaurant.getName().toLowerCase().contains(query.toLowerCase())) {
                    addMarker(restaurant);
                }
            }
        }
    }

    private void addMarker(Restaurant restaurant) {
        LatLng position = new LatLng(restaurant.getLatitude(), restaurant.getLongitude());
        Marker marker = gMap.addMarker(new MarkerOptions()
                .position(position)
                .title(restaurant.getName())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        marker.setTag(restaurant);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getDeviceLocationAndShowNearbyRestaurants();
        }
    }
}
