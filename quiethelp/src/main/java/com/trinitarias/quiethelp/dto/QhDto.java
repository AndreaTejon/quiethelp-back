package com.trinitarias.quiethelp.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.trinitarias.quiethelp.entity.QhConversacionEntity;

public class QhDto {
	
	private QhEmisorDto emisor;
	private QhConversacionDto conversacion;
	private String token; 
	
	public QhDto() {}
	
	public QhDto(QhEmisorDto emisor, QhConversacionDto conversacion) {
		this.emisor = emisor;
		this.conversacion = conversacion;
	}

	public QhEmisorDto getEmisor() {
		return emisor;
	}

	public void setEmisor(QhEmisorDto emisor) {
		this.emisor = emisor;
	}
	
	public QhConversacionDto getConversacion() {
		return conversacion;
	}

	public void setConversacion(QhConversacionDto conversacion) {
		this.conversacion = conversacion;
	}
	
	public String getToken() {
	    return token;
	}

	public void setToken(String token) {
	    this.token = token;
	}

	public static QhDto fromEntityToDto(QhConversacionEntity entity) {
		QhDto dto = new QhDto();
		
		// 1. CREAR EMISOR DTO (datos del alumno)
		QhEmisorDto emisorDto = new QhEmisorDto();
		emisorDto.setCurso(entity.getCurso());
		emisorDto.setGrupo(entity.getGrupo());
		emisorDto.setTarjeta(entity.getTarjeta());
		emisorDto.setUrgente(entity.isUrgente());
		
		// 2. CREAR CONVERSACION DTO
		QhConversacionDto conversacionDto = new QhConversacionDto();
		conversacionDto.setId(String.valueOf(entity.getId()));
		conversacionDto.setEstado(entity.getEstado());
		conversacionDto.setRevisorId(entity.getRevisorId());
		conversacionDto.setRevisorNombre(entity.getRevisorNombre());
		conversacionDto.setFechaInicio(entity.getFechaRecibido());
		conversacionDto.setFechaAsignacion(entity.getFechaAsignacion());
		conversacionDto.setFechaResolucion(entity.getFechaResolucion());

		conversacionDto.setUrgente(entity.isUrgente());
		
		// 3. MAPEAR MENSAJES (si existen)
		if (entity.getMensajes() != null && !entity.getMensajes().isEmpty()) {
			List<QhMensajeDto> mensajesDto = entity.getMensajes().stream()
				.map(msg -> {
					QhMensajeDto msgDto = new QhMensajeDto();
					msgDto.setId(String.valueOf(msg.getId()));
					msgDto.setEmisor(msg.getEmisor());
					msgDto.setMensaje(msg.getContenido());
					msgDto.setLeido(msg.isLeido());
					msgDto.setFecha(msg.getFecha());
					return msgDto;
				})
				.collect(Collectors.toList());
			
			conversacionDto.setMensajes(mensajesDto);
		}
		
		// 4. ASIGNAR AMBOS AL DTO
		dto.setEmisor(emisorDto);
		dto.setConversacion(conversacionDto);
		dto.setToken(entity.getToken());

		return dto;
	}
}