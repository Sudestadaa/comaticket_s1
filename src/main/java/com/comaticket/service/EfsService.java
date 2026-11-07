package com.comaticket.service;

import java.io.File;
import java.io.FileOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class EfsService {

    @Value("${aws.efs.mount-path:./efs-data}")
    private String efsMountPath;

    // Genera el archivo PDF físico en la carpeta montada de EFS
    public String generarYGuardarEnEfs(String eventoId, String usuarioId, String ticketId, String cliente, Double precio, String estado) {
        try {
            // Estructura interna de carpetas en EFS
            File carpeta = new File(efsMountPath + "/" + eventoId + "/" + usuarioId);
            if (!carpeta.exists()) {
                carpeta.mkdirs();
            }

            File archivoPdf = new File(carpeta, ticketId + ".pdf");

            // Generación simple del documento PDF
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(archivoPdf));
            document.open();
            document.add(new Paragraph("=========================================="));
            document.add(new Paragraph("         COMATICKET - TICKET OFICIAL      "));
            document.add(new Paragraph("=========================================="));
            document.add(new Paragraph("Ticket ID: " + ticketId));
            document.add(new Paragraph("Evento ID: " + eventoId));
            document.add(new Paragraph("Usuario ID: " + usuarioId));
            document.add(new Paragraph("Cliente: " + cliente));
            document.add(new Paragraph("Precio: $" + precio));
            document.add(new Paragraph("Estado: " + estado));
            document.add(new Paragraph("=========================================="));
            document.close();

            return archivoPdf.getAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar ticket temporal en EFS: " + e.getMessage(), e);
        }
    }

    // Elimina el archivo físico de EFS
    public void eliminarDeEfs(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }
}