package com.vetautet.application.service.user;

import com.vetautet.application.dto.AuthResponse;
import com.vetautet.application.dto.EmailVerificationResponse;
import com.vetautet.application.dto.ForgotPasswordRequest;
import com.vetautet.application.dto.PasswordResetResponse;
import com.vetautet.application.dto.RegisterRequest;
import com.vetautet.application.dto.ResendVerificationOtpRequest;
import com.vetautet.application.dto.ResetPasswordRequest;
import com.vetautet.application.dto.UserResponse;
import com.vetautet.application.dto.UpdateProfileRequest;
import com.vetautet.application.dto.TicketResponse;
import com.vetautet.application.dto.UserUpdateRequest;
import com.vetautet.application.dto.VerifyEmailRequest;

import java.util.List;

public interface UserAppService {
    AuthResponse login(String email, String password);
    AuthResponse loginWithGoogle(String idToken);
    AuthResponse register(RegisterRequest request);
    EmailVerificationResponse verifyEmail(VerifyEmailRequest request);
    EmailVerificationResponse resendVerificationOtp(ResendVerificationOtpRequest request);
    PasswordResetResponse requestPasswordReset(ForgotPasswordRequest request);
    PasswordResetResponse resetPassword(ResetPasswordRequest request);
    UserResponse getProfile(String email);
    UserResponse getProfileById(Long id);
    AuthResponse refreshToken(String refreshToken);
    void logout(String refreshToken);
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
    List<TicketResponse> getMyTickets(Long userId);
    List<UserResponse> getAllUsers();
    UserResponse updateUser(Long id, UserUpdateRequest request);
    List<String> listRoles();
    void deleteUser(Long id);
}
