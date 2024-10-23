package com.example.projecta;

import java.util.List;

public class YelpBusinessDetailsResponse {

    private String phone;           // Unformatted phone number
    private String display_phone;   // Formatted phone number
    private Location location;      // Location object for address
    private List<Hour> hours;       // Hours object for business hours

    // Getters for phone and display_phone
    public String getPhone() {
        return phone;
    }

    public String getDisplayPhone() {
        return display_phone;
    }

    // Getter for location
    public Location getLocation() {
        return location;
    }

    // Getter for hours
    public List<Hour> getHours() {
        return hours;
    }

    // Inner class for location (address details)
    public class Location {
        private String address1;
        private String address2;
        private String city;
        private String state;
        private String zip_code;
        private String country;

        // Getters for each address field
        public String getAddress1() {
            return address1;
        }

        public String getAddress2() {
            return address2;
        }

        public String getCity() {
            return city;
        }

        public String getState() {
            return state;
        }

        public String getZipCode() {
            return zip_code;
        }

        public String getCountry() {
            return country;
        }
    }

    // Inner class for business hours
    public class Hour {
        private List<Open> open;

        public List<Open> getOpen() {
            return open;
        }

        public class Open {
            private int day;
            private String start;
            private String end;

            public int getDay() {
                return day;
            }

            public String getStart() {
                return start;
            }

            public String getEnd() {
                return end;
            }

            // Check if the business is closed (no start or end times mean it's closed)
            public boolean getIsClosed() {
                return start == null || end == null;
            }
        }
    }
}
