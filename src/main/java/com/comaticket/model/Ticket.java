package com.comaticket.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    private String id; // Ejemplo: TICKET-001

    private String eventoId;
    private String usuarioId;
    private String nombreCliente;
    private Double precio;
    private String estado; // RESERVADO, CONFIRMADO, CANCELADO

    private String efsPath; // Ruta del archivo temporal guardado en EFS
    private String s3Key;   // Ruta dentro de S3 (/eventoId/usuarioId/ticketId.pdf)
    private String s3Url;

    private LocalDateTime fechaCreacion;
}