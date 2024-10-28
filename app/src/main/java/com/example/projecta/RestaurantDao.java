package com.example.projecta;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface RestaurantDao {
    @Query("SELECT * FROM restaurants WHERE section = :section AND lastUpdated >= :validTime")
    List<RestaurantEntity> getCachedRestaurants(String section, long validTime);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RestaurantEntity> restaurants);

    @Query("DELETE FROM restaurants WHERE section = :section")
    void clearSection(String section);
}
