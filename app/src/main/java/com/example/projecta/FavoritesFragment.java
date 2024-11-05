package com.example.projecta;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FavoritesFragment extends Fragment {

    private RecyclerView favoritesRecyclerView;
    private List<Restaurant> favoriteRestaurants;
    private FavoritesAdapter favoritesAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        // Initialize RecyclerView and adapter
        favoritesRecyclerView = view.findViewById(R.id.recyclerView_favorites);
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        favoriteRestaurants = new ArrayList<>();

        // Set up the adapter with the item click listener to open RestaurantDetailsActivity
        favoritesAdapter = new FavoritesAdapter(getContext(), favoriteRestaurants);
        favoritesRecyclerView.setAdapter(favoritesAdapter);

        loadFavoriteRestaurants(); // Start loading favorites
        return view;
    }

    // Method to load favorite business IDs from Firestore
    private void loadFavoriteRestaurants() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(userId).collection("favorites")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        favoriteRestaurants.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            Restaurant restaurant = Restaurant.fromDocument(document);
                            favoriteRestaurants.add(restaurant);
                        }
                        // Notify the adapter after updating the list
                        favoritesAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(requireContext(), "Failed to load favorites", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Fetch restaurant details from Yelp API for each favorite business ID
    private void fetchFavoriteRestaurantDetailsFromYelp(List<String> favoriteBusinessIds) {
        YelpApiService yelpApiService = getYelpApiService();
        String apiKey = "Bearer Raj1vqpkZqqenzxYM7SaeNEITjBQHCz7gCSNgqQjVKzXGd9TrNjakyhQRZRDhCZmS5CN87UZQU5v0UXNoyeWOnOfrXE8jy0_17nTPsOllvXD455mAGdzTmyLWCgVZ3Yx"; // Replace with your actual Yelp API key

        favoriteRestaurants.clear();

        for (String businessId : favoriteBusinessIds) {
            yelpApiService.getRestaurantDetails(apiKey, businessId).enqueue(new Callback<Restaurant>() {
                @Override
                public void onResponse(Call<Restaurant> call, Response<Restaurant> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Restaurant restaurant = response.body();
                        favoriteRestaurants.add(restaurant);

                        // Update RecyclerView once each restaurant is fetched
                        favoritesAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onFailure(Call<Restaurant> call, Throwable t) {
                    Log.e("FavoritesFragment", "Failed to fetch restaurant details: " + t.getMessage());
                }
            });
        }
    }

    // Set up Retrofit to communicate with the Yelp API
    private YelpApiService getYelpApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.yelp.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(YelpApiService.class);
    }
}
