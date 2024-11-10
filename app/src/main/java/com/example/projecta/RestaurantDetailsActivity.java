package com.example.projecta;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RestaurantDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private LinearLayout moreInfoDetails, addressDetails, allergenDetails;
    private boolean isExpanded = false;
    private boolean isAddressExpanded = false;
    private boolean isAllergenExpanded = false;
    private ImageView expandArrow, addressExpandArrow, allergenExpandArrow;
    private TextView operatingHoursTextView;
    private TextView addressTextView;
    private TextView phoneTextView;
    private GoogleMap googleMap;
    private LatLng restaurantLatLng;
    private String restaurantName;
    private Map<String, Boolean> favoriteMap = new HashMap<>(); // Added favoriteMap
    private List<String> restaurantCategories = new ArrayList<>();

    private static final Map<String, Integer> ALLERGENS_MAP = new HashMap<String, Integer>() {{
        put("Tree nut", R.drawable.ic_treenut); // Replace with actual drawable icons
        put("Bean", R.drawable.ic_bean);
        put("Gluten", R.drawable.ic_gluten);
        put("Nut", R.drawable.ic_nut);
        put("Shellfish", R.drawable.ic_shellfish);
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_restaurant_details);

        // Setup shared element transitions
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

        // Initialize views
        TextView nameTextView = findViewById(R.id.restaurant_detail_name);
        TextView timeTextView = findViewById(R.id.restaurant_detail_time);
        ImageView restaurantImageView = findViewById(R.id.restaurant_detail_image);
        operatingHoursTextView = findViewById(R.id.operating_hours_text);
        addressTextView = findViewById(R.id.restaurant_address);
        phoneTextView = findViewById(R.id.restaurant_phone);
        LinearLayout categoryContainer = findViewById(R.id.category_container); // Category container
        TextView originalPriceTextView = findViewById(R.id.original_price);
        ImageView favoriteIcon = findViewById(R.id.favorite_icon); // Favorite icon added
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        originalPriceTextView.setPaintFlags(originalPriceTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        nameTextView.setText(restaurantName);
        timeTextView.setText(time);

        // Load image
        Glide.with(this).load(imageUrl).into(restaurantImageView);

        // Handle "RESERVE" button click
        Button reserveButton = findViewById(R.id.restaurant_detail_reserve);
        reserveButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("restaurant_name", restaurantName);
            args.putString("reservation_time", time);
            args.putString("restaurant_address", addressTextView.getText().toString());
            args.putString("phone", phoneTextView.getText().toString());
            args.putString("image_url", imageUrl);
            args.putDouble("latitude", restaurantLatLng.latitude);
            args.putDouble("longitude", restaurantLatLng.longitude);
            args.putString("operating_hours", operatingHoursTextView.getText().toString());

            ReserveBottomSheet bottomSheet = new ReserveBottomSheet();
            bottomSheet.setArguments(args);
            bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
        });

        // Initialize favorite logic
        final boolean[] isFavorite = {favoriteMap.containsKey(businessId) && favoriteMap.get(businessId)};
        checkFavoriteStatus(userId, businessId, favoriteIcon, isFavorite);
        updateFavoriteIcon(favoriteIcon, isFavorite[0]);

        favoriteIcon.setOnClickListener(v -> {
            boolean newFavoriteStatus = !isFavorite[0]; // Toggle favorite status
            favoriteMap.put(businessId, newFavoriteStatus);
            animateFavoriteIcon(favoriteIcon, newFavoriteStatus);
            updateFavoriteStatusInFirestore(businessId, newFavoriteStatus);
            isFavorite[0] = newFavoriteStatus; // Update the wrapper value
        });

        // More Information Section
        moreInfoDetails = findViewById(R.id.more_info_details);
        LinearLayout moreInfoContainer = findViewById(R.id.more_info_container);
        expandArrow = findViewById(R.id.expand_arrow);

        // Address Section
        addressDetails = findViewById(R.id.address_details);
        LinearLayout addressContainer = findViewById(R.id.address_container);
        addressExpandArrow = findViewById(R.id.address_expand_arrow);

        allergenDetails = findViewById(R.id.allergen_details);
        LinearLayout allergenContainer = findViewById(R.id.allergen_container);
        allergenExpandArrow = findViewById(R.id.allergen_info_expand_arrow);

        moreInfoDetails.setVisibility(View.GONE);
        addressDetails.setVisibility(View.GONE);
        allergenDetails.setVisibility(View.GONE);

        moreInfoContainer.setOnClickListener(v -> toggleMoreInfo());
        addressContainer.setOnClickListener(v -> toggleAddressInfo());
        allergenContainer.setOnClickListener(v -> toggleAllergenInfo());

        displayRandomAllergens();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        if (businessId == null || businessId.trim().isEmpty()) {
            Log.e("ERROR", "Business ID is null or empty");
            operatingHoursTextView.setText("Invalid business ID.");
            return;
        } else {
            fetchBusinessDetails(businessId, categoryContainer); // Pass the container to the fetch method
        }
    }

    private void checkFavoriteStatus(String userId, String businessId, ImageView favoriteIcon, boolean[] isFavorite) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(userId)
                .collection("favorites")
                .document(businessId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Restaurant is a favorite
                        updateFavoriteIcon(favoriteIcon, true);
                        isFavorite[0] = true;
                    } else {
                        // Restaurant is not a favorite
                        updateFavoriteIcon(favoriteIcon, false);
                        isFavorite[0] = false;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error checking favorite status", e);
                    updateFavoriteIcon(favoriteIcon, false); // Default to not favorite
                    isFavorite[0] = false;
                });
    }

    // Helper methods for favorite logic
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
        updateFavoriteIcon(favoriteIcon, isFavorite);
    }

    private void updateFavoriteIcon(ImageView favoriteIcon, boolean isFavorite) {
        favoriteIcon.setImageResource(isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline);
    }

    private void updateFavoriteStatusInFirestore(String businessId, boolean isFavorite) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (isFavorite) {
            // Add all required fields to the favorite
            Map<String, Object> restaurantData = new HashMap<>();
            restaurantData.put("businessId", businessId);
            restaurantData.put("name", restaurantName);
            restaurantData.put("latitude", restaurantLatLng.latitude);
            restaurantData.put("longitude", restaurantLatLng.longitude);
            restaurantData.put("address", addressTextView.getText().toString());
            restaurantData.put("categories", restaurantCategories); // Use the extracted categories
            restaurantData.put("imageUrl", getIntent().getStringExtra("imageUrl"));
            restaurantData.put("priceRange", getIntent().getStringExtra("priceRange"));
            restaurantData.put("priceTag", getIntent().getStringExtra("priceTag"));
            restaurantData.put("time", getIntent().getStringExtra("time"));
            restaurantData.put("distance", getIntent().getStringExtra("distance"));
            restaurantData.put("isFavorite", true);

            // Save to Firestore
            db.collection("users")
                    .document(userId)
                    .collection("favorites")
                    .document(businessId)
                    .set(restaurantData)
                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "Added to favorites"))
                    .addOnFailureListener(e -> Log.e("Firestore", "Failed to add to favorites", e));
        } else {
            // Remove the favorite from Firestore
            db.collection("users")
                    .document(userId)
                    .collection("favorites")
                    .document(businessId)
                    .delete()
                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "Removed from favorites"))
                    .addOnFailureListener(e -> Log.e("Firestore", "Failed to remove from favorites", e));
        }
    }

    private void toggleMoreInfo() {
        if (!isExpanded) {
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

    private void toggleAddressInfo() {
        if (!isAddressExpanded) {
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

    private void toggleAllergenInfo() {
        Fade fade = new Fade(Fade.IN | Fade.OUT); // Use both Fade.IN and Fade.OUT
        fade.setDuration(300);
        TransitionManager.beginDelayedTransition((ViewGroup) allergenDetails.getParent(), fade);

        if (!isAllergenExpanded) {
            allergenDetails.setVisibility(View.VISIBLE); // Show allergen details
            allergenExpandArrow.animate().rotation(90).setDuration(300).start(); // Rotate arrow to expanded state
        } else {
            allergenDetails.setVisibility(View.GONE); // Hide allergen details
            allergenExpandArrow.animate().rotation(0).setDuration(300).start(); // Rotate arrow back to collapsed state
        }

        isAllergenExpanded = !isAllergenExpanded; // Toggle the expanded state
    }


    private void displayRandomAllergens() {
        allergenDetails.removeAllViews(); // Clear any existing allergen views

        List<String> allergens = new ArrayList<>(ALLERGENS_MAP.keySet());
        Collections.shuffle(allergens); // Shuffle to randomize

        for (int i = 0; i < Math.min(3, allergens.size()); i++) { // Show up to 3 allergens
            String allergen = allergens.get(i);
            int iconResId = ALLERGENS_MAP.get(allergen);

            // Create an ImageView and TextView for each allergen
            LinearLayout allergenItem = new LinearLayout(this);
            allergenItem.setOrientation(LinearLayout.VERTICAL);
            allergenItem.setPadding(8, 0, 8, 0);
            allergenItem.setGravity(Gravity.CENTER_HORIZONTAL); // Center align

            ImageView allergenIcon = new ImageView(this);
            allergenIcon.setImageResource(iconResId);

            // Set fixed width and height for the allergen icon
            int iconSize = (int) getResources().getDimension(R.dimen.allergen_icon_size); // Define in dimens.xml
            LinearLayout.LayoutParams iconLayoutParams = new LinearLayout.LayoutParams(iconSize, iconSize);
            allergenIcon.setLayoutParams(iconLayoutParams);

            TextView allergenText = new TextView(this);
            allergenText.setText(allergen);
            allergenText.setTypeface(ResourcesCompat.getFont(this, R.font.tw_cen_mt));
            allergenText.setTextSize(12); // Set an appropriate text size
            allergenText.setGravity(Gravity.CENTER);

            allergenItem.addView(allergenIcon);
            allergenItem.addView(allergenText);

            // Add to allergen details layout
            allergenDetails.addView(allergenItem);
        }
    }


    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        if (restaurantLatLng != null) {
            googleMap.addMarker(new MarkerOptions().position(restaurantLatLng).title(restaurantName));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(restaurantLatLng, 15));
        } else {
            Log.e("MapError", "Invalid restaurantLatLng values: " + restaurantLatLng);
        }
    }

    private void fetchBusinessDetails(String businessId, LinearLayout categoryContainer) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.yelp.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        YelpApiService yelpApiService = retrofit.create(YelpApiService.class);
        String apiKey = "Bearer Raj1vqpkZqqenzxYM7SaeNEITjBQHCz7gCSNgqQjVKzXGd9TrNjakyhQRZRDhCZmS5CN87UZQU5v0UXNoyeWOnOfrXE8jy0_17nTPsOllvXD455mAGdzTmyLWCgVZ3Yx"; // Replace with your actual Yelp API key

        Call<YelpBusinessDetailsResponse> call = yelpApiService.getBusinessDetails(apiKey, businessId);

        call.enqueue(new Callback<YelpBusinessDetailsResponse>() {
            @Override
            public void onResponse(Call<YelpBusinessDetailsResponse> call, Response<YelpBusinessDetailsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    YelpBusinessDetailsResponse businessDetails = response.body();

                    // Extract categories and store them
                    restaurantCategories.clear();
                    if (businessDetails.getCategoryNames() != null) {
                        restaurantCategories.addAll(businessDetails.getCategoryNames());
                    }

                    // Display categories in the UI
                    categoryContainer.removeAllViews(); // Clear any existing category views
                    if (!restaurantCategories.isEmpty()) {
                        for (String category : restaurantCategories) {
                            TextView categoryTextView = new TextView(RestaurantDetailsActivity.this);
                            categoryTextView.setText(category);
                            categoryTextView.setPadding(16, 8, 16, 8);
                            categoryTextView.setBackgroundResource(R.drawable.category_background);
                            categoryTextView.setTextColor(getResources().getColor(R.color.black));
                            categoryTextView.setTextSize(14);

                            categoryTextView.setTypeface(ResourcesCompat.getFont(RestaurantDetailsActivity.this, R.font.tw_cen_mt));

                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            layoutParams.setMargins(8, 0, 8, 0); // Set left and right margins between tags
                            categoryTextView.setLayoutParams(layoutParams);

                            // Add each TextView to the container
                            categoryContainer.addView(categoryTextView);
                        }
                    } else {
                        TextView noCategoryText = new TextView(RestaurantDetailsActivity.this);
                        noCategoryText.setTypeface(ResourcesCompat.getFont(RestaurantDetailsActivity.this, R.font.tw_cen_mt));
                        noCategoryText.setText("No categories available");
                        categoryContainer.addView(noCategoryText);
                    }

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
                    phoneTextView.setText(phone != null && !phone.isEmpty() ? phone : "Phone number not available");
                } else {
                    Log.e("API_ERROR", "Response unsuccessful: " + response.code() + " " + response.message());
                }
            }

            @Override
            public void onFailure(Call<YelpBusinessDetailsResponse> call, Throwable t) {
                Log.e("API_FAILURE", "API call failed: " + t.getMessage(), t);
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
            SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

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

        if (fullAddress.length() == 0) {
            addressTextView.setText("Address not available");
        } else {
            addressTextView.setText(fullAddress.toString());
        }
    }
}
