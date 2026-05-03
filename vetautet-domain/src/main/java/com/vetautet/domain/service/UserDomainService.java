package com.vetautet.domain.service;

import com.vetautet.domain.model.User;

import java.util.List;
import java.util.Set;

public interface UserDomainService {
    boolean authenticate(String email, String password);
    User register(User user);
    boolean existsByEmail(String email);
    User getByEmail(String email);
    User getById(Long id);
    User markEmailVerified(String email);
    User updatePassword(String email, String newPassword);
    List<User> getAllUsers();
    User updateProfile(Long userId, String name, String phone, String address, String nationality, String imageUrl);
    User updateAdminUser(Long userId, String name, String phone, Set<String> roleCodes);
    List<String> getRoleCodes();
    void deleteUser(Long id);
}
