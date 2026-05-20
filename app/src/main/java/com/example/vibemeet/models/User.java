package com.example.vibemeet.models;

public class User {
    private String id;
    private String name;
    private String email;
    private String profileImageUrl;
    private double latitude;
    private double longitude;
    private String bio;
    private String[] interests; // activities/categories user likes

    public User() {}

    public User(String id, String name, String email, String profileImageUrl) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String[] getInterests() { return interests; }
    public void setInterests(String[] interests) { this.interests = interests; }
}
