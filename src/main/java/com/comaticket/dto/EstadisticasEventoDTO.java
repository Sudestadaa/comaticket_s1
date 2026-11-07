package com.comaticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EstadisticasEventoDTO {
    private String eventoId;
    private long totalReservas;
    private long totalConfirmados;
    private long totalCancelados;
    private double ingresosTotales;
}