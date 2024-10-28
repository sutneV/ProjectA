package com.example.projecta;

import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RestaurantDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private LinearLayout moreInfoDetails, addressDetails;
    private boolean isExpanded = false;
    private boolean isAddressExpanded = false;
    private ImageView expandArrow, addressExpandArrow;
    private TextView operatingHoursTextView;
    private TextView addressTextView;
    private TextView phoneTextView;
    private GoogleMap googleMap;
    private LatLng restaurantLatLng;  // For storing restaurant's lat/long
    private String restaurantName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_details);

        // Inflate the custom transition for rounded corners (assuming it's in res/transition folder)
        Transition sharedTransition = TransitionInflater.from(this).inflateTransition(R.transition.rounded_transition);

        // Set shared element transitions
        getWindow().setSharedElementEnterTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.move));
        getWindow().setSharedElementExitTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.move));

        // Get the passed data from the intent
        restaurantName = getIntent().getStringExtra("name");
        String businessId = getIntent().getStringExtra("businessId");
        String time = getIntent().getStringExtra("time");
        String imageUrl = getIntent().getStringExtra("imageUrl");
        double latitude = getIntent().getDoubleExtra("latitude", 0);
        double longitude = getIntent().getDoubleExtra("longitude", 0);

        restaurantLatLng = new LatLng(latitude, longitude);

        // Log the received businessId for debugging
        Log.d("Business ID", "Received Business ID: " + businessId);

        // Find views and set the data
        TextView nameTextView = findViewById(R.id.restaurant_detail_name);
        TextView timeTextView = findViewById(R.id.restaurant_detail_time);
        ImageView restaurantImageView = findViewById(R.id.restaurant_detail_image);
        operatingHoursTextView = findViewById(R.id.operating_hours_text);  // TextView for hours
        addressTextView = findViewById(R.id.restaurant_address);           // TextView for address
        phoneTextView = findViewById(R.id.restaurant_phone);        // TextView for phone

        nameTextView.setText(restaurantName);
        timeTextView.setText(time);

        // Load the image into the ImageView
        Glide.with(this).load(imageUrl).into(restaurantImageView);

        // Handle "RESERVE" button click
        Button reserveButton = findViewById(R.id.restaurant_detail_reserve);
        reserveButton.setOnClickListener(v -> {
            // Create a Bundle to pass data to the bottom sheet
            Bundle args = new Bundle();
            args.putString("restaurant_name", restaurantName);   // Pass restaurant name
            args.putString("reservation_time", time);  // Pass reservation time

            // Create and show the ReserveBottomSheet with the arguments
            ReserveBottomSheet bottomSheet = new ReserveBottomSheet();
            bottomSheet.setArguments(args);
            bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
        });


        // Handle the expandable section (More Information)
        moreInfoDetails = findViewById(R.id.more_info_details);
        LinearLayout moreInfoContainer = findViewById(R.id.more_info_container);
        expandArrow = findViewById(R.id.expand_arrow);

        addressDetails = findViewById(R.id.address_details);
        LinearLayout addressContainer = findViewById(R.id.address_container);
        addressExpandArrow = findViewById(R.id.address_expand_arrow);

        // Initially hide the "More Information" details
        moreInfoDetails.setVisibility(View.GONE);
        addressDetails.setVisibility(View.GONE);

        moreInfoContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMoreInfo();
            }
        });

        addressContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAddressInfo();
            }
        });
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        if (businessId == null || businessId.trim().isEmpty()) {
            Log.e("ERROR", "Business ID is null or empty");
            operatingHoursTextView.setText("Invalid business ID.");
            return;
        } else {
            // Fetch business details from Yelp API
            fetchBusinessDetails(businessId);
        }
    }

    // Toggle the visibility of the "More Information" section
    private void toggleMoreInfo() {
        if (!isExpanded) {  // Only apply animation on expansion
            Fade fadeIn = new Fade(Fade.IN);
            fadeIn.setDuration(300);
            TransitionManager.beginDelayedTransition((ViewGroup) moreInfoDetails.getParent(), fadeIn);
            moreInfoDetails.setVisibility(View.VISIBLE);
            expandArrow.animate().rotation(90).setDuration(300).start();
        } else {
            moreInfoDetails.setVisibility(View.GONE);
            expandArrow.animate().rotation(0).setDuration(300).start();
        }
        isExpanded = !isExpanded;
    }

    // Toggle the visibility of the "Address" section
    private void toggleAddressInfo() {
        if (!isAddressExpanded) {  // Only apply animation on expansion
            Fade fadeIn = new Fade(Fade.IN);
            fadeIn.setDuration(300);
            TransitionManager.beginDelayedTransition((ViewGroup) addressDetails.getParent(), fadeIn);
            addressDetails.setVisibility(View.VISIBLE);
            addressExpandArrow.animate().rotation(90).setDuration(300).start();
        } else {
            addressDetails.setVisibility(View.GONE);
            addressExpandArrow.animate().rotation(0).setDuration(300).start();
        }
        isAddressExpanded = !isAddressExpanded;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        if (restaurantLatLng != null && restaurantLatLng.latitude != 0 && restaurantLatLng.longitude != 0) {
            googleMap.addMarker(new MarkerOptions()
                    .position(restaurantLatLng)
                    .title(restaurantName));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(restaurantLatLng, 15));
        } else {
            Log.e("MapError", "Invalid restaurantLatLng values: " + restaurantLatLng);
        }
    }


    // Fetch business details from Yelp API
    private void fetchBusinessDetails(String businessId) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.yelp.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        YelpApiService yelpApiService = retrofit.create(YelpApiService.class);

        String apiKey = "Bearer Raj1vqpkZqqenzxYM7SaeNEITjBQHCz7gCSNgqQjVKzXGd9TrNjakyhQRZRDhCZmS5CN87UZQU5v0UXNoyeWOnOfrXE8jy0_17nTPsOllvXD455mAGdzTmyLWCgVZ3Yx";  // Replace with your actual Yelp API key
        Call<YelpBusinessDetailsResponse> call = yelpApiService.getBusinessDetails(apiKey, businessId);

        call.enqueue(new Callback<YelpBusinessDetailsResponse>() {
            @Override
            public void onResponse(Call<YelpBusinessDetailsResponse> call, Response<YelpBusinessDetailsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    YelpBusinessDetailsResponse businessDetails = response.body();

                    // Display operating hours
                    if (businessDetails.getHours() != null) {
                        displayOperatingHours(businessDetails.getHours());
                    } else {
                        operatingHoursTextView.setText("Operating hours not available");
                    }

                    // Display address
                    if (businessDetails.getLocation() != null) {
                        displayAddress(businessDetails.getLocation());
                    } else {
                        addressTextView.setText("Address not available");
                    }

                    // Display phone
                    String phone = businessDetails.getDisplayPhone();
                    if (phone != null && !phone.isEmpty()) {
                        phoneTextView.setText(phone);
                    } else {
                        phoneTextView.setText("Phone number not available");
                    }

                } else {
                    operatingHoursTextView.setText("Operating hours unavailable");
                    Log.e("API_ERROR", "Response unsuccessful. Code: " + response.code() + " Message: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<YelpBusinessDetailsResponse> call, Throwable t) {
                Log.e("API_FAILURE", "API call failed: " + t.getMessage(), t);
                operatingHoursTextView.setText("Operating hours unavailable");
                Toast.makeText(getApplicationContext(), "Failed to fetch details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Display the operating hours
    private void displayOperatingHours(List<YelpBusinessDetailsResponse.Hour> hours) {
        if (hours != null && !hours.isEmpty()) {
            StringBuilder hoursString = new StringBuilder();
            String[] daysOfWeek = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

            Map<Integer, List<String>> hoursByDay = new HashMap<>();
            for (int i = 0; i < 7; i++) {
                hoursByDay.put(i, new ArrayList<>());
            }

            SimpleDateFormat inputFormat = new SimpleDateFormat("HHmm");
            SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a");

            for (YelpBusinessDetailsResponse.Hour hour : hours) {
                for (YelpBusinessDetailsResponse.Hour.Open open : hour.getOpen()) {
                    int day = open.getDay();
                    try {
                        Date startTime = inputFormat.parse(open.getStart());
                        Date endTime = inputFormat.parse(open.getEnd());
                        String openCloseTime = outputFormat.format(startTime) + " - " + outputFormat.format(endTime);
                        hoursByDay.get(day).add(openCloseTime);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }

            for (int i = 0; i < 7; i++) {
                List<String> times = hoursByDay.get(i);
                if (times.isEmpty()) {
                    hoursString.append(daysOfWeek[i]).append(": Closed\n");
                } else {
                    hoursString.append(daysOfWeek[i]).append(": ").append(String.join(", ", times)).append("\n");
                }
            }

            operatingHoursTextView.setText(hoursString.toString());
        } else {
            operatingHoursTextView.setText("Operating hours not available");
        }
    }

    // Display address
    private void displayAddress(YelpBusinessDetailsResponse.Location location) {
        StringBuilder fullAddress = new StringBuilder();

        if (location.getAddress1() != null && !location.getAddress1().isEmpty()) {
            fullAddress.append(location.getAddress1());
        }

        if (location.getAddress2() != null && !location.getAddress2().isEmpty()) {
            fullAddress.append(", ").append(location.getAddress2());
        }

        if (location.getCity() != null && !location.getCity().isEmpty()) {
            fullAddress.append(", ").append(location.getCity());
        }

        if (location.getState() != null && !location.getState().isEmpty()) {
            fullAddress.append(", ").append(location.getState());
        }

        if (location.getZipCode() != null && !location.getZipCode().isEmpty()) {
            fullAddress.append(" ").append(location.getZipCode());
        }

        if (location.getCountry() != null && !location.getCountry().isEmpty()) {
            fullAddress.append(", ").append(location.getCountry());
        }

        // If the full address is still empty, display a fallback message
        if (fullAddress.length() == 0) {
            addressTextView.setText("Address not available");
        } else {
            addressTextView.setText(fullAddress.toString());
        }
    }
}
