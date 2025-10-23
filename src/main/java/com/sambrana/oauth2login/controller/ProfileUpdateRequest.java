package com.sambrana.oauth2login.controller;

public class ProfileUpdateRequest {
    private String displayName;
    private String bio;

   
    public String getDisplayName() {
        return displayName;
    }
    public String getBio() {
        return bio;
    }


    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public void setBio(String bio) {
        this.bio = bio;
    }
}