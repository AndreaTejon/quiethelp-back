package com.trinitarias.quiethelp.dto;

public class QhEmisorDto {
	private String curso, grupo, tarjeta;
	private boolean urgente;
	
	public QhEmisorDto() {}

	public QhEmisorDto(String curso, String grupo, String tarjeta) {
		this.curso = curso;
		this.grupo = grupo;
		this.tarjeta = tarjeta;
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

}
