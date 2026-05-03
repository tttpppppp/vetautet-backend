package com.vetautet.application.service.ticket.impl;

import com.vetautet.application.dto.TicketResponse;
import com.vetautet.application.dto.TicketUpdateRequest;
import com.vetautet.application.mapper.UserMapper;
import com.vetautet.application.service.ticket.TicketAppService;
import com.vetautet.domain.model.Ticket;
import com.vetautet.domain.service.TicketDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TicketAppServiceImpl implements TicketAppService {

    @Autowired
    private TicketDomainService ticketDomainService;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<TicketResponse> getTicketsByTrip(Long tripId) {
        return ticketDomainService.getTicketsByTrip(tripId).stream()
                .map(userMapper::toTicketResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TicketResponse updateTicket(Long id, TicketUpdateRequest request) {
        Ticket saved = ticketDomainService.updateTicket(id, request.getPrice(), request.getStatus());
        return userMapper.toTicketResponse(saved);
    }

    @Override
    @Transactional
    public void releaseExpiredTickets() {
        ticketDomainService.releaseExpiredTickets();
    }

    @Override
    @Transactional
    public void deleteTicket(Long id) {
        ticketDomainService.deleteTicket(id);
    }
}
