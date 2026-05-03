package com.vetautet.application.service.event.impl;

import com.vetautet.application.service.event.EventAppService;
import com.vetautet.domain.service.HiDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventAppServiceImpl implements EventAppService {
    @Autowired
    private HiDomainService hiDomainService;

    @Override
    public String sayHi(String name) {
        return  hiDomainService.sayHi(name);
    }
}
