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

        if (orderId == null) {
            // Generate a new order ID if it's a new order
            orderId = generateOrderID();
            saveOrderToFirebase(); // Save only if it’s a new order
        } else {
            // Load existing order details if orderId is available
            loadOrderFromFirebase(orderId);
        }

        // Set basic data
        restaurantNameTextView.setText(restaurantName);
        reservationTimeTextView.setText("Pick-up: " + reservationTime);
        addressTextView.setText(restaurantAddress);
        orderIdTextView.setText(orderId);
        itemQuantityTextView.setText(String.valueOf(quantity));
        itemNameTextView.setText("Magic Bag");
        allergenInfoTextView.setText(allergyFriendly ? "Allergen-friendly" : "May contain allergens");
        itemPriceTextView.setText(String.format("MYR %.2f", totalPrice));


        // Set click listener for expanding address section
        findViewById(R.id.address_container).setOnClickListener(v -> toggleAddressDetails());

        // Set up the Google Map for displaying the location
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
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
                            finish();
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
// Save order data to Firebase Firestore
    private void saveOrderToFirebase() {
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("order_id", orderId);  // Use the single generated orderId
        orderData.put("restaurant_name", restaurantName);
        orderData.put("reservation_time", reservationTime);
        orderData.put("restaurant_address", restaurantAddress);
        orderData.put("quantity", quantity);
        orderData.put("allergy_friendly", allergyFriendly);
        orderData.put("subtotal", subtotal);
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
                        displayOrderData(document);
                    } else {
                        Toast.makeText(OrderConfirmationActivity.this, "Order not found for current user", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(OrderConfirmationActivity.this, "Error loading order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Display order data on the screen
    private void displayOrderData(DocumentSnapshot document) {
        TextView restaurantNameTextView = findViewById(R.id.restaurant_name);
        TextView reservationTimeTextView = findViewById(R.id.pickup_time_text);
        TextView addressTextView = findViewById(R.id.restaurant_address);
        TextView itemQuantityTextView = findViewById(R.id.item_quantity);
        TextView allergenInfoTextView = findViewById(R.id.allergen_info);
        TextView itemPriceTextView = findViewById(R.id.item_price);

        restaurantNameTextView.setText(document.getString("restaurant_name"));
        reservationTimeTextView.setText("Pick-up: " + document.getString("reservation_time"));
        addressTextView.setText(document.getString("restaurant_address"));
        itemQuantityTextView.setText(String.valueOf(document.getLong("quantity")));
        allergenInfoTextView.setText(document.getBoolean("allergy_friendly") ? "Allergen-friendly" : "May contain allergens");
        itemPriceTextView.setText(String.format("MYR %.2f", document.getDouble("total_price")));
    }

    // Generate a formatted Order ID
    private String generateOrderID() {
        String datePart = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        int randomPart = new Random().nextInt(9000) + 1000; // Random 4-digit number
        return "ORD-" + datePart + "-" + randomPart;
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
            googleMap.addMarker(new MarkerOptions().position(restaurantLatLng).title(restaurantName));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(restaurantLatLng, 15));
        }
    }
}
