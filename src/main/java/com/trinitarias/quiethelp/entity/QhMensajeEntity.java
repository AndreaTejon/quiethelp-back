package com.trinitarias.quiethelp.entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

import com.trinitarias.quiethelp.dto.QhMensajeDto;

import jakarta.persistence.*;

@Entity
@Table(name = "mensajes")
public class QhMensajeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación muchos a uno con conversación
    @ManyToOne
    @JoinColumn(name = "conversacion_id", nullable = false)
    private QhConversacionEntity conversacion;

    private String emisor;        // "alumno" o "profesor"

    @Column(length = 2000)
    private String contenido;

    private String fecha;

    private boolean leido;
    
 // NUEVOS CAMPOS PARA BLOCKCHAIN
    @Column(length = 64)
    private String hashActual;      // Hash SHA-256 de este mensaje
    
    @Column(length = 64)
    private String hashAnterior;    // Hash del mensaje anterior en la conversación
    

    // Constructores
    public QhMensajeEntity() {}

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public QhConversacionEntity getConversacion() {
        return conversacion;
    }

    public void setConversacion(QhConversacionEntity conversacion) {
        this.conversacion = conversacion;
    }

    public String getEmisor() {
        return emisor;
    }

    public void setEmisor(String emisor) {
        this.emisor = emisor;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
            this.fecha = fecha;
    }

    public boolean isLeido() {
        return leido;
    }

    public void setLeido(boolean leido) {
        this.leido = leido;
    }
    
    public String getHashActual() {
        return hashActual;
    }

    public void setHashActual(String hashActual) {
        this.hashActual = hashActual;
    }

    public String getHashAnterior() {
        return hashAnterior;
    }

    public void setHashAnterior(String hashAnterior) {
        this.hashAnterior = hashAnterior;
    }
    
    public static QhMensajeEntity fromDtoToEntity(QhMensajeDto dto, QhConversacionEntity conversacion) {
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        QhMensajeEntity entity = new QhMensajeEntity();
        entity.setConversacion(conversacion);
        entity.setEmisor(dto.getEmisor() != null ? dto.getEmisor() : "alumno");
        entity.setContenido(dto.getMensaje());
        entity.setFecha(LocalDateTime.now().format(formatter));
        entity.setLeido(dto.isLeido());
        
        return entity;
    }
    
    // MÉTODO PARA CALCULAR EL HASH
    public String calcularHash() {
        // Combinar: id + contenido + fecha + hashAnterior
        String datos = this.id + this.contenido + this.fecha + 
                       (this.hashAnterior != null ? this.hashAnterior : "0");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(datos.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al calcular hash", e);
        }
    }
    
    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}