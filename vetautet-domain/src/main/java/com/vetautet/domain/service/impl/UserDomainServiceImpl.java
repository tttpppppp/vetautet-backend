package com.vetautet.domain.service.impl;

import com.vetautet.domain.exception.BusinessException;
import com.vetautet.domain.model.Role;
import com.vetautet.domain.model.User;
import com.vetautet.domain.repository.RoleRepository;
import com.vetautet.domain.repository.UserRepository;
import com.vetautet.domain.service.PasswordService;
import com.vetautet.domain.service.UserDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserDomainServiceImpl implements UserDomainService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordService passwordService;

    @Override
    public boolean authenticate(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(normalizeEmail(email));
        if (userOpt.isPresent()) {
            return passwordService.matches(password, userOpt.get().getPassword());
        }
        return false;
    }

    @Override
    public User register(User user) {
        user.setEmail(normalizeEmail(user.getEmail()));
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BusinessException("EMAIL_ALREADY_USED");
        }

        user.setPassword(passwordService.encode(user.getPassword()));
        user.setIsEmailVerified(false);

        Optional<Role> customerRole = roleRepository.findByCode("CUSTOMER");
        customerRole.ifPresent(role -> user.getRoles().add(role));

        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(normalizeEmail(email));
    }

    @Override
    public User getByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));
    }

    @Override
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));
    }

    @Override
    public User markEmailVerified(String email) {
        User user = getByEmail(email);
        user.setIsEmailVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public User updatePassword(String email, String newPassword) {
        User user = getByEmail(email);
        user.setPassword(passwordService.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User updateProfile(Long userId, String name, String phone, String address, String nationality, String imageUrl) {
        User user = getById(userId);

        if (name != null && !name.isBlank()) {
            user.setName(name);
        }
        if (phone != null && !phone.isBlank()) {
            user.setPhone(phone);
        }
        if (address != null && !address.isBlank()) {
            user.setAddress(address);
        }
        if (nationality != null && !nationality.isBlank()) {
            user.setNationality(nationality);
        }
        if (imageUrl != null && !imageUrl.isBlank()) {
            user.setImageUrl(imageUrl);
        }
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    @Override
    public User updateAdminUser(Long userId, String name, String phone, Set<String> roleCodes) {
        User user = getById(userId);

        if (name != null) {
            user.setName(name);
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        if (roleCodes != null) {
            Set<Role> roles = roleCodes.stream()
                    .map(code -> roleRepository.findByCode(code)
                            .orElseThrow(() -> new RuntimeException("Role not found: " + code)))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    @Override
    public List<String> getRoleCodes() {
        return roleRepository.findAll().stream()
                .map(Role::getCode)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
