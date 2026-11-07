package com.comaticket.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comaticket.dto.EstadisticasEventoDTO;
import com.comaticket.dto.TicketRequest;
import com.comaticket.dto.TicketUpdateRequest;
import com.comaticket.model.Ticket;
import com.comaticket.service.TicketService;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // 1. Generar ticket (Creación en EFS + S3)
    @PostMapping("/generar")
    public ResponseEntity<Ticket> generarTicket(@RequestBody TicketRequest request) {
        Ticket nuevoTicket = ticketService.generarTicket(request);
        return ResponseEntity.ok(nuevoTicket);
    }

    // 2. Subir / Sincronizar ticket a AWS S3
    @PostMapping("/{id}/subir-s3")
    public ResponseEntity<Ticket> subirS3(@PathVariable String id) {
        Ticket ticketActualizado = ticketService.subirTicketAS3(id);
        return ResponseEntity.ok(ticketActualizado);
    }

    // 3. Descargar ticket desde S3 por ID
    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargarTicket(@PathVariable String id) {
        byte[] pdfContent = ticketService.descargarTicket(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + id + ".pdf\"")
                .body(pdfContent);
    }

    // 4. Modificar detalles del ticket
    @PutMapping("/{id}")
    public ResponseEntity<Ticket> modificarTicket(@PathVariable String id, @RequestBody TicketUpdateRequest request) {
        Ticket ticketModificado = ticketService.modificarTicket(id, request);
        return ResponseEntity.ok(ticketModificado);
    }

    // 5. Eliminar ticket
    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarTicket(@PathVariable String id) {
        ticketService.eliminarTicket(id);
        return ResponseEntity.ok("Ticket " + id + " eliminado con éxito de EFS, S3 y la base de datos.");
    }

    // 6. Consultar historial de tickets (con filtros opcionales de eventoId y usuarioId)
    @GetMapping("/historial")
    public ResponseEntity<List<Ticket>> consultarHistorial(
            @RequestParam(required = false) String eventoId,
            @RequestParam(required = false) String usuarioId) {
        List<Ticket> historial = ticketService.consultarHistorial(eventoId, usuarioId);
        return ResponseEntity.ok(historial);
    }

    // 7. Reportar estadísticas por evento
    @GetMapping("/estadisticas/{eventoId}")
    public ResponseEntity<EstadisticasEventoDTO> obtenerEstadisticas(@PathVariable String eventoId) {
        EstadisticasEventoDTO stats = ticketService.obtenerEstadisticas(eventoId);
        return ResponseEntity.ok(stats);
    }
}