package com.trinitarias.quiethelp.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trinitarias.quiethelp.dto.QhDto;
import com.trinitarias.quiethelp.dto.QhMensajeDto;
import com.trinitarias.quiethelp.entity.QhConversacionEntity;
import com.trinitarias.quiethelp.entity.QhMensajeEntity;
import com.trinitarias.quiethelp.repository.QhConversacionRepository;
import com.trinitarias.quiethelp.repository.QhMensajeRepository;

@Service
public class QhService {

	@Autowired
	private QhConversacionRepository conversacionRepository;
	
	@Autowired
	private QhMensajeRepository mensajeRepository;
	
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	
	/* Crear converasción -- mensaje de alumno*/
	@Transactional
	public QhDto crearConverascion(QhDto dto) {
		QhConversacionEntity conversacion = QhConversacionEntity.fromDtoToEntity(dto); //Crear conversación
		
		QhConversacionEntity conversacionGuardada = conversacionRepository.save(conversacion);
		
		if(dto.getConversacion() != null && dto.getConversacion().getMensajes() != null && !dto.getConversacion().getMensajes().isEmpty()) {
		
			QhMensajeDto primerMensajeDto = dto.getConversacion().getMensajes().get(0);
			QhMensajeEntity mensaje = QhMensajeEntity.fromDtoToEntity(primerMensajeDto, conversacionGuardada);
			mensajeRepository.save(mensaje);
		}
		return QhDto.fromEntityToDto(conversacionGuardada);
	}
	
	/*Obtener conversacion -> dashboard profesor -filtros incluidos*/
	public List<QhDto> obtenerConversacionesDashboard(String estado, String tarjeta, Boolean urgente) {
		List<QhConversacionEntity> conversaciones = conversacionRepository.filtrarConversaciones(estado, tarjeta, urgente);
		
		return conversaciones.stream().map(conv -> {
				QhDto dto = QhDto.fromEntityToDto(conv);
				//Se manda el primer mensaje
				if (dto.getConversacion() != null) {
					dto.getConversacion().setMensajes(null); //No se envían todos los mensjaes
				}
				return dto;
				}).collect(Collectors.toList());
	}
	
	/*Conversación completa*/
	public QhDto obtenerConversacionCompleta(Long id) {
		QhConversacionEntity conversacion = conversacionRepository.findById(id).orElseThrow(RuntimeException::new);
		return QhDto.fromEntityToDto(conversacion);
	}
	
	/*Asignar a un profesor --> revisar*/
	@Transactional
	public QhDto asignarConversacion(Long id, String revisorId, String revisorNombre) {
		QhConversacionEntity conversacion = conversacionRepository.findById(id).orElseThrow(RuntimeException::new);
		conversacion.setEstado("EN_REVISION");
		conversacion.setRevisorId(revisorId);
		conversacion.setRevisorNombre(revisorNombre);
		conversacion.setFechaAsignacion(LocalDateTime.now().format(formatter));
		
		QhConversacionEntity actualizada = conversacionRepository.save(conversacion);
		return QhDto.fromEntityToDto(actualizada);
	}
	
	
}
