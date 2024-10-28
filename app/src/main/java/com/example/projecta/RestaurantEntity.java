package com.example.projecta;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "restaurants")
public class RestaurantEntity {
    @PrimaryKey
    @NonNull
    public String businessId;
    public String name;
    public String imageUrl;
    public String distance;
    public String priceRange;
    public String address;
    public String time;
    public double latitude;
    public double longitude;
    public String priceTag;
    public boolean isFavorite;
    public String section; // Popular, Nearby, or Dinner
    public long lastUpdated;
}
