package com.vetautet.infrastructure.persistence.repository.impl;

import com.vetautet.domain.model.Role;
import com.vetautet.domain.model.User;
import com.vetautet.domain.repository.UserRepository;
import com.vetautet.infrastructure.persistence.entity.RoleEntity;
import com.vetautet.infrastructure.persistence.mapper.PersistenceMapper;
import com.vetautet.infrastructure.persistence.repository.RoleJpaRepository;
import com.vetautet.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class UserInfrasRepositoryImpl implements UserRepository {

    @Autowired
    private UserJpaRepository jpaRepository;
    
    @Autowired
    private RoleJpaRepository roleJpaRepository;
    
    @Autowired
    private PersistenceMapper mapper;

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            return mapper.toDomain(jpaRepository.save(mapper.toEntity(user)));
        }

        return jpaRepository.findById(user.getId())
                .map(existing -> {
                    existing.setName(user.getName());
                    existing.setEmail(user.getEmail());
                    existing.setPassword(user.getPassword());
                    existing.setPhone(user.getPhone());
                    existing.setAddress(user.getAddress());
                    existing.setNationality(user.getNationality());
                    existing.setImageUrl(user.getImageUrl());
                    existing.setRewardPoints(user.getRewardPoints());
                    existing.setMembershipRank(user.getMembershipRank());
                    existing.setIsIdentityVerified(user.getIsIdentityVerified());
                    existing.setIsEmailVerified(user.getIsEmailVerified());
                    existing.setCreatedAt(user.getCreatedAt());
                    existing.setUpdatedAt(user.getUpdatedAt());

                    if (user.getRoles() != null) {
                        if (!hasSameRoleIds(existing.getRoles(), user.getRoles())) {
                            existing.getRoles().clear();
                            existing.getRoles().addAll(user.getRoles().stream()
                                    .map(role -> roleJpaRepository.getReferenceById(role.getId()))
                                    .collect(Collectors.toSet()));
                        }
                    }

                    return mapper.toDomain(jpaRepository.save(existing));
                })
                .orElseGet(() -> mapper.toDomain(jpaRepository.save(mapper.toEntity(user))));
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private boolean hasSameRoleIds(Set<RoleEntity> existingRoles, Set<Role> newRoles) {
        if (existingRoles == null) {
            existingRoles = Set.of();
        }
        if (newRoles == null) {
            return existingRoles.isEmpty();
        }
        if (existingRoles.size() != newRoles.size()) {
            return false;
        }

        Set<Long> existingIds = existingRoles.stream()
                .map(RoleEntity::getId)
                .collect(Collectors.toSet());
        Set<Long> newIds = newRoles.stream()
                .map(Role::getId)
                .collect(Collectors.toSet());
        return existingIds.equals(newIds);
    }
}
