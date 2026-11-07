package com.comaticket.service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.comaticket.dto.EstadisticasEventoDTO;
import com.comaticket.dto.TicketRequest;
import com.comaticket.dto.TicketUpdateRequest;
import com.comaticket.model.Ticket;
import com.comaticket.repository.TicketRepository;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final EfsService efsService;
    private final S3Service s3Service;

    public TicketService(TicketRepository ticketRepository, EfsService efsService, S3Service s3Service) {
        this.ticketRepository = ticketRepository;
        this.efsService = efsService;
        this.s3Service = s3Service;
    }

    // 1. Generar Ticket (Guarda en EFS + Sube a S3 + Registra en BD)
    public Ticket generarTicket(TicketRequest request) {
        String ticketId = "TCK-" + System.currentTimeMillis();
        String estadoInicial = "RESERVADO";

        // a) Guardar temporalmente en EFS
        String efsPath = efsService.generarYGuardarEnEfs(
                request.getEventoId(), request.getUsuarioId(), ticketId,
                request.getNombreCliente(), request.getPrecio(), estadoInicial
        );

        // b) Guardar en S3 con la estructura /eventoId/usuarioId/ticketId.pdf
        String s3Key = String.format("%s/%s/%s.pdf", request.getEventoId(), request.getUsuarioId(), ticketId);
        String s3Url; //= s3Service.subirArchivo(s3Key, new File(efsPath));

        try {
            s3Url = s3Service.subirArchivo(s3Key, new File(efsPath));
        } catch (Exception e) {
            System.err.println("Aviso S3 (Modo Local): No se pudo conectar a AWS S3 -> " + e.getMessage());
            s3Url = "https://comaticket-s2.s3.amazonaws.com/" + s3Key;
        }

        // c) Guardar metadatos en Base de Datos
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .eventoId(request.getEventoId())
                .usuarioId(request.getUsuarioId())
                .nombreCliente(request.getNombreCliente())
                .precio(request.getPrecio())
                .estado(estadoInicial)
                .efsPath(efsPath)
                .s3Key(s3Key)
                .s3Url(s3Url)
                .fechaCreacion(LocalDateTime.now())
                .build();

        return ticketRepository.save(ticket);
    }

    // 2. Subir Ticket a S3 manualmente (sincronizar desde EFS)
    public Ticket subirTicketAS3(String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado: " + ticketId));

        File archivoEfs = new File(ticket.getEfsPath());
        if (!archivoEfs.exists()) {
            // Regenera si no existe en EFS
            efsService.generarYGuardarEnEfs(
                    ticket.getEventoId(), ticket.getUsuarioId(), ticket.getId(),
                    ticket.getNombreCliente(), ticket.getPrecio(), ticket.getEstado()
            );
        }
        /** 
        String url = s3Service.subirArchivo(ticket.getS3Key(), archivoEfs);
        ticket.setS3Url(url);
        return ticketRepository.save(ticket);*/
        // Intenta subir a S3 con protección para el entorno local
        try {
            String url = s3Service.subirArchivo(ticket.getS3Key(), archivoEfs);
            ticket.setS3Url(url);
        } catch (Exception e) {
            System.err.println("Aviso S3 (Modo Local en subir-s3): " + e.getMessage());
        }

        return ticketRepository.save(ticket);

    }
    
    /** 
     3. Descargar Ticket desde S3
    public byte[] descargarTicket(String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        return s3Service.descargarArchivo(ticket.getS3Key());
    }
        */
       
    // 3. Descargar Ticket (Desde S3 o fallback a EFS si no hay conexión AWS)
    public byte[] descargarTicket(String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado: " + ticketId));
        try {
            return s3Service.descargarArchivo(ticket.getS3Key());
        } catch (Exception e) {
            System.err.println("Aviso S3 (Modo Local): Leyendo ticket desde EFS local...");
            try {
                return java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(ticket.getEfsPath()));
            } catch (Exception ex) {
                throw new RuntimeException("Error al leer el archivo en EFS: " + ex.getMessage());
            }
        }
    }

    // 4. Modificar Ticket (Actualiza BD, regenera en EFS y sobrescribe en S3)
    public Ticket modificarTicket(String ticketId, TicketUpdateRequest updateRequest) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        ticket.setNombreCliente(updateRequest.getNombreCliente());
        ticket.setPrecio(updateRequest.getPrecio());
        ticket.setEstado(updateRequest.getEstado());

        // Regenerar en EFS con los nuevos datos
        efsService.generarYGuardarEnEfs(
                ticket.getEventoId(), ticket.getUsuarioId(), ticket.getId(),
                ticket.getNombreCliente(), ticket.getPrecio(), ticket.getEstado()
        );

        // Re-subir a S3 (Sobrescribe el archivo existente)
        // s3Service.subirArchivo(ticket.getS3Key(), new File(ticket.getEfsPath()));
        try {
            String s3Url = s3Service.subirArchivo(ticket.getS3Key(), new File(ticket.getEfsPath()));
            ticket.setS3Url(s3Url);
        } catch (Exception e) {
            System.err.println("Aviso S3 (Modo Local en PUT): " + e.getMessage());
        }

        return ticketRepository.save(ticket);
    }

    // 5. Eliminar Ticket (Borra de BD, EFS y AWS S3)
    public void eliminarTicket(String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        efsService.eliminarDeEfs(ticket.getEfsPath());
        s3Service.eliminarArchivo(ticket.getS3Key());
        ticketRepository.delete(ticket);
    }

    // 6. Consultar Historial con Filtros
    public List<Ticket> consultarHistorial(String eventoId, String usuarioId) {
        if (eventoId != null && usuarioId != null) {
            return ticketRepository.findByEventoIdAndUsuarioId(eventoId, usuarioId);
        } else if (eventoId != null) {
            return ticketRepository.findByEventoId(eventoId);
        } else if (usuarioId != null) {
            return ticketRepository.findByUsuarioId(usuarioId);
        }
        return ticketRepository.findAll();
    }

    // 7. Reportar Estadísticas por Evento
    public EstadisticasEventoDTO obtenerEstadisticas(String eventoId) {
        List<Ticket> tickets = ticketRepository.findByEventoId(eventoId);

        long reservas = tickets.stream().filter(t -> "RESERVADO".equalsIgnoreCase(t.getEstado())).count();
        long confirmados = tickets.stream().filter(t -> "CONFIRMADO".equalsIgnoreCase(t.getEstado())).count();
        long cancelados = tickets.stream().filter(t -> "CANCELADO".equalsIgnoreCase(t.getEstado())).count();
        double ingresos = tickets.stream()
                .filter(t -> !"CANCELADO".equalsIgnoreCase(t.getEstado()))
                .mapToDouble(Ticket::getPrecio)
                .sum();

        return EstadisticasEventoDTO.builder()
                .eventoId(eventoId)
                .totalReservas(reservas)
                .totalConfirmados(confirmados)
                .totalCancelados(cancelados)
                .ingresosTotales(ingresos)
                .build();
    }
}