package com.example.projecta;

public class Business {
    private String name;
    private String image_url;
    private double distance;
    private String price;
    private Location location;
    private double rating;  // Field for the rating
    private String id;  // Field for the business ID
    private Coordinates coordinates;// Add this field for coordinates

    // Getters for all the fields
    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return image_url;
    }

    public double getDistance() {
        return distance;
    }

    public String getPriceRange() {
        return price;
    }

    public Location getLocation() {
        return location;
    }

    public double getRating() {
        return rating;
    }

    public String getId() {
        return id;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    // Static inner Location class
    public static class Location {
        private String address1;

        public String getAddress1() {
            return address1;
        }
    }

    // Static inner Coordinates class
    public static class Coordinates {
        private double latitude;
        private double longitude;

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }
}

