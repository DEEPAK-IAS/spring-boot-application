package com.example.user_api.dto;

public class FirebaseLoginRequestDTO {
    private String idToken;

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}