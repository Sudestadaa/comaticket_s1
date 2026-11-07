package com.comaticket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.comaticket.model.Ticket;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {
    List<Ticket> findByEventoId(String eventoId);
    List<Ticket> findByUsuarioId(String usuarioId);
    List<Ticket> findByEventoIdAndUsuarioId(String eventoId, String usuarioId);
}