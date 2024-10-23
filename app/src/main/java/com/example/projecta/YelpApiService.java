package com.example.projecta;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface YelpApiService {
    @GET("v3/businesses/search")
    Call<YelpResponse> getRestaurants(
            @Header("Authorization") String apiKey,    // API Key in the header
            @Query("latitude") double latitude,        // Pass latitude
            @Query("longitude") double longitude,      // Pass longitude
            @Query("categories") String categories,    // Restaurant category
            @Query("radius") int radius,               // Radius in meters
            @Query("limit") int limit                  // Limit of results
    );

    @GET("v3/businesses/{id}")
    Call<YelpBusinessDetailsResponse> getBusinessDetails(
            @Header("Authorization") String apiKey,
            @Path("id") String businessId
    );
}
