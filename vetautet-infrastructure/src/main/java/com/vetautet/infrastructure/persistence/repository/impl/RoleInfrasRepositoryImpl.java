package com.vetautet.infrastructure.persistence.repository.impl;

import com.vetautet.domain.model.Role;
import com.vetautet.domain.repository.RoleRepository;
import com.vetautet.infrastructure.persistence.mapper.PersistenceMapper;
import com.vetautet.infrastructure.persistence.repository.RoleJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class RoleInfrasRepositoryImpl implements RoleRepository {

    @Autowired
    private RoleJpaRepository jpaRepository;
    
    @Autowired
    private PersistenceMapper mapper;

    @Override
    public List<Role> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Role> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }
}
