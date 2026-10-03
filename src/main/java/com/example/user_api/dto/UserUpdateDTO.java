package com.example.user_api.dto;

import jakarta.validation.constraints.Size;

public class UserUpdateDTO {

    private String name;

    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    // Default Constructor
    public UserUpdateDTO() {
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}