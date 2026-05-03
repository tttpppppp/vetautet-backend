package com.vetautet.domain.repository;

import com.vetautet.domain.model.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    List<Role> findAll();
    Optional<Role> findByCode(String code);
}
