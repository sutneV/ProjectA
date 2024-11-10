package com.example.projecta;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.transition.Fade;
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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class OrderConfirmationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private LatLng restaurantLatLng;
    private String restaurantName, reservationTime, restaurantAddress;
    private boolean allergyFriendly;
    private int quantity;
    private double subtotal;
    private double totalPrice;
    private LinearLayout addressDetails, orderDetailsSection;
    private ImageView addressExpandArrow;
    private boolean isAddressExpanded = false;
    private boolean isOrderDetailsExpanded = false;

    // Firebase Firestore and Authentication instances
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseUser currentUser;

    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(Color.TRANSPARENT); // Make the status bar transparent
        setContentView(R.layout.activity_order_confirmation);

        // Retrieve current user
        currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // If the user is not logged in, close this activity and redirect to login screen
            Toast.makeText(this, "Please log in to view your orders.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Retrieve passed arguments
        Intent intent = getIntent();
        restaurantName = intent.getStringExtra("restaurant_name");
        reservationTime = intent.getStringExtra("reservation_time");
        restaurantAddress = intent.getStringExtra("restaurant_address");
        quantity = intent.getIntExtra("quantity", 1);
        allergyFriendly = intent.getBooleanExtra("allergy_friendly", false);
        totalPrice = intent.getDoubleExtra("total_price", 0.0); // Total price passed from ReserveBottomSheet
        double pricePerItem = totalPrice / quantity; // Derive price per item from total and quantity
        subtotal = pricePerItem * quantity; // Calculate subtotal

        double latitude = intent.getDoubleExtra("latitude", 0);
        double longitude = intent.getDoubleExtra("longitude", 0);
        restaurantLatLng = new LatLng(latitude, longitude);

        // Initialize views
        TextView restaurantNameTextView = findViewById(R.id.restaurant_name);
        TextView reservationTimeTextView = findViewById(R.id.pickup_time_text);
        TextView addressTextView = findViewById(R.id.restaurant_address);
        TextView orderIdTextView = findViewById(R.id.order_id_text);
        TextView itemQuantityTextView = findViewById(R.id.item_quantity);
        TextView itemNameTextView = findViewById(R.id.item_name);
        TextView allergenInfoTextView = findViewById(R.id.allergen_info);
        TextView itemPriceTextView = findViewById(R.id.item_price);
        TextView subtotalTextView = findViewById(R.id.subtotal_price);
        TextView cityTaxTextView = findViewById(R.id.city_tax_price);
        TextView stateTaxTextView = findViewById(R.id.state_tax_price);
        TextView totalPriceTextView = findViewById(R.id.total_price);
        orderDetailsSection = findViewById(R.id.order_details_section);
        addressDetails = findViewById(R.id.address_details);
        addressExpandArrow = findViewById(R.id.address_expand_arrow);

        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v -> finish());

        setupCancelReservationButton();

        // Calculate taxes based on subtotal
        double cityTax = totalPrice * 0.04; // 4% city tax
        double stateTax = totalPrice * 0.03; // 3% state tax
        subtotal = totalPrice - cityTax - stateTax;

        // Set calculated prices
        subtotalTextView.setText(String.format("MYR %.2f", subtotal));
        cityTaxTextView.setText(String.format("MYR %.2f", cityTax));
        stateTaxTextView.setText(String.format("MYR %.2f", stateTax));
        totalPriceTextView.setText(String.format("Total: MYR %.2f", totalPrice));

        // Check if this is a new or existing order
        orderId = getIntent().getStringExtra("order_id");
        restaurantName = intent.getStringExtra("restaurant_name");

        if (orderId == null) {
            generateUniqueOrderID(new OnOrderIDGeneratedListener() {
                @Override
                public void onOrderIDGenerated(String generatedOrderID) {
                    orderId = generatedOrderID;
                    saveOrderToFirebase(); // Save the order with the unique ID
                    // Update UI if necessary
                    TextView orderIdTextView = findViewById(R.id.order_id_text);
                    orderIdTextView.setText(orderId);
                }
            });
        } else {
            loadOrderFromFirebase(orderId);
        }

        // Set basic data
        restaurantNameTextView.setText(restaurantName);
        reservationTimeTextView.setText("Pick-up: " + reservationTime);
        addressTextView.setText(restaurantAddress);
        orderIdTextView.setText(orderId);
        itemQuantityTextView.setText(String.valueOf(quantity));
        itemNameTextView.setText("The Portkey Package");
        allergenInfoTextView.setText(allergyFriendly ? "Allergen-friendly" : "May contain allergens");
        itemPriceTextView.setText(String.format("MYR %.2f", totalPrice));


        // Set click listener for expanding address section
        findViewById(R.id.address_container).setOnClickListener(v -> toggleAddressDetails());

        // Set up the Google Map for displaying the location
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this); // Attach the OnMapReadyCallback
        } else {
            Log.e("MapDebug", "SupportMapFragment is null. Check if R.id.map exists in the layout.");
        }


