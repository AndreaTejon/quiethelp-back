/*package com.trinitarias.quiethelp.entity;

import com.trinitarias.quiethelp.dto.QhDto;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="quiethelp")
public class QhEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	private String curso;
	
	private String grupo;
	
	private String tarjeta;
	
	private String mensaje;
	
	private String fecha;
	
	private boolean urgente;
	
	private String estado;           // "Pendiente", "En revisión", "Resuelto"
	
	private String revisorId;        // ID del comité en Supabase (UUID)
	
	private String revisorNombre;    // Nombre del ayudante asignado
	
	private String fechaRecibido;  // Fecha real del backend
	
	private String fechaRevision;  // Cuándo se asignó/revisó
	
	public QhEntity () {}
	
	public QhEntity(long id, String curso, String grupo, String tarjeta, String mensaje, String fecha) {
		super();
		this.id = id;
		this.curso = curso;
		this.grupo = curso;
		this.tarjeta = tarjeta;
		this.mensaje = mensaje;
		this.fecha = fecha;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
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

	public String getMensaje() {
		return mensaje;
	}

	public void setMensaje(String mensaje) {
		this.mensaje = mensaje;
	}
	
	public String getFecha() {
		return fecha;
	}
	
	public void setFecha(String fecha) {
		this.fecha = fecha;
	}
	
	public boolean isUrgente() {
		return urgente;
	}
	
	public void setUrgente() {
		urgente = true;
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

	public String getFechaRevision() {
		return fechaRevision;
	}

	public void setFechaRevision(String fechaRevision) {
		this.fechaRevision = fechaRevision;
	}
/*
	public static QhEntity fromDtoToEntity(QhDto dto) {
		QhEntity et = new QhEntity();
		et.setCurso(dto.getEmisor().getCurso());
		et.setGrupo(dto.getEmisor().getGrupo());
		et.setTarjeta(dto.getEmisor().getTarjeta());
		et.setMensaje(dto.getMensaje().getMensaje());
		et.setFecha(dto.getEmisor().getFecha());
		et.setEstado(dto.getMensaje().getEstado());
		et.setRevisorId(dto.getMensaje().getRevisorId());
		et.setRevisorNombre(dto.getMensaje().getRevisorNombre());
		et.setFechaRecibido(dto.getMensaje().getFechaRecibido());
		if(dto.getEmisor().isUrgente()) {
			et.setUrgente();
		}
		return et;
	}
}*/
 