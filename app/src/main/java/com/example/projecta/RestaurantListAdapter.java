package com.example.projecta;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;

public class RestaurantListAdapter extends RecyclerView.Adapter<RestaurantListAdapter.ViewHolder> {

    private Context context;
    private List<Restaurant> restaurantList;
    private HashMap<String, String> priceMap;
    private HashMap<String, Boolean> favoriteMap;

    public RestaurantListAdapter(Context context, List<Restaurant> restaurantList, HashMap<String, String> priceMap, HashMap<String, Boolean> favoriteMap) {
        this.context = context;
        this.restaurantList = restaurantList;
        this.priceMap = priceMap;
        this.favoriteMap = favoriteMap;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_restaurant_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Restaurant restaurant = restaurantList.get(position);

        holder.restaurantName.setText(restaurant.getName());
        holder.restaurantDistance.setText(restaurant.getDistance());
        String priceTag = priceMap.get(restaurant.getBusinessId());
        holder.restaurantPriceTag.setText(priceTag);
        holder.restaurantPriceRange.setText(restaurant.getPriceRange() != null ? restaurant.getPriceRange() : "Price range unknown");
        holder.restaurantTime.setText(restaurant.getTime() != null ? restaurant.getTime() : "Time not available");

        Glide.with(context)
                .load(restaurant.getImageUrl())
                .placeholder(R.drawable.ic_placeholder_image)
                .into(holder.restaurantImage);

        ViewCompat.setTransitionName(holder.restaurantImage, "restaurantImageTransition");

        boolean isFavorite = favoriteMap.containsKey(restaurant.getBusinessId()) && favoriteMap.get(restaurant.getBusinessId());
        holder.favoriteIcon.setImageResource(isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline);

        holder.favoriteIcon.setOnClickListener(v -> {
            boolean currentFavoriteStatus = favoriteMap.getOrDefault(restaurant.getBusinessId(), false);
            boolean newFavoriteStatus = !currentFavoriteStatus;
            favoriteMap.put(restaurant.getBusinessId(), newFavoriteStatus);
            animateFavoriteIcon(holder.favoriteIcon, newFavoriteStatus);
            notifyFavoriteStatusChanged(restaurant.getBusinessId(), newFavoriteStatus);
            updateFavoriteStatusInFirestore(restaurant, newFavoriteStatus);
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RestaurantDetailsActivity.class);
            intent.putExtra("name", restaurant.getName());
            intent.putExtra("distance", restaurant.getDistance());
            intent.putExtra("priceTag", priceTag);
            intent.putExtra("priceRange", restaurant.getPriceRange());
            intent.putExtra("imageUrl", restaurant.getImageUrl());
            intent.putExtra("time", restaurant.getTime());
            intent.putExtra("businessId", restaurant.getBusinessId());
            intent.putExtra("latitude", restaurant.getLatitude());
            intent.putExtra("longitude", restaurant.getLongitude());

            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    (Activity) context,
                    holder.restaurantImage, // The shared element view
                    ViewCompat.getTransitionName(holder.restaurantImage) // The transition name
            );
            context.startActivity(intent, options.toBundle());

        });
    }

    private void animateFavoriteIcon(ImageView favoriteIcon, boolean isFavorite) {
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(favoriteIcon, "scaleX", 1f, 1.2f);
        ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(favoriteIcon, "scaleY", 1f, 1.2f);
        ObjectAnimator scaleXDown = ObjectAnimator.ofFloat(favoriteIcon, "scaleX", 1.2f, 1f);
        ObjectAnimator scaleYDown = ObjectAnimator.ofFloat(favoriteIcon, "scaleY", 1.2f, 1f);
        animatorSet.play(scaleXUp).with(scaleYUp);
        animatorSet.play(scaleXDown).with(scaleYDown).after(scaleXUp);
        animatorSet.setDuration(300);
        animatorSet.start();
    }

    private void notifyFavoriteStatusChanged(String businessId, boolean isFavorite) {
        for (int i = 0; i < restaurantList.size(); i++) {
            if (restaurantList.get(i).getBusinessId().equals(businessId)) {
                notifyItemChanged(i);
            }
        }
    }

    private void updateFavoriteStatusInFirestore(Restaurant restaurant, boolean isFavorite) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String businessId = restaurant.getBusinessId();

        if (isFavorite) {
            db.collection("users")
                    .document(userId)
                    .collection("favorites")
                    .document(businessId)
                    .set(restaurant.toMap())
                    .addOnSuccessListener(aVoid -> {
                        // Successfully added to Firestore
                    })
                    .addOnFailureListener(e -> {
                        // Failed to add to Firestore
                    });
        } else {
            db.collection("users")
                    .document(userId)
                    .collection("favorites")
                    .document(businessId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Successfully removed from Firestore
                    })
                    .addOnFailureListener(e -> {
                        // Failed to remove from Firestore
                    });
        }
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
            favoriteIcon = itemView.findViewById(R.id.favorite_icon);
        }
    }
}
