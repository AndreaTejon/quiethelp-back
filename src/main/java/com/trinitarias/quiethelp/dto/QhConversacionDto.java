package com.trinitarias.quiethelp.dto;

import java.util.List;

public class QhConversacionDto {

	private String id;

	private String estado; // pendiente, revision, resuelto

	private String revisorId;

	private String revisorNombre;

	private String fechaInicio;

	private String fechaAsignacion;

	private String fechaResolucion;

	private boolean urgente;
	
	private boolean cadenaVerificada;

	private List<QhMensajeDto> mensajes; // Los mensajes

	public QhConversacionDto() {
	}

	public QhConversacionDto(
			String id,
			String estado,
			String revisorId,
			String revisorNombre,
			String fechaInicio,
			String fechaAsignacion,
			String fechaResolucion) {

		this.id = id;
		this.estado = estado;
		this.revisorId = revisorId;
		this.revisorNombre = revisorNombre;
		this.fechaInicio = fechaInicio;
		this.fechaAsignacion = fechaAsignacion;
		this.fechaResolucion = fechaResolucion;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public String getFechaInicio() {
		return fechaInicio;
	}

	public void setFechaInicio(String fechaInicio) {
		this.fechaInicio = fechaInicio;
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

	public boolean isUrgente() {
		return urgente;
	}

	public void setUrgente(boolean urgente) {
		this.urgente = urgente;
	}

	public List<QhMensajeDto> getMensajes() {
		return mensajes;
	}

	public void setMensajes(List<QhMensajeDto> mensajes) {
		this.mensajes = mensajes;
	}
	
	public boolean isCadenaVerificada() {
	    return cadenaVerificada;
	}

	public void setCadenaVerificada(boolean cadenaVerificada) {
	    this.cadenaVerificada = cadenaVerificada;
	}
}