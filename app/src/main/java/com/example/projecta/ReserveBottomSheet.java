package com.example.projecta;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ReserveBottomSheet extends BottomSheetDialogFragment {

    private int quantity = 1;
    private double totalPrice = 14.99; // Example starting price
    private double pricePerItem = 14.99;
    private boolean allergyFriendly = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_reserve, container, false);

        // Retrieve data from arguments
        Bundle args = getArguments();
        String businessId = args.getString("business_id");
        String restaurantName = args.getString("restaurant_name");
        String reservationTime = args.getString("reservation_time");

        // Initialize views
        TextView tvRestaurantName = view.findViewById(R.id.reservation_restaurant_name);
        TextView tvReservationTime = view.findViewById(R.id.reservation_time);
        TextView tvQuantity = view.findViewById(R.id.tv_quantity);
        TextView tvTotalPrice = view.findViewById(R.id.tv_total_price);
        ImageButton btnDecreaseQuantity = view.findViewById(R.id.btn_decrease_quantity);
        ImageButton btnIncreaseQuantity = view.findViewById(R.id.btn_increase_quantity);
        CheckBox allergyCheckBox = view.findViewById(R.id.allergen_checkbox);
        Button btnReserve = view.findViewById(R.id.btn_reserve);

        // Set up animation for buttons
        addScaleAnimation(btnReserve);
        addScaleAnimation(btnIncreaseQuantity);
        addScaleAnimation(btnDecreaseQuantity);

        // Set initial data
        tvRestaurantName.setText(restaurantName);
        tvReservationTime.setText(reservationTime);
        tvQuantity.setText(String.valueOf(quantity));
        updateTotalPrice(tvTotalPrice);

        // Handle quantity increase
        btnIncreaseQuantity.setOnClickListener(v -> {
            quantity++;
            tvQuantity.setText(String.valueOf(quantity));
            updateTotalPrice(tvTotalPrice);
        });

        // Handle quantity decrease
        btnDecreaseQuantity.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQuantity.setText(String.valueOf(quantity));
                updateTotalPrice(tvTotalPrice);
            }
        });

        // Handle allergy checkbox
        allergyCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> allergyFriendly = isChecked);

        // Handle reservation button click to open OrderConfirmationActivity
        btnReserve.setOnClickListener(v -> {
            // Dismiss the bottom sheet
            dismiss();

            // Open OrderConfirmationActivity with details
            openOrderConfirmationActivity(restaurantName, reservationTime);
        });

        return view;
    }

    // Update the total price based on quantity
    private void updateTotalPrice(TextView tvTotalPrice) {
        totalPrice = pricePerItem * quantity;
        tvTotalPrice.setText(String.format("MYR %.2f", totalPrice)); // Only update the price, without "Total:"
    }

    private void addScaleAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }

    // Function to open OrderConfirmationActivity with the necessary data
    private void openOrderConfirmationActivity(String restaurantName, String reservationTime) {
        Intent intent = new Intent(getActivity(), OrderConfirmationActivity.class);

        // Pass the necessary data using intent extras
        Bundle args = getArguments();
        if (args != null) {
            intent.putExtra("business_id", args.getString("business_id"));
            intent.putExtra("restaurant_name", args.getString("restaurant_name"));
            intent.putExtra("reservation_time", args.getString("reservation_time"));
            intent.putExtra("restaurant_address", args.getString("restaurant_address"));
            intent.putExtra("phone", args.getString("phone"));
            intent.putExtra("image_url", args.getString("image_url"));
            intent.putExtra("latitude", args.getDouble("latitude"));
            intent.putExtra("longitude", args.getDouble("longitude"));
            intent.putExtra("operating_hours", args.getString("operating_hours"));
            intent.putExtra("quantity", quantity);
            intent.putExtra("allergy_friendly", allergyFriendly);
            intent.putExtra("total_price", totalPrice);
        }
        // Start OrderConfirmationActivity
        startActivity(intent);
    }
}
