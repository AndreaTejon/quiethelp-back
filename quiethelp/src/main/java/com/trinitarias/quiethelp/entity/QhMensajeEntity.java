package com.trinitarias.quiethelp.entity;

import java.time.LocalDateTime;

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
    private String contenido;z

    private String fecha;

    private boolean leido;
    

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
    
    public static QhMensajeEntity fromDtoToEntity(QhMensajeDto dto, QhConversacionEntity conversacion) {
        QhMensajeEntity entity = new QhMensajeEntity();
        
        entity.setConversacion(conversacion);
        entity.setEmisor(dto.getEmisor() != null ? dto.getEmisor() : "alumno");
        entity.setContenido(dto.getMensaje());
        entity.setFecha(dto.getFecha() != null ? dto.getFecha() : LocalDateTime.now().toString());
        entity.setLeido(dto.isLeido());
        
        return entity;
    }
}