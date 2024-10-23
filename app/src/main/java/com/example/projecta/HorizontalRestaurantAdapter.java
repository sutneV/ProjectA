package com.example.projecta;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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

public class HorizontalRestaurantAdapter extends RecyclerView.Adapter<HorizontalRestaurantAdapter.ViewHolder> {

    private Context context;
    private List<Restaurant> restaurantList;
    private HashMap<String, String> priceMap;  // HashMap to store the price for each restaurant
    private HashMap<String, Boolean> favoriteMap;  // Initialize the favoriteMap

    public HorizontalRestaurantAdapter(Context context, List<Restaurant> restaurantList, HashMap<String, String> priceMap, HashMap<String, Boolean> favoriteMap) {
        this.context = context;
        this.restaurantList = restaurantList;
        this.priceMap = priceMap; // Use the shared HashMap
        this.favoriteMap = favoriteMap; // Use the shared favorite map
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.horizontal_restaurant_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Restaurant restaurant = restaurantList.get(position);

        // Set restaurant details
        holder.restaurantName.setText(restaurant.getName());
        holder.restaurantDistance.setText(restaurant.getDistance());

        // Retrieve and display the price from the shared priceMap
        String priceTag = priceMap.get(restaurant.getBusinessId());
        holder.restaurantPriceTag.setText(priceTag);

        // Set the price range
        holder.restaurantPriceRange.setText(restaurant.getPriceRange() != null ? restaurant.getPriceRange() : "Price range unknown");

        // Set the restaurant time
        holder.restaurantTime.setText(restaurant.getTime() != null ? restaurant.getTime() : "Time not available");

        // Load restaurant image
        Glide.with(context)
                .load(restaurant.getImageUrl())
                .placeholder(R.drawable.ic_placeholder_image)
                .into(holder.restaurantImage);

        // Set the favorite icon based on the restaurant's favorite status from favoriteMap
        boolean isFavorite = favoriteMap.containsKey(restaurant.getBusinessId()) && favoriteMap.get(restaurant.getBusinessId());
        holder.favoriteIcon.setImageResource(isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline);

        // Set click listener for the favorite button
        holder.favoriteIcon.setOnClickListener(v -> {
            boolean currentFavoriteStatus = favoriteMap.getOrDefault(restaurant.getBusinessId(), false);
            boolean newFavoriteStatus = !currentFavoriteStatus;

            // Update the favorite status in the map
            favoriteMap.put(restaurant.getBusinessId(), newFavoriteStatus);

            // Apply the animation and update the UI
            animateFavoriteIcon(holder.favoriteIcon, newFavoriteStatus);
            notifyItemChanged(position);
        });

        // Set a click listener to navigate to the details screen
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RestaurantDetailsActivity.class);

            // Pass the restaurant details to the next activity
            intent.putExtra("name", restaurant.getName());
            intent.putExtra("distance", restaurant.getDistance());
            intent.putExtra("priceTag", priceTag);
            intent.putExtra("priceRange", restaurant.getPriceRange());
            intent.putExtra("imageUrl", restaurant.getImageUrl());
            intent.putExtra("time", restaurant.getTime());
            intent.putExtra("businessId", restaurant.getBusinessId());
            intent.putExtra("latitude", restaurant.getLatitude());
            intent.putExtra("longitude", restaurant.getLongitude());

            // Start the RestaurantDetailsActivity
            context.startActivity(intent);
        });
    }

    // Method to animate the favorite icon
    private void animateFavoriteIcon(ImageView favoriteIcon, boolean isFavorite) {
        AnimatorSet animatorSet = new AnimatorSet();

        // Scale up animation
        ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(favoriteIcon, "scaleX", 1f, 1.2f);
        ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(favoriteIcon, "scaleY", 1f, 1.2f);

        // Scale down animation
        ObjectAnimator scaleXDown = ObjectAnimator.ofFloat(favoriteIcon, "scaleX", 1.2f, 1f);
        ObjectAnimator scaleYDown = ObjectAnimator.ofFloat(favoriteIcon, "scaleY", 1.2f, 1f);

        // Combine animations and start
        animatorSet.play(scaleXUp).with(scaleYUp);
        animatorSet.play(scaleXDown).with(scaleYDown).after(scaleXUp);
        animatorSet.setDuration(300);
        animatorSet.start();
    }

    @Override
    public int getItemCount() {
        return restaurantList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView restaurantName, restaurantDistance, restaurantPriceRange, restaurantTime, restaurantPriceTag;
        ImageView restaurantImage, favoriteIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            restaurantName = itemView.findViewById(R.id.restaurant_name);
            restaurantDistance = itemView.findViewById(R.id.restaurant_distance);
            restaurantPriceTag = itemView.findViewById(R.id.restaurant_price);
            restaurantPriceRange = itemView.findViewById(R.id.restaurant_price_range);
            restaurantTime = itemView.findViewById(R.id.restaurant_time);
            restaurantImage = itemView.findViewById(R.id.restaurant_image);
            favoriteIcon = itemView.findViewById(R.id.favorite_icon);  // Initialize favoriteIcon
        }
    }
}
