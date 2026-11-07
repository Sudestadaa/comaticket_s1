package com.comaticket.dto;

import lombok.Data;

@Data
public class TicketRequest {
    private String eventoId;
    private String usuarioId;
    private String nombreCliente;
    private Double precio;
}