package com.example.projecta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ReserveBottomSheet extends BottomSheetDialogFragment {

    private int quantity = 1;
    private double totalPrice = 4.00; // Example starting price
    private double pricePerItem = 4.00;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_reserve, container, false);

        Bundle args = getArguments();
        String restaurantName = args.getString("restaurant_name");
        String reservationTime = args.getString("reservation_time");

        TextView tvRestaurantName = view.findViewById(R.id.reservation_restaurant_name);
        TextView tvReservationTime = view.findViewById(R.id.reservation_time);
        TextView tvQuantity = view.findViewById(R.id.tv_quantity);
        TextView tvTotalPrice = view.findViewById(R.id.tv_total_price);
        ImageButton btnDecreaseQuantity = view.findViewById(R.id.btn_decrease_quantity);
        ImageButton btnIncreaseQuantity = view.findViewById(R.id.btn_increase_quantity);
        Button btnReserve = view.findViewById(R.id.btn_reserve);

        addScaleAnimation(btnReserve);
        addScaleAnimation(btnIncreaseQuantity);
        addScaleAnimation(btnDecreaseQuantity);

        tvRestaurantName.setText(restaurantName);
        tvReservationTime.setText(reservationTime);

        // Handle increase quantity
        btnIncreaseQuantity.setOnClickListener(v -> {
            quantity++;
            tvQuantity.setText(String.valueOf(quantity));
            updateTotalPrice(tvTotalPrice);
        });

        // Handle decrease quantity
        btnDecreaseQuantity.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQuantity.setText(String.valueOf(quantity));
                updateTotalPrice(tvTotalPrice);
            }
        });

        // Handle reservation
        btnReserve.setOnClickListener(v -> {
            // Add your reservation logic here
            dismiss();
            Toast.makeText(getContext(), "Reserved " + quantity + " items", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    // Update the total price based on quantity
    private void updateTotalPrice(TextView tvTotalPrice) {
        totalPrice = pricePerItem * quantity;
        tvTotalPrice.setText(String.format("US$ %.2f", totalPrice)); // Only update the price, without "Total:"
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
}
