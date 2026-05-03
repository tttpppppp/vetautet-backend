package com.vetautet.domain.service.impl;

import com.vetautet.domain.repository.HiDomainRepository;
import com.vetautet.domain.service.HiDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HiDomainServiceImpl implements HiDomainService {
    @Autowired
    HiDomainRepository hiDomainRepository;


    @Override
    public String sayHi(String name) {
        return hiDomainRepository.sayHi(name);
    }
}