// Set up the "Pick-Up Completed" button
        Button pickupCompletedButton = findViewById(R.id.pickup_completed_button);
        pickupCompletedButton.setOnClickListener(v -> {
            if (currentUser != null && orderId != null) {
                String userId = currentUser.getUid();
                Log.d("OrderConfirmationActivity", "Attempting to update order with ID: " + orderId + " for user: " + userId);

                db.collection("users")
                        .document(userId)
                        .collection("orders")
                        .document(orderId)
                        .update("status", "Completed")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(OrderConfirmationActivity.this, "Pick-up Completed!", Toast.LENGTH_SHORT).show();

                            // Navigate to the review screen
                            ReviewBottomSheet reviewBottomSheet = new ReviewBottomSheet();
                            Bundle args = new Bundle();
                            args.putString("order_id", orderId); // Pass order ID
                            args.putString("restaurant_name", restaurantName != null ? restaurantName : "Unknown"); // Pass restaurant name
                            reviewBottomSheet.setArguments(args);
                            reviewBottomSheet.show(getSupportFragmentManager(), "ReviewBottomSheet");

                        })
                        .addOnFailureListener(e -> {
                            Log.e("OrderConfirmationActivity", "Failed to update status", e);
                            Toast.makeText(OrderConfirmationActivity.this, "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Log.e("OrderConfirmationActivity", "Error: Order ID or user not available - orderId: " + orderId + ", currentUser: " + currentUser);
                Toast.makeText(this, "Error: Order ID or user not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Make order ID clickable to show/hide order details
        orderIdTextView.setOnClickListener(v -> toggleOrderDetails());
    }

    // Save order data to Firebase Firestore
    private void saveOrderToFirebase() {
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        double cityTax = totalPrice * 0.04; // 4% city tax
        double stateTax = totalPrice * 0.03; // 3% state tax
        subtotal = totalPrice - cityTax - stateTax;

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("order_id", orderId);  // Use the single generated orderId
        orderData.put("restaurant_name", restaurantName);
        orderData.put("reservation_time", reservationTime);
        orderData.put("restaurant_address", restaurantAddress);
        orderData.put("quantity", quantity);
        orderData.put("allergy_friendly", allergyFriendly);
        orderData.put("subtotal", totalPrice - cityTax - stateTax);
        orderData.put("city_tax", totalPrice * 0.04); // Calculate city tax
        orderData.put("state_tax", totalPrice * 0.03); // Calculate state tax
        orderData.put("total_price", totalPrice);
        orderData.put("latitude", restaurantLatLng.latitude);
        orderData.put("longitude", restaurantLatLng.longitude);
        orderData.put("status", "Pending Pickup");

        db.collection("users")
                .document(userId)
                .collection("orders")
                .document(orderId)  // Use the single generated orderId
                .set(orderData)
                .addOnSuccessListener(aVoid -> Toast.makeText(OrderConfirmationActivity.this, "Order saved successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(OrderConfirmationActivity.this, "Failed to save order: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    // Load order data from Firebase Firestore for specific user
    private void loadOrderFromFirebase(String orderId) {
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        db.collection("users")
                .document(userId)
                .collection("orders")
                .document(orderId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();

                        // Call the method to display data
                        displayOrderData(document);
                    } else {
                        Log.e("FirebaseDebug", "Order not found for current user or does not exist.");
                        Toast.makeText(OrderConfirmationActivity.this, "Order not found for current user", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseDebug", "Error loading order: " + e.getMessage(), e);
                    Toast.makeText(OrderConfirmationActivity.this, "Error loading order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    // Display order data on the screen
    private void displayOrderData(DocumentSnapshot document) {
        // Fetch UI components
        TextView restaurantNameTextView = findViewById(R.id.restaurant_name);
        TextView reservationTimeTextView = findViewById(R.id.pickup_time_text);
        TextView addressTextView = findViewById(R.id.restaurant_address);
        TextView itemQuantityTextView = findViewById(R.id.item_quantity);
        TextView allergenInfoTextView = findViewById(R.id.allergen_info);
        TextView itemPriceTextView = findViewById(R.id.item_price);
        TextView subtotalTextView = findViewById(R.id.subtotal_price);
        TextView cityTaxTextView = findViewById(R.id.city_tax_price);
        TextView stateTaxTextView = findViewById(R.id.state_tax_price);
        TextView totalPriceTextView = findViewById(R.id.total_price);
        TextView statusTextView = findViewById(R.id.order_status); // Assuming you have a TextView for the status
        Button pickupCompletedButton = findViewById(R.id.pickup_completed_button);


        // Hide button by default
        pickupCompletedButton.setVisibility(View.GONE);

        // Update TextViews with Firebase data
        restaurantName = document.getString("restaurant_name");
        restaurantNameTextView.setText(document.getString("restaurant_name"));
        reservationTimeTextView.setText("Pick-up: " + document.getString("reservation_time"));
        addressTextView.setText(document.getString("restaurant_address"));
        itemQuantityTextView.setText(String.valueOf(document.getLong("quantity")));

        // Format prices and taxes properly
        double subtotal = document.getDouble("subtotal") != null ? document.getDouble("subtotal") : 0.0;
        double cityTax = document.getDouble("city_tax") != null ? document.getDouble("city_tax") : 0.0;
        double stateTax = document.getDouble("state_tax") != null ? document.getDouble("state_tax") : 0.0;
        double totalPrice = document.getDouble("total_price") != null ? document.getDouble("total_price") : 0.0;

        double latitude = document.getDouble("latitude") != null ? document.getDouble("latitude") : 0.0;
        double longitude = document.getDouble("longitude") != null ? document.getDouble("longitude") : 0.0;
        restaurantLatLng = new LatLng(latitude, longitude);

        // Update prices
        subtotalTextView.setText(String.format("MYR %.2f", subtotal));
        cityTaxTextView.setText(String.format("MYR %.2f", cityTax));
        stateTaxTextView.setText(String.format("MYR %.2f", stateTax));
        totalPriceTextView.setText(String.format("Total: MYR %.2f", totalPrice));

        // Update allergen info
        boolean allergyFriendly = document.getBoolean("allergy_friendly") != null && document.getBoolean("allergy_friendly");
        allergenInfoTextView.setText(allergyFriendly ? "Allergen-friendly" : "May contain allergens");

        // Update item price
        itemPriceTextView.setText(String.format("MYR %.2f", totalPrice));

        // Update status
        String status = document.getString("status");
        if (status != null) {
            statusTextView.setText(status);

            // Show "Pick-Up Completed" button only if status is not "Completed"
            if (!status.equalsIgnoreCase("Completed")) {
                pickupCompletedButton.setVisibility(View.VISIBLE);
            }
        } else {
            statusTextView.setText("Status unknown");
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e("MapDebug", "SupportMapFragment is null.");
        }
    }

    private void setupCancelReservationButton() {
        TextView cancelReservationButton = findViewById(R.id.cancel_reservation);

        cancelReservationButton.setOnClickListener(v -> {
            if (currentUser != null && orderId != null) {
                String userId = currentUser.getUid();

                // Show confirmation dialog
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Cancel Reservation")
                        .setMessage("Are you sure you want to cancel this reservation?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            // Delete the order from Firestore
                            db.collection("users")
                                    .document(userId)
                                    .collection("orders")
                                    .document(orderId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(OrderConfirmationActivity.this, "Reservation canceled successfully.", Toast.LENGTH_SHORT).show();
                                        finish(); // Close the activity
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("CancelReservation", "Failed to cancel reservation", e);
                                        Toast.makeText(OrderConfirmationActivity.this, "Failed to cancel reservation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .show();
            } else {
                Log.e("CancelReservation", "Error: User not logged in or order ID is null.");
                Toast.makeText(this, "Error: Unable to cancel reservation. Please try again later.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Generate a formatted Order ID
    private void generateUniqueOrderID(OnOrderIDGeneratedListener listener) {
        String datePart = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        int randomPart = new Random().nextInt(9000) + 1000; // Random 4-digit number
        String newOrderID = "ORD-" + datePart + "-" + randomPart;

        // Check Firebase to ensure the ID is unique
        db.collection("users")
                .document(currentUser.getUid())
                .collection("orders")
                .document(newOrderID)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        // If the generated ID already exists, retry
                        generateUniqueOrderID(listener);
                    } else {
                        // If the ID is unique, invoke the listener
                        listener.onOrderIDGenerated(newOrderID);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("OrderIDGeneration", "Failed to check order ID uniqueness", e);
                    Toast.makeText(this, "Failed to generate order ID. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    // Callback interface to handle asynchronous ID generation
    interface OnOrderIDGeneratedListener {
        void onOrderIDGenerated(String orderID);
    }

    // Toggle address details visibility
    private void toggleAddressDetails() {
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

    // Toggle order details visibility
    private void toggleOrderDetails() {
        if (!isOrderDetailsExpanded) {
            Fade fadeIn = new Fade(Fade.IN);
            fadeIn.setDuration(300);
            TransitionManager.beginDelayedTransition((ViewGroup) orderDetailsSection.getParent(), fadeIn);
            orderDetailsSection.setVisibility(View.VISIBLE);
        } else {
            orderDetailsSection.setVisibility(View.GONE);
        }
        isOrderDetailsExpanded = !isOrderDetailsExpanded;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        if (restaurantLatLng != null) {
            googleMap.addMarker(new MarkerOptions()
                    .position(restaurantLatLng)
                    .title(restaurantName));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(restaurantLatLng, 15)); // Zoom level 15 for better focus
        } else {
            Log.e("MapDebug", "restaurantLatLng is null");
            Toast.makeText(this, "Unable to load map location.", Toast.LENGTH_SHORT).show();
        }
    }
}
