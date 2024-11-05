package com.example.projecta;

import java.util.List;
import java.util.stream.Collectors;

public class Business {
    private String name;
    private String image_url;
    private double distance;
    private String price;
    private Location location;
    private double rating;  // Rating of the business
    private String id;  // Unique ID of the business
    private Coordinates coordinates;  // Location coordinates
    private List<Category> categories;  // List of categories for this business

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

    public List<Category> getCategories() {
        return categories;
    }

    // Helper method to retrieve category names as a List<String>
    public List<String> getCategoryNames() {
        if (categories == null) return null;
        return categories.stream().map(Category::getTitle).collect(Collectors.toList());
    }

    // Static inner class for Location details
    public static class Location {
        private String address1;

        public String getAddress1() {
            return address1;
        }
    }

    // Static inner class for Coordinates details
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

    // Static inner class for Category details
    public static class Category {
        private String alias;  // Internal identifier for the category
        private String title;  // Display name of the category

        public String getAlias() {
            return alias;
        }

        public String getTitle() {
            return title;
        }
    }
}
