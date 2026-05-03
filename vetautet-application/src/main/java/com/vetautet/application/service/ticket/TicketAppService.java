package com.vetautet.application.service.ticket;

import com.vetautet.application.dto.TicketResponse;
import com.vetautet.application.dto.TicketUpdateRequest;
import java.util.List;

public interface TicketAppService {
    List<TicketResponse> getTicketsByTrip(Long tripId);
    TicketResponse updateTicket(Long id, TicketUpdateRequest request);
    void releaseExpiredTickets();
    void deleteTicket(Long id);
}
