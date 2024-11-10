package com.example.projecta;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder> {

    private final List<Review> reviewList;
    private final Context context;

    public ReviewsAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);

        // Set restaurant name and review text
        holder.restaurantName.setText(review.getRestaurantName());

        String formattedReviewText = "\"" + review.getReviewText() + "\"";
        holder.reviewText.setText(formattedReviewText);

        // Set star rating dynamically
        holder.setStarRating(review.getStarRating());
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
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
            // Clear any previously added stars
            starRatingContainer.removeAllViews();

            // Dynamically create star icons
            for (int i = 1; i <= 5; i++) {
                ImageView star = new ImageView(itemView.getContext());
                if (i <= starRating) {
                    star.setImageResource(R.drawable.ic_star_filled_reviews); // Filled star
                } else {
                    star.setImageResource(R.drawable.ic_star_empty_reviews); // Empty star
                }

                // Set size and margin
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 4, 0); // Add spacing between stars
                star.setLayoutParams(params);

                // Add the star to the container
                starRatingContainer.addView(star);
            }
        }
    }
}
