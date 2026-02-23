package com.trinitarias.quiethelp.dto;

public class QhMensajeDto {
	private String id;
	
	private String emisor;
	
	private String mensaje;
	
	private boolean leido;
	
	private String fecha;

	public QhMensajeDto() {}
	
	public QhMensajeDto(String id, String emisor, String mensaje, String estado) {
		this.id = id;
		this.emisor = emisor;
		this.mensaje = mensaje;
	}

	public String getMensaje() {
		return mensaje;
	}

	public void setMensaje(String mensaje) {
		this.mensaje = mensaje;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEmisor() {
		return emisor;
	}

	public void setEmisor(String emisor) {
		this.emisor = emisor;
	}

	public boolean isLeido() {
		return leido;
	}

	public void setLeido(boolean leido) {
		this.leido = leido;
	}
	
	public String getFecha() {
		return fecha;
	}
	
	public void setFecha(String fecha) {
		this.fecha = fecha;
	}

}
