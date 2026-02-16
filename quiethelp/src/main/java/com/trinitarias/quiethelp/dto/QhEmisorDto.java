package com.trinitarias.quiethelp.dto;

public class QhEmisorDto {
	private String curso, grupo, tarjeta, fecha;
	private boolean urgente;
	
	public QhEmisorDto() {}

	public QhEmisorDto(String curso, String grupo, String tarjeta, String fecha) {
		this.curso = curso;
		this.grupo = grupo;
		this.tarjeta = tarjeta;
		this.fecha = fecha;
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
	
	public String getFecha() {
		return fecha;
	}
	
	public void setFecha(String fecha) {
		this.fecha = fecha;
	}
	
	public boolean isUrgente() {
		return urgente;
	}
	
	public void setUrgente(boolean urgente) {
		this.urgente = urgente;
	}

}
