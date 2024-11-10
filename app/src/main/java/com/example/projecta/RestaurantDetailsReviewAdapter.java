package com.example.projecta;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RestaurantDetailsReviewAdapter extends RecyclerView.Adapter<RestaurantDetailsReviewAdapter.ReviewViewHolder> {

    private final List<Pair<Review, String>> reviewsWithUsernames;
    private final Context context;

    public RestaurantDetailsReviewAdapter(Context context, List<Pair<Review, String>> reviewsWithUsernames) {
        this.context = context;
        this.reviewsWithUsernames = reviewsWithUsernames;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Pair<Review, String> reviewWithUsername = reviewsWithUsernames.get(position);
        Review review = reviewWithUsername.first;
        String username = reviewWithUsername.second;

        holder.restaurantName.setText(username); // Use the username instead of restaurant name
        String formattedReviewText = "\"" + review.getReviewText() + "\"";
        holder.reviewText.setText(formattedReviewText);

        holder.setStarRating(review.getStarRating());
    }

    @Override
    public int getItemCount() {
        return reviewsWithUsernames.size();
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        private final TextView restaurantName;
        private final TextView reviewText;
        private final LinearLayout starRatingContainer;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            restaurantName = itemView.findViewById(R.id.restaurant_name);
            reviewText = itemView.findViewById(R.id.review_text);
            starRatingContainer = itemView.findViewById(R.id.star_rating_container);
        }

        public void setStarRating(int starRating) {
            starRatingContainer.removeAllViews();

            for (int i = 1; i <= 5; i++) {
                ImageView star = new ImageView(itemView.getContext());
                if (i <= starRating) {
                    star.setImageResource(R.drawable.ic_star_filled_reviews);
                } else {
                    star.setImageResource(R.drawable.ic_star_empty_reviews);
                }

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 4, 0);
                star.setLayoutParams(params);

                starRatingContainer.addView(star);
            }
        }
    }
}
