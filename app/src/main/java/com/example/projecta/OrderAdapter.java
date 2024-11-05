package com.example.projecta;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orderList;
    private Context context;

    public OrderAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.orders_card, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.orderIdText.setText(order.getOrderId());
        holder.restaurantNameText.setText(order.getRestaurantName());
        holder.reservationTimeText.setText(order.getReservationTime());
        holder.statusText.setText(order.getStatus());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderConfirmationActivity.class);
            intent.putExtra("order_id", order.getOrderId());
            context.startActivity(intent);
        });
    }


    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView orderIdText, restaurantNameText, reservationTimeText, statusText, totalPriceText;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            orderIdText = itemView.findViewById(R.id.order_id_text);
            restaurantNameText = itemView.findViewById(R.id.restaurant_name_text);
            reservationTimeText = itemView.findViewById(R.id.reservation_time_text);
            statusText = itemView.findViewById(R.id.status_text);
        }
    }
}
