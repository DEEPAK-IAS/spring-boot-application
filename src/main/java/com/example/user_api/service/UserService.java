package com.example.user_api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.user_api.dto.AuthResponseDTO;
import com.example.user_api.dto.ForgetPasswordDTO;
import com.example.user_api.dto.LoginRequestDTO;
import com.example.user_api.dto.ResetPasswordDTO;
import com.example.user_api.dto.UserCreateDTO;
import com.example.user_api.dto.UserResponseDTO;
import com.example.user_api.dto.UserUpdateDTO;
import com.example.user_api.dto.VerifyOtpDTO;
import com.example.user_api.entity.User;
import com.example.user_api.exception.ResourceNotFoundException;
import com.example.user_api.repository.UserRepository;
import com.example.user_api.security.JwtUtil;
import com.example.user_api.utils.OtpUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

  private final EmailService emailService;
  private final CloudinaryService cloudinaryService;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
      EmailService emailService, CloudinaryService cloudinaryService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
    this.emailService = emailService;
    this.cloudinaryService = cloudinaryService;
  }

  public UserResponseDTO createUser(UserCreateDTO dto) {
    User user = new User();
    user.setName(dto.getName());
    user.setEmail(dto.getEmail());
    user.setPassword(passwordEncoder.encode(dto.getPassword()));

    User savedUser = userRepository.save(user);

    return new UserResponseDTO(
        savedUser.getId(),
        savedUser.getName(),
        savedUser.getEmail());
  }

  public AuthResponseDTO login(LoginRequestDTO dto) {
    User user = userRepository.findByEmail(dto.getEmail())
        .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password"));

    if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
      throw new ResourceNotFoundException("Invalid email or password");
    }

    // Generate both tokens
    String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());
    String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

    return new AuthResponseDTO(accessToken, refreshToken);
  }

  public String refreshAccessToken(String refreshToken) {
    // 1. Validate the refresh token signature and expiration
    if (!jwtUtil.isTokenValid(refreshToken)) {
      throw new RuntimeException("Invalid or expired refresh token");
    }

    // 2. Extract email from the refresh token
    String email = jwtUtil.extractEmail(refreshToken);

    // 3. Fetch the user from the database to get their CURRENT role
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    // 4. Issue a brand new short-lived access token with their current role
    return jwtUtil.generateAccessToken(user.getEmail(), user.getRole());
  }

  public void forgotPassword(ForgetPasswordDTO dto) {

    User user = userRepository.findByEmail(dto.getEmail())
        .orElseThrow(() -> new ResourceNotFoundException("invalid Email"));

    String otp = OtpUtil.generateOtp();

    user.setOtp(otp);
    user.setOtpGeneratedTime(LocalDateTime.now().plusMinutes(10));

    // 4. Save the updated user back to PostgreSQL
    userRepository.save(user);

    // 5. Send the email
    emailService.sendOtpEmail(user.getEmail(), otp);

  }

  public void verifyOtp(VerifyOtpDTO dto) {
    User user = userRepository.findByEmail(dto.getEmail())
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + dto.getEmail()));

    // 1. Check if OTP matches
    if (user.getOtp() == null || !user.getOtp().equals(dto.getOtp())) {
      throw new IllegalArgumentException("Invalid OTP");
    }

    // 2. Check if OTP has expired (Current time is after expiration time)
    if (LocalDateTime.now().isAfter(user.getOtpGeneratedTime())) {
      throw new IllegalArgumentException("OTP has expired");
    }
  }

  public void resetPassword(ResetPasswordDTO dto) {
    User user = userRepository.findByEmail(dto.getEmail())
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + dto.getEmail()));

    // 1. Re-validate OTP for security
    if (user.getOtp() == null || !user.getOtp().equals(dto.getOtp())) {
      throw new IllegalArgumentException("Invalid OTP");
    }

    if (LocalDateTime.now().isAfter(user.getOtpGeneratedTime())) {
      throw new IllegalArgumentException("OTP has expired");
    }

    // 2. Hash the new password using BCrypt
    user.setPassword(passwordEncoder.encode(dto.getNewPassword()));

    // 3. Clear out the OTP fields so it can never be reused
    user.setOtp(null);
    user.setOtpGeneratedTime(null);

    // 4. Save the updated user to PostgreSQL
    userRepository.save(user);
  }

  // 1. Get profile details by email
  public UserResponseDTO getUserByEmail(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

    return new UserResponseDTO(
        user.getId(),
        user.getName(),
        user.getEmail());
  }

  // 2. Upload profile picture using the authenticated user's email
  public String uploadProfilePictureByEmail(String email, MultipartFile file) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

    // Upload to Cloudinary
    String imageUrl = cloudinaryService.uploadImage(file);

    // Save URL to this specific user's record
    user.setProfilePictureUrl(imageUrl);
    userRepository.save(user);

    return imageUrl;
  }

  public AuthResponseDTO firebaseLogin(String firebaseIdToken) {
    try {
      // 1. Verify the token using Firebase Admin SDK
      FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(firebaseIdToken);
      String email = decodedToken.getEmail();
      String name = decodedToken.getName();
      String pictureUrl = decodedToken.getPicture(); // <-- Extract profile picture URL

      // 2. Check if user already exists in PostgreSQL; if not, auto-register them
      User user = userRepository.findByEmail(email).orElseGet(() -> {
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(name != null ? name : "Social User");
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        newUser.setRole("ROLE_USER");
        newUser.setProfilePictureUrl(pictureUrl); // Save picture on creation
        return userRepository.save(newUser);
      });

      // Optional: Update profile picture on subsequent logins if it changed
      if (pictureUrl != null && !pictureUrl.equals(user.getProfilePictureUrl())) {
        user.setProfilePictureUrl(pictureUrl);
        userRepository.save(user);
      }

      // 3. Generate your application's tokens
      String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());
      String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

      return new AuthResponseDTO(accessToken, refreshToken);

    } catch (FirebaseAuthException e) {
      throw new RuntimeException("Invalid Firebase ID token: " + e.getMessage());
    }
  }

  // 1. Get All Users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 2. Get User By ID
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    // 3. Update User
    public User updateUser(Long id, UserUpdateDTO dto) {
        User user = getUserById(id);
        user.setName(dto.getName());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        return userRepository.save(user);
    }

    // 4. Delete User
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }
}