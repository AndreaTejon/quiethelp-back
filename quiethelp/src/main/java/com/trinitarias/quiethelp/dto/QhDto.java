package com.trinitarias.quiethelp.dto;

import com.trinitarias.quiethelp.entity.QhEntity;

public class QhDto {
	
	private QhMensajeDto mensaje;
	private QhEmisorDto emisor;
	
	public QhDto() {}
	
	public QhDto(QhEmisorDto emisor, QhMensajeDto mensaje) {
		this.emisor = emisor;
		this.mensaje = mensaje;
	}

	public QhMensajeDto getMensaje() {
		return mensaje;
	}

	public void setMensaje(QhMensajeDto mensaje) {
		this.mensaje = mensaje;
	}

	public QhEmisorDto getEmisor() {
		return emisor;
	}

	public void setEmisor(QhEmisorDto emisor) {
		this.emisor = emisor;
	}
	
	public QhDto fromEntityToDto (QhEntity et) {
		QhDto dto = new QhDto();
		emisor.setCurso(et.getCurso());
		emisor.setGrupo(et.getGrupo());
		emisor.setTarjeta(et.getTarjeta());
		mensaje.setMensaje(et.getMensaje());
		return dto;
	}
}
