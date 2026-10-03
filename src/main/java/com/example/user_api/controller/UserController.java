package com.example.user_api.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.user_api.dto.UserCreateDTO;
import com.example.user_api.service.UserService;

import jakarta.validation.Valid;

import com.example.user_api.dto.ApiResponse;
import com.example.user_api.dto.UserResponseDTO;
import com.example.user_api.dto.UserUpdateDTO;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.example.user_api.entity.User;

@RestController
@RequestMapping("/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  

  @GetMapping("/profile")
  public ResponseEntity<ApiResponse<UserResponseDTO>> getMyProfile(Authentication authentication) {
    // authentication.getName() returns the email we put inside the JWT subject!
    String email = authentication.getName();

    UserResponseDTO user = userService.getUserByEmail(email);

    ApiResponse<UserResponseDTO> response = new ApiResponse<>(
        true,
        "Profile retrieved successfully",
        user);

    return ResponseEntity.ok(response);
  }

  // 2. Upload Profile Picture for the Logged-in User (No ID in URL anymore!)
  @PostMapping("/profile-picture")
  public ResponseEntity<ApiResponse<String>> uploadMyProfilePicture(
      Authentication authentication,
      @RequestParam("file") MultipartFile file) {

    String email = authentication.getName();
    String imageUrl = userService.uploadProfilePictureByEmail(email, file);

    ApiResponse<String> response = new ApiResponse<>(
        true,
        "Profile picture uploaded successfully",
        imageUrl);

    return ResponseEntity.ok(response);
  }

 @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // GET /api/users/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // PUT /api/users/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody UserUpdateDTO dto) {
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }

    // DELETE /api/users/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }

}
