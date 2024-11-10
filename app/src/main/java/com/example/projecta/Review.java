package com.example.projecta;

public class Review {
    private String restaurantName;
    private String reviewText;
    private int starRating;

    // Default constructor for Firestore
    public Review() {}

    public Review(String restaurantName, String reviewText, int starRating) {
        this.restaurantName = restaurantName;
        this.reviewText = reviewText;
        this.starRating = starRating;
    }

    // Getters and Setters
    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public int getStarRating() {
        return starRating;
    }

    public void setStarRating(int starRating) {
        this.starRating = starRating;
    }
}
