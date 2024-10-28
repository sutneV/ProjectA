package com.example.projecta;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoritesViewHolder> {

    private Context context;
    private List<Restaurant> favoriteRestaurants;
    private FirebaseFirestore db;
    private String userId;

    public FavoritesAdapter(Context context, List<Restaurant> favoriteRestaurants) {
        this.context = context;
        this.favoriteRestaurants = favoriteRestaurants;
        this.db = FirebaseFirestore.getInstance();
        this.userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public FavoritesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.favorites_restaurant_card, parent, false);
        return new FavoritesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoritesViewHolder holder, int position) {
        Restaurant restaurant = favoriteRestaurants.get(position);

        holder.restaurantName.setText(restaurant.getName());
        holder.restaurantDistance.setText(restaurant.getDistance());
        holder.restaurantPriceRange.setText(restaurant.getPriceRange());
        holder.restaurantTime.setText(restaurant.getTime());
        holder.restaurantPriceTag.setText(restaurant.getPriceTag());

        // Load the restaurant image
        Glide.with(context)
                .load(restaurant.getImageUrl())
                .placeholder(R.drawable.ic_placeholder_image)
                .into(holder.restaurantImage);

        // Display filled heart if favorited
        holder.favoriteIcon.setImageResource(R.drawable.ic_favorite_filled);

        // Handle favorite button click to remove from favorites
        holder.favoriteIcon.setOnClickListener(v -> {
            animateFavoriteIcon(holder.favoriteIcon);
            removeRestaurantFromFavorites(restaurant, position);
        });
    }

    @Override
    public int getItemCount() {
        return favoriteRestaurants.size();
    }

    // Remove restaurant from Firebase Firestore and update the list
    private void removeRestaurantFromFavorites(Restaurant restaurant, int position) {
        db.collection("users")
                .document(userId)
                .collection("favorites")
                .document(restaurant.getBusinessId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove the item from the list and notify the adapter
                    favoriteRestaurants.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, favoriteRestaurants.size());
                })
                .addOnFailureListener(e -> {
                    // Handle failure (optional)
                });
    }

    // Method to animate the favorite icon
    private void animateFavoriteIcon(ImageView favoriteIcon) {
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

    static class FavoritesViewHolder extends RecyclerView.ViewHolder {
        ImageView restaurantImage, favoriteIcon;
        TextView restaurantName, restaurantDistance, restaurantPriceRange, restaurantTime, restaurantPriceTag;

        public FavoritesViewHolder(@NonNull View itemView) {
            super(itemView);
            restaurantImage = itemView.findViewById(R.id.restaurant_image);
            favoriteIcon = itemView.findViewById(R.id.favorite_icon);
            restaurantName = itemView.findViewById(R.id.restaurant_name);
            restaurantDistance = itemView.findViewById(R.id.restaurant_distance);
            restaurantPriceRange = itemView.findViewById(R.id.restaurant_price_range);
            restaurantTime = itemView.findViewById(R.id.restaurant_time);
            restaurantPriceTag = itemView.findViewById(R.id.restaurant_price);
        }
    }
}
