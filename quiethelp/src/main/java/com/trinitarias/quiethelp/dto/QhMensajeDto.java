package com.trinitarias.quiethelp.dto;

public class QhMensajeDto {
	private String mensaje;
	
	public QhMensajeDto() {}
	
	public QhMensajeDto(String mensaje) {
		this.mensaje = mensaje;
	}

	public String getMensaje() {
		return mensaje;
	}

	public void setMensaje(String mensaje) {
		this.mensaje = mensaje;
	}
	

}
