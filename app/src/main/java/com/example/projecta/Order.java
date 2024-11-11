package com.example.projecta;

import com.google.firebase.firestore.PropertyName;

import kotlin.text.UStringsKt;

public class Order {
    @PropertyName("business_id")
    private String businessId;
    @PropertyName("order_id")
    private String orderId;
    @PropertyName("restaurant_name")
    private String restaurantName;
    @PropertyName("reservation_time")
    private String reservationTime;
    @PropertyName("status")
    private String status;

    public Order() {
        // Default constructor required for calls to DataSnapshot.getValue(Order.class)
    }

    public Order(String businessId, String orderId, String restaurantName, String reservationTime, String status) {
        this.businessId = businessId;
        this.orderId = orderId;
        this.restaurantName = restaurantName;
        this.reservationTime = reservationTime;
        this.status = status;
    }

    @PropertyName("business_id")
    public String getBusinessId() {
        return businessId;
    }

    @PropertyName("order_id")
    public String getOrderId() {
        return orderId;
    }

    @PropertyName("order_id")
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    @PropertyName("restaurant_name")
    public String getRestaurantName() {
        return restaurantName;
    }

    @PropertyName("restaurant_name")
    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    @PropertyName("reservation_time")
    public String getReservationTime() {
        return reservationTime;
    }

    @PropertyName("reservation_time")
    public void setReservationTime(String reservationTime) {
        this.reservationTime = reservationTime;
    }

    @PropertyName("status")
    public String getStatus() {
        return status;
    }

    @PropertyName("status")
    public void setStatus(String status) {
        this.status = status;
    }
}
