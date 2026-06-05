package com.vetautet.controller.resource;

import com.vetautet.application.dto.AuthResponse;
import com.vetautet.application.dto.EmailVerificationResponse;
import com.vetautet.application.dto.ForgotPasswordRequest;
import com.vetautet.application.dto.GoogleLoginRequest;
import com.vetautet.application.dto.LoginRequest;
import com.vetautet.application.dto.PasswordResetResponse;
import com.vetautet.application.dto.RegisterRequest;
import com.vetautet.application.dto.ResendVerificationOtpRequest;
import com.vetautet.application.dto.ResetPasswordRequest;
import com.vetautet.application.dto.UserResponse;
import com.vetautet.application.dto.UpdateProfileRequest;
import com.vetautet.application.dto.TicketResponse;
import com.vetautet.application.dto.VerifyEmailRequest;
import com.vetautet.application.service.user.UserAppService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserAppService userAppService;

    @PostMapping("/login")
    @RateLimiter(name = "authLogin")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(userAppService.login(loginRequest.getEmail(), loginRequest.getPassword()));
    }

    @PostMapping("/google")
    @RateLimiter(name = "authLogin")
    public ResponseEntity<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(userAppService.loginWithGoogle(request.getToken()));
    }

    @PostMapping("/register")
    @RateLimiter(name = "authOtp")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(userAppService.register(registerRequest));
    }

    @PostMapping("/verify-email")
    @RateLimiter(name = "authOtp")
    public ResponseEntity<EmailVerificationResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(userAppService.verifyEmail(request));
    }

    @PostMapping("/resend-verification-otp")
    @RateLimiter(name = "authOtp")
    public ResponseEntity<EmailVerificationResponse> resendVerificationOtp(
            @Valid @RequestBody ResendVerificationOtpRequest request) {
        return ResponseEntity.ok(userAppService.resendVerificationOtp(request));
    }

    @PostMapping("/forgot-password/request")
    @RateLimiter(name = "authOtp")
    public ResponseEntity<PasswordResetResponse> requestPasswordReset(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(userAppService.requestPasswordReset(request));
    }

    @PostMapping("/forgot-password/reset")
    @RateLimiter(name = "authOtp")
    public ResponseEntity<PasswordResetResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(userAppService.resetPassword(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal(expression = "userId") Long userId) {
        return ResponseEntity.ok(userAppService.getProfileById(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userAppService.updateProfile(userId, request));
    }

    @GetMapping("/my-tickets")
    public ResponseEntity<List<TicketResponse>> getMyTickets(@AuthenticationPrincipal(expression = "userId") Long userId) {
        return ResponseEntity.ok(userAppService.getMyTickets(userId));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        return ResponseEntity.ok(userAppService.refreshToken(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        userAppService.logout(refreshToken);
        return ResponseEntity.ok().build();
    }
}
