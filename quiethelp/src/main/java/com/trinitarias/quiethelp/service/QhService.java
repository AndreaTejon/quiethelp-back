package com.trinitarias.quiethelp.service;

//Fecha
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//Colecciones
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

//Concurrencia
import java.util.concurrent.CompletableFuture;

//Streams
import java.util.stream.Collectors;

//Spring
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

//HTTP
import org.springframework.http.HttpEntity;      // Para la petición completa
import org.springframework.http.HttpHeaders;    // Para las cabeceras
import org.springframework.http.MediaType;      // Para application/json

// BBDD 
import com.trinitarias.quiethelp.components.SupabaseClient;

//Dtos y Entities
import com.trinitarias.quiethelp.dto.QhDashboardResumenDto;
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
	
	@Autowired
	private RestTemplate restTemplate; //CLiente HTTp
	
	@Value("${n8n.webhook.url}")
	private String n8nUrl;
	
    @Autowired
    private SupabaseClient supabaseClient;
	
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	
	/* Crear converasción -- mensaje de alumno*/
	@Transactional
	public QhDto crearConversacion(QhDto dto) {
		// 1. Guarda en Supabase
		QhConversacionEntity conversacion = QhConversacionEntity.fromDtoToEntity(dto); //Crear conversación
		QhConversacionEntity guardada = conversacionRepository.save(conversacion);
		
		// 2. Se guarda el primer mensaje
		QhMensajeDto primerMensajeDto = dto.getConversacion().getMensajes().get(0);
		
		// 3. Guardar mensaje en BBDD
	    QhMensajeEntity mensaje = QhMensajeEntity.fromDtoToEntity(primerMensajeDto, guardada);
	    QhMensajeEntity mensajeGuardado = mensajeRepository.save(mensaje);
		
		// 4. Enviar a N8N
	    CompletableFuture.runAsync(() -> {
	        try {
	            Map<String, Object> payload = new HashMap<>();
	            payload.put("mensajeId", mensajeGuardado.getId()); // Mensaje a actualizar
	            payload.put("conversacionId", guardada.getId());
	            payload.put("contenidoOriginal", primerMensajeDto.getMensaje()); // Texto a anonimizar
	            
	            //Petición HTTP
	            HttpHeaders headers = new HttpHeaders();
	            headers.setContentType(MediaType.APPLICATION_JSON);
	            
	            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
	            
	          //LLamada a N8
	            restTemplate.postForObject(n8nUrl, request, String.class);
	            
	        } catch (Exception e) {
	            System.err.println("Error al anonimizar: " + e.getMessage());
	        }
	    });

		return QhDto.fromEntityToDto(guardada);
	}
	
	/*Obtener conversacion -> dashboard profesor -filtros incluidos*/
	public List<QhDto> obtenerConversacionesDashboard(String estado, String tarjeta, Boolean urgente) {
		List<QhConversacionEntity> conversaciones = conversacionRepository.filtrarConversaciones(estado, tarjeta, urgente);
		
		return conversaciones.stream().map(conv -> {
				QhDto dto = QhDto.fromEntityToDto(conv);
				//Se manda el primer mensaje
	            if (dto.getConversacion() != null && dto.getConversacion().getMensajes() != null && !dto.getConversacion().getMensajes().isEmpty()) {
	                    // Crear lista con solo el primer mensaje
	                    List<QhMensajeDto> soloPrimerMensaje = new ArrayList<>();
	                    soloPrimerMensaje.add(dto.getConversacion().getMensajes().get(0));
	                    dto.getConversacion().setMensajes(soloPrimerMensaje);
	                } else {
	                    dto.getConversacion().setMensajes(null);
	                }
	                
	                return dto;
				}).collect(Collectors.toList());
	}
	
	/*Conversación completa*/
	public QhDto obtenerConversacionCompleta(Long id) {
		QhConversacionEntity conversacion = conversacionRepository.findById(id).orElseThrow(() -> new RuntimeException("Conversacion no encontrada con id: " + id));
		return QhDto.fromEntityToDto(conversacion);
	}
	
	/*Asignar a un profesor --> revisar*/
	@Transactional
	public QhDto asignarConversacion(Long id, String revisorId, String revisorNombre) {
		QhConversacionEntity conversacion = conversacionRepository.findById(id).orElseThrow(() -> new RuntimeException("Conversacion no encontrada con id: " + id));
		conversacion.setEstado("EN_REVISION");
		conversacion.setRevisorId(revisorId);
		conversacion.setRevisorNombre(revisorNombre);
		conversacion.setFechaAsignacion(LocalDateTime.now().format(formatter));
		
		QhConversacionEntity actualizada = conversacionRepository.save(conversacion);
		return QhDto.fromEntityToDto(actualizada);
	}
	
	/*Responder una conversación*/
	@Transactional
	public QhDto responderConversacion(Long id, String contenido, String revisorId, String revisorNombre) {
		QhConversacionEntity conversacion = conversacionRepository.findById(id).orElseThrow(() -> new RuntimeException("Conversacion no encontrada con id: " + id));
		//Mensaje profesor
		QhMensajeEntity mensaje = new QhMensajeEntity();
		mensaje.setConversacion(conversacion);
		mensaje.setEmisor("profesor");
		mensaje.setContenido(contenido);
		mensaje.setFecha(LocalDateTime.now().format(formatter));
		mensaje.setLeido(false);
		
		mensajeRepository.save(mensaje);
		
		if("PENDIENTE".equals(conversacion.getEstado())) { //Si está "PENDIENTE" pasa a "EN_REVISION"
			conversacion.setEstado("EN_REVISION");
			conversacion.setRevisorId(revisorId);
			conversacion.setRevisorNombre(revisorNombre);
			conversacion.setFechaAsignacion(LocalDateTime.now().format(formatter));
			conversacionRepository.save(conversacion);
		}
		
		return QhDto.fromEntityToDto(conversacion);
	}
	
	
	//Respuesta alumno
	@Transactional
	public QhDto alumnoResponde(Long id, String contenido, String token) {
	    QhConversacionEntity conversacion = conversacionRepository.findById(id).orElseThrow(() -> new RuntimeException("Conversación no encontrada"));
	   
	    // Validar token --> identificación
	    if (!supabaseClient.validarToken(token)) {
	        throw new RuntimeException("Token inválido");
	    }
	    
	    // Crear mensaje del ALUMNO (se anonimizará)
	    QhMensajeEntity mensaje = new QhMensajeEntity();
	    mensaje.setConversacion(conversacion);
	    mensaje.setEmisor("alumno");  // Para el JSON
	    mensaje.setContenido(contenido);
	    mensaje.setFecha(LocalDateTime.now().format(formatter));
	    mensaje.setLeido(false);
	    
	    QhMensajeEntity guardado = mensajeRepository.save(mensaje);
	    
	    // Enviar a N8N
	    CompletableFuture.runAsync(() -> {
	        try {
	            Map<String, Object> payload = new HashMap<>();
	            payload.put("mensajeId", guardado.getId());
	            payload.put("conversacionId", id);
	            payload.put("contenidoOriginal", contenido);
	            
	            HttpHeaders headers = new HttpHeaders();
	            headers.setContentType(MediaType.APPLICATION_JSON);
	            
	            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
	            
	            restTemplate.postForObject(n8nUrl, request, String.class);
	            
	        } catch (Exception e) {
	            System.err.println("Error al anonimizar respuesta: " + e.getMessage());
	        }
	    });
	    
	    return QhDto.fromEntityToDto(conversacion);
	}
	
	/*Cambiar de estado -resuelto,revision-*/
	@Transactional
	public QhDto cambiarEstadoConversacion(Long id, String nuevoEstado) {
		QhConversacionEntity conversacion = conversacionRepository.findById(id).orElseThrow(() -> new RuntimeException("Conversacion no encontrada con id: " + id));
		conversacion.setEstado(nuevoEstado);
		
		if("RESUELTO".equals(nuevoEstado)) {
			conversacion.setFechaResolucion(LocalDateTime.now().format(formatter));
		}
		QhConversacionEntity actualizada = conversacionRepository.save(conversacion);
		return QhDto.fromEntityToDto(actualizada);
	}
	
	/*Marcar mensaje leído*/
	@Transactional
	public void marcarMensajesComoLeidos(Long conversacionId, String emisor) {
		if("profesor".equals(emisor)) {
			mensajeRepository.marcarMensajesProfesorComoLeidos(conversacionId);
		}
		
		if("alumno".equals(emisor)) {
			mensajeRepository.marcarMensajesAlumnoComoLeidos(conversacionId);
		}
	}
	
	/*Contadores Dashboard*/
	public QhDashboardResumenDto obtenerResumenDashboard () {
		QhDashboardResumenDto resumen = new QhDashboardResumenDto();
	    resumen.setPendientes(conversacionRepository.countByEstado("PENDIENTE"));
	    resumen.setEnRevision(conversacionRepository.countByEstado("EN_REVISION"));
	    resumen.setResueltos(conversacionRepository.countByEstado("RESUELTO"));
	    resumen.setUrgentes(conversacionRepository.countByUrgenteTrue());    
	    return resumen;
	}
	
}
