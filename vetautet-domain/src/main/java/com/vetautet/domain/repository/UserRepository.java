package com.vetautet.domain.repository;

import com.vetautet.domain.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    List<User> findAll();
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    User save(User user);
    boolean existsByEmail(String email);
    void deleteById(Long id);
}
