package com.vetautet.application.service.user.impl;

import com.vetautet.application.dto.AuthResponse;
import com.vetautet.application.dto.EmailVerificationResponse;
import com.vetautet.application.dto.ForgotPasswordRequest;
import com.vetautet.application.dto.PasswordResetResponse;
import com.vetautet.application.dto.RegisterRequest;
import com.vetautet.application.dto.ResendVerificationOtpRequest;
import com.vetautet.application.dto.ResetPasswordRequest;
import com.vetautet.application.dto.TicketResponse;
import com.vetautet.application.dto.UpdateProfileRequest;
import com.vetautet.application.dto.UserResponse;
import com.vetautet.application.dto.UserUpdateRequest;
import com.vetautet.application.dto.VerifyEmailRequest;
import com.vetautet.application.mapper.UserMapper;
import com.vetautet.application.service.user.UserAppService;
import com.vetautet.domain.exception.BusinessException;
import com.vetautet.domain.gateway.EmailVerificationMailGateway;
import com.vetautet.domain.gateway.GoogleAuthGateway;
import com.vetautet.domain.gateway.TokenGateway;
import com.vetautet.domain.model.EmailVerificationOtp;
import com.vetautet.domain.model.GoogleUserInfo;
import com.vetautet.domain.model.RefreshToken;
import com.vetautet.domain.model.User;
import com.vetautet.domain.service.EmailVerificationOtpDomainService;
import com.vetautet.domain.service.RefreshTokenDomainService;
import com.vetautet.domain.service.TicketDomainService;
import com.vetautet.domain.service.UserDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserAppServiceImpl implements UserAppService {
    private static final int EMAIL_OTP_EXPIRES_MINUTES = 15;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private TokenGateway tokenGateway;

    @Autowired
    private GoogleAuthGateway googleAuthGateway;

    @Autowired
    private RefreshTokenDomainService refreshTokenDomainService;

    @Autowired
    private TicketDomainService ticketDomainService;

    @Autowired
    private EmailVerificationOtpDomainService emailVerificationOtpDomainService;

    @Autowired
    private EmailVerificationMailGateway emailVerificationMailGateway;

    @Autowired
    private UserMapper userMapper;

    @Override
    public AuthResponse login(String email, String password) {
        email = normalizeEmail(email);
        if (userDomainService.authenticate(email, password)) {
            User user = userDomainService.getByEmail(email);
            ensureEmailVerified(user);

            String accessToken = tokenGateway.generateAccessToken(user.getEmail(), user.getId());
            String refreshTokenString = tokenGateway.generateRefreshToken(user.getEmail(), user.getId());

            saveRefreshToken(user, refreshTokenString);

            return buildAuthResponse(accessToken, refreshTokenString, user);
        }
        throw new RuntimeException("Sai tai khoan hoac mat khau");
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        GoogleUserInfo googleUser = googleAuthGateway.verifyIdToken(idToken);
        String email = normalizeEmail(googleUser.getEmail());

        User user;
        if (userDomainService.existsByEmail(email)) {
            user = userDomainService.getByEmail(email);
            if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
                user = userDomainService.markEmailVerified(email);
            }
            user = syncGoogleProfileIfNeeded(user, googleUser);
        } else {
            User newUser = new User(resolveGoogleName(googleUser), email, generateExternalPassword());
            newUser.setImageUrl(googleUser.getPictureUrl());
            user = userDomainService.register(newUser);
            user = userDomainService.markEmailVerified(user.getEmail());
        }

        String accessToken = tokenGateway.generateAccessToken(user.getEmail(), user.getId());
        String refreshTokenString = tokenGateway.generateRefreshToken(user.getEmail(), user.getId());
        saveRefreshToken(user, refreshTokenString);

        AuthResponse response = buildAuthResponse(accessToken, refreshTokenString, user);
        setAuthCode(response, "GOOGLE_LOGIN_SUCCESS");
        return response;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userDomainService.existsByEmail(email)) {
            User existingUser = userDomainService.getByEmail(email);
            if (Boolean.TRUE.equals(existingUser.getIsEmailVerified())) {
                throw new BusinessException("EMAIL_ALREADY_USED");
            }

            EmailVerificationOtp verificationOtp = createAndSendVerificationOtp(existingUser);
            AuthResponse response = buildAuthResponse(null, null, existingUser);
            response.setRequiresEmailVerification(true);
            response.setEmailAlreadyRegistered(true);
            setAuthCode(response, "EMAIL_UNVERIFIED_OTP_SENT");
            System.out.println(">>> [EMAIL VERIFY] Resent OTP for existing unverified email=" + existingUser.getEmail()
                    + ", expiresAt=" + verificationOtp.getExpiresAt());
            return response;
        }

        User user = new User(request.getName(), email, request.getPassword());
        user.setPhone(request.getPhone());

        User savedUser = userDomainService.register(user);
        EmailVerificationOtp verificationOtp = createAndSendVerificationOtp(savedUser);

        AuthResponse response = buildAuthResponse(null, null, savedUser);
        response.setRequiresEmailVerification(true);
        response.setEmailAlreadyRegistered(false);
        setAuthCode(response, "ACCOUNT_CREATED_OTP_SENT");
        System.out.println(">>> [EMAIL VERIFY] Created OTP for email=" + savedUser.getEmail()
                + ", expiresAt=" + verificationOtp.getExpiresAt());
        return response;
    }

    @Override
    @Transactional
    public EmailVerificationResponse verifyEmail(VerifyEmailRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userDomainService.getByEmail(email);

        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            return new EmailVerificationResponse(email, true, "EMAIL_ALREADY_VERIFIED", "EMAIL_ALREADY_VERIFIED", null);
        }

        emailVerificationOtpDomainService.verifyOtp(email, request.getOtp());
        User verifiedUser = userDomainService.markEmailVerified(email);

        return new EmailVerificationResponse(
                verifiedUser.getEmail(),
                Boolean.TRUE.equals(verifiedUser.getIsEmailVerified()),
                "EMAIL_VERIFIED",
                "EMAIL_VERIFIED",
                null
        );
    }

    @Override
    @Transactional
    public EmailVerificationResponse resendVerificationOtp(ResendVerificationOtpRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userDomainService.getByEmail(email);

        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            return new EmailVerificationResponse(email, true, "EMAIL_ALREADY_VERIFIED", "EMAIL_ALREADY_VERIFIED", null);
        }

        EmailVerificationOtp verificationOtp = createAndSendVerificationOtp(user);
        return new EmailVerificationResponse(
                email,
                false,
                "OTP_RESENT",
                "OTP_RESENT",
                verificationOtp.getExpiresAt()
        );
    }

    @Override
    @Transactional
    public PasswordResetResponse requestPasswordReset(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (!userDomainService.existsByEmail(email)) {
            throw new BusinessException("EMAIL_NOT_REGISTERED");
        }

        User user = userDomainService.getByEmail(email);
        EmailVerificationOtp resetOtp = createAndSendPasswordResetOtp(user);
        return new PasswordResetResponse(
                email,
                "PASSWORD_RESET_OTP_SENT",
                "PASSWORD_RESET_OTP_SENT",
                resetOtp.getExpiresAt()
        );
    }

    @Override
    @Transactional
    public PasswordResetResponse resetPassword(ResetPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userDomainService.getByEmail(email);

        emailVerificationOtpDomainService.verifyOtp(email, request.getOtp());
        userDomainService.updatePassword(email, request.getNewPassword());
        if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
            userDomainService.markEmailVerified(email);
        }
        refreshTokenDomainService.revokeAllByUserId(user.getId());

        return new PasswordResetResponse(
                email,
                "PASSWORD_RESET_SUCCESS",
                "PASSWORD_RESET_SUCCESS",
                null
        );
    }

    @Override
    public UserResponse getProfile(String email) {
        User user = userDomainService.getByEmail(email);
        UserResponse response = userMapper.toResponse(user);
        response.setTripsCount((int) ticketDomainService.countTicketsByUser(user.getId()));
        return response;
    }

    @Override
    public UserResponse getProfileById(Long id) {
        User user = userDomainService.getById(id);
        UserResponse response = userMapper.toResponse(user);
        response.setTripsCount((int) ticketDomainService.countTicketsByUser(user.getId()));
        return response;
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User savedUser = userDomainService.updateProfile(
                userId,
                request.getName(),
                request.getPhone(),
                request.getAddress(),
                request.getNationality(),
                request.getImageUrl()
        );
        UserResponse response = userMapper.toResponse(savedUser);
        response.setTripsCount((int) ticketDomainService.countTicketsByUser(savedUser.getId()));
        return response;
    }

    @Override
    public List<TicketResponse> getMyTickets(Long userId) {
        User user = userDomainService.getById(userId);

        return ticketDomainService.getTicketsByUser(user.getId()).stream()
                .map(userMapper::toTicketResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshTokenString) {
        RefreshToken tokenFromDb = refreshTokenDomainService.getByToken(refreshTokenString);

        if (tokenFromDb.isRevoked()) {
            refreshTokenDomainService.revokeAllByUserId(tokenFromDb.getUserId());
            throw new RuntimeException("Canh bao bao mat: Token da duoc su dung truoc do. Vui long dang nhap lai!");
        }

        String userEmail = tokenGateway.extractUsername(refreshTokenString);

        if (!tokenGateway.isTokenValid(refreshTokenString, userEmail)) {
            throw new RuntimeException("Token da het han");
        }

        tokenFromDb.setRevoked(true);
        refreshTokenDomainService.save(tokenFromDb);

        User user = userDomainService.getByEmail(userEmail);
        ensureEmailVerified(user);
        String newAccessToken = tokenGateway.generateAccessToken(user.getEmail(), user.getId());
        String newRefreshToken = tokenGateway.generateRefreshToken(user.getEmail(), user.getId());

        saveRefreshToken(user, newRefreshToken);

        return buildAuthResponse(newAccessToken, newRefreshToken, user);
    }

    @Override
    @Transactional
    public void logout(String refreshTokenString) {
        RefreshToken tokenFromDb = refreshTokenDomainService.getByToken(refreshTokenString);

        tokenFromDb.setRevoked(true);
        refreshTokenDomainService.save(tokenFromDb);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userDomainService.getAllUsers().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User savedUser = userDomainService.updateAdminUser(id, request.getName(), request.getPhone(), request.getRoles());
        return userMapper.toResponse(savedUser);
    }

    @Override
    public List<String> listRoles() {
        return userDomainService.getRoleCodes();
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userDomainService.deleteUser(id);
    }

    private void saveRefreshToken(User user, String token) {
        refreshTokenDomainService.revokeAllByUserId(user.getId());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setToken(token);
        refreshToken.setExpiredAt(LocalDateTime.now().plusWeeks(1));
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(LocalDateTime.now());

        refreshTokenDomainService.save(refreshToken);
    }

    private EmailVerificationOtp createAndSendVerificationOtp(User user) {
        String otp = generateSixDigitOtp();
        EmailVerificationOtp verificationOtp = emailVerificationOtpDomainService.createOtp(
                user.getId(),
                user.getEmail(),
                otp,
                LocalDateTime.now().plusMinutes(EMAIL_OTP_EXPIRES_MINUTES)
        );

        emailVerificationMailGateway.sendVerificationOtp(
                user.getEmail(),
                user.getName(),
                otp,
                EMAIL_OTP_EXPIRES_MINUTES
        );

        return verificationOtp;
    }

    private EmailVerificationOtp createAndSendPasswordResetOtp(User user) {
        String otp = generateSixDigitOtp();
        EmailVerificationOtp resetOtp = emailVerificationOtpDomainService.createOtp(
                user.getId(),
                user.getEmail(),
                otp,
                LocalDateTime.now().plusMinutes(EMAIL_OTP_EXPIRES_MINUTES)
        );

        emailVerificationMailGateway.sendPasswordResetOtp(
                user.getEmail(),
                user.getName(),
                otp,
                EMAIL_OTP_EXPIRES_MINUTES
        );

        return resetOtp;
    }

    private String generateSixDigitOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        boolean emailVerified = Boolean.TRUE.equals(user.getIsEmailVerified());
        return new AuthResponse(accessToken, refreshToken, user.getEmail(), emailVerified, !emailVerified);
    }

    private void ensureEmailVerified(User user) {
        if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new BusinessException("LOGIN_REQUIRES_EMAIL_VERIFICATION");
        }
    }

    private void setAuthCode(AuthResponse response, String code) {
        response.setCode(code);
        response.setMessage(code);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String resolveGoogleName(GoogleUserInfo googleUser) {
        if (googleUser.getName() != null && !googleUser.getName().isBlank()) {
            return googleUser.getName().trim();
        }
        String email = normalizeEmail(googleUser.getEmail());
        int atIndex = email == null ? -1 : email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : "Google User";
    }

    private String generateExternalPassword() {
        return "GOOGLE_LOGIN_" + UUID.randomUUID() + "_" + UUID.randomUUID();
    }

    private User syncGoogleProfileIfNeeded(User user, GoogleUserInfo googleUser) {
        boolean shouldSyncName = (user.getName() == null || user.getName().isBlank())
                && googleUser.getName() != null
                && !googleUser.getName().isBlank();
        boolean shouldSyncImage = (user.getImageUrl() == null || user.getImageUrl().isBlank())
                && googleUser.getPictureUrl() != null
                && !googleUser.getPictureUrl().isBlank();

        if (!shouldSyncName && !shouldSyncImage) {
            return user;
        }

        return userDomainService.updateProfile(
                user.getId(),
                shouldSyncName ? googleUser.getName().trim() : null,
                null,
                null,
                null,
                shouldSyncImage ? googleUser.getPictureUrl() : null
        );
    }
}
