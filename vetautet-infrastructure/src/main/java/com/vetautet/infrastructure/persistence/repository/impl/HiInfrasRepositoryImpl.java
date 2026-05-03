package com.vetautet.infrastructure.persistence.repository.impl;

import com.vetautet.domain.repository.HiDomainRepository;
import org.springframework.stereotype.Service;

@Service
public class HiInfrasRepositoryImpl implements HiDomainRepository {
    @Override
    public String sayHi(String name) {
        return "infras " + name;
    }
}
