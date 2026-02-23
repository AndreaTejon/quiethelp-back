package com.trinitarias.quiethelp.entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.trinitarias.quiethelp.dto.QhDto;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "conversaciones")
public class QhConversacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Datos del emisor (alumno)
    private String curso;
    private String grupo;
    private String tarjeta;        // Bullying, Académico, Emocional
    private boolean urgente;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    // Primer mensaje (el texto inicial)
    @Column(length = 2000)
    private String mensajeInicial;

    // Estado de la conversación
    private String estado;           // PENDIENTE, EN_REVISION, RESUELTO

    // Datos del revisor (profesor)
    private String revisorId;        // UUID de Supabase
    private String revisorNombre;

    // Fechas de gestión
    private String fechaRecibido;    // La pone el backend al crear
    private String fechaAsignacion;  // Cuando profesor toma el caso
    private String fechaResolucion;  // Cuando se resuelve

    // Relación uno a muchos con mensajes
    @OneToMany(mappedBy = "conversacion", cascade = CascadeType.ALL)
    private List<QhMensajeEntity> mensajes;

    // Constructores
    public QhConversacionEntity() {}

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCurso() {
        return curso;
    }

    public void setCurso(String curso) {
        this.curso = curso;
    }

    public String getGrupo() {
        return grupo;
    }

    public void setGrupo(String grupo) {
        this.grupo = grupo;
    }

    public String getTarjeta() {
        return tarjeta;
    }

    public void setTarjeta(String tarjeta) {
        this.tarjeta = tarjeta;
    }

    public boolean isUrgente() {
        return urgente;
    }

    public void setUrgente(boolean urgente) {
        this.urgente = urgente;
    }

    public String getMensajeInicial() {
        return mensajeInicial;
    }

    public void setMensajeInicial(String mensajeInicial) {
        this.mensajeInicial = mensajeInicial;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getRevisorId() {
        return revisorId;
    }

    public void setRevisorId(String revisorId) {
        this.revisorId = revisorId;
    }

    public String getRevisorNombre() {
        return revisorNombre;
    }

    public void setRevisorNombre(String revisorNombre) {
        this.revisorNombre = revisorNombre;
    }

    public String getFechaRecibido() {
        return fechaRecibido;
    }

    public void setFechaRecibido(String fechaRecibido) {
        this.fechaRecibido = fechaRecibido;
    }

    public String getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(String fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public String getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(String fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }

    public List<QhMensajeEntity> getMensajes() {
        return mensajes;
    }

    public void setMensajes(List<QhMensajeEntity> mensajes) {
        this.mensajes = mensajes;
    }
    
    public static QhConversacionEntity fromDtoToEntity(QhDto dto) {
        QhConversacionEntity entity = new QhConversacionEntity();
        
        // Datos del emisor (primer mensaje)
        entity.setCurso(dto.getEmisor().getCurso());
        entity.setGrupo(dto.getEmisor().getGrupo());
        entity.setTarjeta(dto.getEmisor().getTarjeta());
        entity.setUrgente(dto.getEmisor().isUrgente());
        
        // Primer mensaje (el texto)
        if (dto.getConversacion() != null && 
            dto.getConversacion().getMensajes() != null && 
            !dto.getConversacion().getMensajes().isEmpty()) {
            entity.setMensajeInicial(dto.getConversacion().getMensajes().get(0).getMensaje());
        }
        
        // Estado inicial (lo pone el backend, no viene de Flutter)
        entity.setEstado("PENDIENTE");
        entity.setFechaRecibido(LocalDateTime.now().format(formatter));
        
        return entity;
    }
}