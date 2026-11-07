package com.comaticket.dto;

import lombok.Data;

@Data
public class TicketUpdateRequest {
    private String nombreCliente;
    private Double precio;
    private String estado;
}