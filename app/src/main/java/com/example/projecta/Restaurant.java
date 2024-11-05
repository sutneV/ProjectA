package com.example.projecta;

import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Restaurant {
    private String name;
    private String imageUrl;
    private String distance;
    private String price;
    private String address;
    private String time;
    private String businessId;
    private double latitude;
    private double longitude;
    private String priceTag;
    private boolean isFavorite;
    private List<String> categories;

    // Constructor with arguments
    public Restaurant(String name, String imageUrl, String distance, String price, String address, String time,
                      String businessId, double latitude, double longitude, String priceTag, boolean isFavorite, List<String> categories) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.distance = distance;
        this.price = price;
        this.address = address;
        this.time = time;
        this.businessId = businessId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.priceTag = priceTag;
        this.isFavorite = isFavorite;
        this.categories = categories;
    }

    // No-argument constructor for Firebase deserialization
    public Restaurant() {
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getDistance() {
        return distance;
    }

    public String getPriceRange() {
        return price;
    }

    public String getAddress() {
        return address;
    }

    public String getTime() {
        return time;
    }

    public String getBusinessId() {
        return businessId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getPriceTag() {
        return priceTag;
    }

    public void setPriceTag(String priceTag) {
        this.priceTag = priceTag;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    // Getters and Setters for categories
    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    // Method to convert Restaurant object to a Map<String, Object> for Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", getName());
        map.put("imageUrl", getImageUrl());
        map.put("distance", getDistance());
        map.put("priceRange", getPriceRange());
        map.put("address", getAddress());
        map.put("time", getTime());
        map.put("businessId", getBusinessId());
        map.put("latitude", getLatitude());
        map.put("longitude", getLongitude());
        map.put("priceTag", getPriceTag());
        map.put("isFavorite", isFavorite());
        map.put("categories", getCategories());  // Adding categories to the map
        return map;
    }

    // Static method to create a Restaurant object from Firestore DocumentSnapshot
    public static Restaurant fromDocument(DocumentSnapshot document) {
        Restaurant restaurant = new Restaurant();
        restaurant.name = document.getString("name");
        restaurant.imageUrl = document.getString("imageUrl");
        restaurant.distance = document.getString("distance");
        restaurant.price = document.getString("priceRange");
        restaurant.address = document.getString("address");
        restaurant.time = document.getString("time");
        restaurant.businessId = document.getString("businessId");
        restaurant.latitude = document.getDouble("latitude");
        restaurant.longitude = document.getDouble("longitude");
        restaurant.priceTag = document.getString("priceTag");
        restaurant.isFavorite = document.getBoolean("isFavorite") != null && document.getBoolean("isFavorite");
        restaurant.categories = (List<String>) document.get("categories");  // Retrieve categories as a List<String>
        return restaurant;
    }
}
