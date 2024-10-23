package com.example.projecta;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.gms.maps.model.LatLngBounds;
import org.json.JSONArray;
import org.json.JSONObject;
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
    private PlacesClient placesClient;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);

        // Initialize MapView
        mapView = rootView.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Initialize FusedLocationProviderClient to get the user's location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize Places API client
        Places.initialize(requireContext(), "YOUR_GOOGLE_MAPS_API_KEY");
        placesClient = Places.createClient(requireContext());

        return rootView;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;

        // Check if permission is granted
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions if not granted
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Enable MyLocation Layer on the map
        gMap.setMyLocationEnabled(true);

        // Get the user's location and find nearby restaurants
        getDeviceLocationAndShowNearbyRestaurants();
    }

    private void getDeviceLocationAndShowNearbyRestaurants() {
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                    // Move the camera to the user's location
                    gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14));

                    // Find and pin nearby restaurants using the Places API Web Service
                    findNearbyRestaurants(userLocation);
                }
            }
        });
    }

    // Fetch nearby restaurants using Places API Web Service
    private void findNearbyRestaurants(LatLng location) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            OkHttpClient client = new OkHttpClient();

            String apiKey = "AIzaSyB9ERUPhy8YK4JaIR7T4R_viu0fak5cUHQ";
            String requestUrl = String.format(Locale.getDefault(),
                    "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=%f,%f&radius=1500&type=restaurant&key=%s",
                    location.latitude, location.longitude, apiKey);

            Request request = new Request.Builder().url(requestUrl).build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);
                    JSONArray results = json.getJSONArray("results");

                    requireActivity().runOnUiThread(() -> {
                        for (int i = 0; i < results.length(); i++) {
                            try {
                                JSONObject place = results.getJSONObject(i);
                                JSONObject geometry = place.getJSONObject("geometry");
                                JSONObject placeLocation = geometry.getJSONObject("location"); // Changed variable name

                                String placeName = place.getString("name");
                                LatLng placeLatLng = new LatLng(placeLocation.getDouble("lat"), placeLocation.getDouble("lng")); // Using renamed variable

                                // Add a marker for each nearby restaurant
                                gMap.addMarker(new MarkerOptions()
                                        .position(placeLatLng)
                                        .title(placeName)
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error fetching nearby restaurants.", Toast.LENGTH_SHORT).show();
                });
            }
        });
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
}
