package com.trinitarias.quiethelp.service;

//Fecha
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//Colecciones
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

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
	private RestTemplate restTemplate;
	
	@Value("${n8n.webhook.url}")
	private String n8nUrl;
	
    @Autowired
    private SupabaseClient supabaseClient;
	
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	
	/* Crear conversación -- mensaje de alumno*/
	@Transactional
	public QhDto crearConversacion(QhDto dto) {
	    // 1. Obtener mensaje PRIMERO
	    QhMensajeDto primerMensajeDto = dto.getConversacion().getMensajes().get(0);
	    String textoMensaje = primerMensajeDto.getMensaje();
	    
	    System.out.println("📨 Mensaje recibido: '" + textoMensaje + "'");
	    
	    // 2. Si NO es validación, guardar TODO normal
	    QhConversacionEntity conversacion = QhConversacionEntity.fromDtoToEntity(dto);
	    QhConversacionEntity guardada = conversacionRepository.save(conversacion);
	    
	    QhMensajeEntity mensaje = QhMensajeEntity.fromDtoToEntity(primerMensajeDto, guardada);
	    QhMensajeEntity mensajeGuardado = mensajeRepository.save(mensaje);
	    
	    // 3. Enviar a N8N
	    CompletableFuture.runAsync(() -> {
	        try {
	            sendN8NMethod(textoMensaje, guardada, mensajeGuardado);
	            
	        } catch (Exception e) {
	            System.err.println("Error al anonimizar: " + e.getMessage());
	        }
	    });
	    
	    return QhDto.fromEntityToDto(guardada);
	}

	private void sendN8NMethod(String textoMensaje, QhConversacionEntity guardada, QhMensajeEntity mensajeGuardado) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("mensajeId", mensajeGuardado.getId());
		payload.put("conversacionId", guardada.getId());
		payload.put("contenidoOriginal", textoMensaje);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
		
		restTemplate.postForObject(n8nUrl, request, String.class);
	}
	
	/* Obtener conversaciones por token del alumno (solo PENDIENTE y EN_REVISION) */
	public List<QhConversacionEntity> obtenerConversacionesPorToken(String token) {
	    // Buscar conversaciones por token (asumiendo que guardas el token en la tabla)
	    // Y filtrar solo las que NO están RESUELTAS
	    return conversacionRepository.findByTokenAndEstadoNot(token, "RESUELTO");
	}
	
	
	/*Obtener conversacion -> dashboard profesor -filtros incluidos*/
	public List<QhDto> obtenerConversacionesDashboard(String estado, String tarjeta, Boolean urgente) {
	    List<QhConversacionEntity> conversaciones =
	            conversacionRepository.filtrarConversaciones(estado, tarjeta, urgente);

	    return conversaciones.stream().map(conv -> {
	        QhDto dto = QhDto.fromEntityToDto(conv);

	        if (dto.getConversacion() == null) {
	            return dto;
	        }

	        if (dto.getConversacion().getMensajes() == null ||
	                dto.getConversacion().getMensajes().isEmpty()) {
	            dto.getConversacion().setMensajes(null);
	            return dto;
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
	    mensaje.setLeido(false);  // IMPORTANTE: el profesor no lo ha leído aún
	    
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
	
	/* Marcar mensajes como leídos (método legacy existente) */
	@Transactional
	public void marcarMensajesComoLeidos(Long conversacionId, String emisor) {
		if("profesor".equals(emisor)) {
			mensajeRepository.marcarMensajesProfesorComoLeidos(conversacionId);
		}
		
		if("alumno".equals(emisor)) {
			mensajeRepository.marcarMensajesAlumnoComoLeidos(conversacionId);
		}
	}
	
	/* NUEVO MÉTODO: Marcar mensajes del ALUMNO como leídos por el PROFESOR */
	@Transactional
	public int marcarMensajesAlumnoComoLeidos(Long conversacionId, Long revisorId) {
	    // 1. Verificar que la conversación existe
	    QhConversacionEntity conversacion = conversacionRepository.findById(conversacionId)
	        .orElseThrow(() -> new RuntimeException("Conversación no encontrada con id: " + conversacionId));
	    
	    // 2. Verificar que el revisor está asignado a esta conversación
	    if (conversacion.getRevisorId() == null || !conversacion.getRevisorId().equals(String.valueOf(revisorId))) {
	        throw new RuntimeException("No autorizado: este revisor no está asignado a esta conversación");
	    }
	    
	    // 3. Marcar todos los mensajes del alumno como leídos
	    int mensajesActualizados = mensajeRepository.marcarMensajesAlumnoComoLeidos(conversacionId);
	    
	    System.out.println("✅ Marcados " + mensajesActualizados + " mensajes del alumno como leídos en conversación " + conversacionId);
	    
	    return mensajesActualizados;
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
	
	/* Obtener conversaciones para el dashboard de un profesor específico */
	public List<QhDto> obtenerConversacionesDashboardPorRevisor(
	        String estado, String tarjeta, Boolean urgente, String revisorId) {
	    
	    // Obtener conversaciones PENDIENTES (sin asignar) + las asignadas a este profesor
	    List<QhConversacionEntity> conversaciones = 
	        conversacionRepository.findPendientesYAsignadasARevisor(estado, tarjeta, urgente, revisorId);
	    
	    return conversaciones.stream()
	        .map(QhDto::fromEntityToDto)
	        .collect(Collectors.toList());
	}
	
	//PARA MENSAJE ANONIMO
	
		@Transactional
		public QhDto actualizarMensajeAnonimizado(Long mensajeId, String contenidoAnonimizado) {
		    QhMensajeEntity mensaje = mensajeRepository.findById(mensajeId)
		            .orElseThrow(() -> new RuntimeException("Mensaje no encontrado con id: " + mensajeId));

		    mensaje.setContenido(contenidoAnonimizado);

		    QhMensajeEntity actualizado = mensajeRepository.save(mensaje);

		    return QhDto.fromEntityToDto(actualizado.getConversacion());
		}

}
