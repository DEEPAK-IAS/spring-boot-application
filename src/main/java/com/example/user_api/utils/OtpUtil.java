package com.example.user_api.utils;

import java.security.SecureRandom;

public class OtpUtil {

    public static String generateOtp() {
        SecureRandom secureRandom = new SecureRandom();
        
        // Generates a random number between 100000 and 999999 (always 6 digits)
        int otpNumber = 100000 + secureRandom.nextInt(900000);
        
        return String.valueOf(otpNumber);
    }
}