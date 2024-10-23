package com.example.projecta;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.HashMap;
import java.util.List;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.ViewHolder> {

    private Context context;
    private List<Restaurant> restaurantList;
    private HashMap<String, String> priceMap;  // HashMap to store the price for each restaurant

    public RestaurantAdapter(Context context, List<Restaurant> restaurantList, HashMap<String, String> priceMap) {
        this.context = context;
        this.restaurantList = restaurantList;
        this.priceMap = priceMap;  // Use the shared HashMap
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.restaurant_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Restaurant restaurant = restaurantList.get(position);

        holder.restaurantName.setText(restaurant.getName());
        holder.restaurantDistance.setText(restaurant.getDistance());

        // Retrieve the price from the shared HashMap and set it
        String priceTag = priceMap.get(restaurant.getBusinessId());
        holder.restaurantPrice.setText(priceTag);

        holder.restaurantTime.setText(restaurant.getTime());

        // Load the restaurant image using Glide
        Glide.with(context)
                .load(restaurant.getImageUrl())
                .into(holder.restaurantImage);

        // Set a click listener to navigate to the details screen
        holder.itemView.setOnClickListener(v -> {
            // Create an Intent to open the RestaurantDetailsActivity
            Intent intent = new Intent(context, RestaurantDetailsActivity.class);

            // Pass the restaurant details to the next activity
            intent.putExtra("name", restaurant.getName());
            intent.putExtra("distance", restaurant.getDistance());
            intent.putExtra("price", priceTag);  // Pass the price tag from the HashMap
            intent.putExtra("time", restaurant.getTime());
            intent.putExtra("imageUrl", restaurant.getImageUrl());
            intent.putExtra("businessId", restaurant.getBusinessId());
            intent.putExtra("latitude", restaurant.getLatitude());
            intent.putExtra("longitude", restaurant.getLongitude());

            // Start the RestaurantDetailsActivity
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return restaurantList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView restaurantName, restaurantDistance, restaurantPrice, restaurantTime;
        ImageView restaurantImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            restaurantName = itemView.findViewById(R.id.restaurant_name);
            restaurantDistance = itemView.findViewById(R.id.restaurant_distance);
            restaurantPrice = itemView.findViewById(R.id.restaurant_price);
            restaurantTime = itemView.findViewById(R.id.restaurant_time);
            restaurantImage = itemView.findViewById(R.id.restaurant_image);
        }
    }
}
