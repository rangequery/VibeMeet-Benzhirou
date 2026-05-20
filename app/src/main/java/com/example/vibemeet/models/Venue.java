package com.example.vibemeet.models;

import java.io.Serializable;
import java.util.Calendar;

public class Venue implements Serializable {
    private String id;
    private String name;
    private String type;
    private String description;
    private double latitude;
    private double longitude;
    private String address;
    private String district; // Casablanca neighborhood
    private float rating;
    private int reviewCount;
    private String imageEmoji; // emoji as visual placeholder
    private String[] tags;
    private double distanceKm;
    private int priceLevel; // 1 = $, 2 = $$, 3 = $$$
    private int openHour; // 24h format, e.g., 8 = 8am
    private int closeHour; // 24h format, e.g., 23 = 11pm
    private String[] vibeKeywords; // e.g., "chill", "lively", "romantic"
    private String phoneNumber;
    private String website;
    private String imageUrl;
    private String topReview;
    private String topReviewAuthor;

    public Venue() {}

    public Venue(String name, String type, double latitude, double longitude, String address) {
        this.id = name.toLowerCase().replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "");
        this.name = name;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    public boolean isOpenNow() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (openHour <= closeHour) {
            return hour >= openHour && hour < closeHour;
        } else {
            // Crosses midnight
            return hour >= openHour || hour < closeHour;
        }
    }

    public String getPriceLevelText() {
        switch (priceLevel) {
            case 1: return "$";
            case 2: return "$$";
            case 3: return "$$$";
            default: return "";
        }
    }

    public String getHoursText() {
        if (openHour == 0 && closeHour == 0) return "Hours unknown";
        return String.format("%02d:00 - %02d:00", openHour, closeHour);
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public String getImageEmoji() { return imageEmoji; }
    public void setImageEmoji(String imageEmoji) { this.imageEmoji = imageEmoji; }

    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public int getPriceLevel() { return priceLevel; }
    public void setPriceLevel(int priceLevel) { this.priceLevel = priceLevel; }

    public int getOpenHour() { return openHour; }
    public void setOpenHour(int openHour) { this.openHour = openHour; }

    public int getCloseHour() { return closeHour; }
    public void setCloseHour(int closeHour) { this.closeHour = closeHour; }

    public String[] getVibeKeywords() { return vibeKeywords; }
    public void setVibeKeywords(String[] vibeKeywords) { this.vibeKeywords = vibeKeywords; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getTopReview() { return topReview; }
    public void setTopReview(String topReview) { this.topReview = topReview; }

    public String getTopReviewAuthor() { return topReviewAuthor; }
    public void setTopReviewAuthor(String topReviewAuthor) { this.topReviewAuthor = topReviewAuthor; }

    /** Estimated walking time at 5 km/h. */
    public int getWalkingMinutes() {
        return (int) Math.ceil(distanceKm / 5.0 * 60);
    }

    /** Estimated driving time at 30 km/h average city speed. */
    public int getDrivingMinutes() {
        return (int) Math.ceil(distanceKm / 30.0 * 60);
    }
}
