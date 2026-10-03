package com.example.user_api.controller;

import com.example.user_api.dto.ApiResponse;
import com.example.user_api.dto.AuthResponseDTO;
import com.example.user_api.dto.FirebaseLoginRequestDTO;
import com.example.user_api.dto.ForgetPasswordDTO;
import com.example.user_api.dto.LoginRequestDTO;
import com.example.user_api.dto.ResetPasswordDTO;
import com.example.user_api.dto.UserCreateDTO;
import com.example.user_api.dto.UserResponseDTO;
import com.example.user_api.dto.VerifyOtpDTO;
import com.example.user_api.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Null;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUser(@Valid @RequestBody UserCreateDTO dto) {

        UserResponseDTO user = userService.createUser(dto);

        ApiResponse<UserResponseDTO> response = new ApiResponse<>(
                true,
                "User created successfully",
                user);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@Valid @RequestBody LoginRequestDTO dto) {
        // userService.login now returns AuthResponseDTO (containing access + refresh
        // tokens)
        AuthResponseDTO authResponse = userService.login(dto);

        ApiResponse<AuthResponseDTO> apiResponse = new ApiResponse<>(
                true,
                "Login successful",
                authResponse);

        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshAccessToken(@RequestBody Map<Object, Object> request) {
        String refreshToken = (String) request.get("refreshToken");

        String newAccessToken = userService.refreshAccessToken(refreshToken);

        ApiResponse<String> response = new ApiResponse<>(
                true,
                "Access token refreshed successfully",
                newAccessToken);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Null>> postMethodName(@Valid @RequestBody ForgetPasswordDTO dto) {

        userService.forgotPassword(dto);

        ApiResponse<Null> response = new ApiResponse<>(true, "Otp sent to your mail id", null);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Null>> verifyOtp(@Valid @RequestBody VerifyOtpDTO dto) {
        userService.verifyOtp(dto);

        ApiResponse<Null> response = new ApiResponse<>(
                true,
                "OTP verified successfully",
                null);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Null>> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(dto);

        ApiResponse<Null> response = new ApiResponse<>(
                true,
                "Password has been reset successfully. You can now login with your new password.",
                null);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/firebase")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> firebaseLogin(@RequestBody FirebaseLoginRequestDTO dto) {
        AuthResponseDTO authResponse = userService.firebaseLogin(dto.getIdToken());

        ApiResponse<AuthResponseDTO> response = new ApiResponse<>(
                true,
                "Firebase social login successful",
                authResponse);

        return ResponseEntity.ok(response);
    }

}