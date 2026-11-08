package com.comaticket.dto;

import lombok.Data;

@Data
public class TicketRequest {

    private String eventoId;
    private String usuarioId;
    private String nombreCliente;
    private Double precio;

    public void setEventoId(String eventoId) {
        this.eventoId = (eventoId != null) ? eventoId.trim().replaceAll("\\s+", "_") : null;
    }
}
