package com.trinitarias.quiethelp.controller;

import com.trinitarias.quiethelp.components.QhValidator;
import com.trinitarias.quiethelp.components.SupabaseClient;
import com.trinitarias.quiethelp.dto.QhDashboardResumenDto;
import com.trinitarias.quiethelp.dto.QhDto;
import com.trinitarias.quiethelp.dto.QhDtoConToken;
import com.trinitarias.quiethelp.service.QhService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversaciones")
@CrossOrigin(origins = "*") // Permite conexiones desde Flutter
public class QhController {

    @Autowired
    private QhService qhService; // Servicio con la lógica de negocio

    @Autowired
    private QhValidator qhValidator; // Validador personalizado

    @Autowired
    private SupabaseClient supabaseClient; // Cliente para validar tokens con Supabase

    /* ============================================
       ENDPOINT PARA ALUMNOS: Crear conversación (primer mensaje)
       POST /api/conversaciones
       Recibe: QhDtoConToken (con token + emisor + conversacion)
       ============================================ */
    @PostMapping
    public ResponseEntity<?> crearConversacion(@RequestBody QhDtoConToken dtoConToken) {

        // 1. Validar que el token no sea nulo ni vacío
        if (dtoConToken.getToken() == null || dtoConToken.getToken().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "El token es obligatorio"));
        }

        // 2. Validar el token contra Supabase (que exista en la BD)
        boolean tokenValido = supabaseClient.validarToken(dtoConToken.getToken());

        if (!tokenValido) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token inválido o no autorizado"));
        }

        // 3. Extraer los datos del mensaje para validarlos
        QhDto dto = new QhDto();
        dto.setEmisor(dtoConToken.getEmisor());
        dto.setConversacion(dtoConToken.getConversacion());

        // 4. Validar los datos del mensaje (categoría, mensaje no vacío, etc)
        List<String> errores = qhValidator.validarQhDto(dto);

        if (!errores.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("errores", errores);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // 5. Detectar si es mensaje de validación
        String textoMensaje = dto.getConversacion().getMensajes().get(0).getMensaje();
        System.out.println("🔍 CONTROLLER - mensaje: '" + textoMensaje + "'");
        boolean esMensajeValidacion = textoMensaje.toLowerCase().trim().equals("validación") || 
                textoMensaje.toLowerCase().trim().equals("validacion");

        if (esMensajeValidacion) {
            // No guardamos nada, pero devolvemos 201 CREATED para que el frontend navegue
            System.out.println("📌 Mensaje de validación detectado - No se guarda en BD");
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                        "valido", true,
                        "mensaje", "Token válido",
                        "validacion", true
                    ));
        }

        // 6. Crear la conversación en la BD (solo para mensajes reales)
        try {
            QhDto nuevaConversacion = qhService.crearConversacion(dto);
            // 201 CREATED: recurso creado exitosamente
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevaConversacion);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("mensaje", "Error al crear la conversación: " + e.getMessage());
            // 500 INTERNAL SERVER ERROR: error inesperado en el servidor
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /* ============================================
       ENDPOINT PARA PROFESOR: Obtener dashboard con filtros
       GET /api/conversaciones/dashboard?estado=PENDIENTE&tarjeta=Bullying&urgente=true
       ============================================ */
    @GetMapping("/dashboard")
    public ResponseEntity<List<QhDto>> obtenerDashboard(@RequestParam(required = false) String estado, @RequestParam(required = false) String tarjeta,
            @RequestParam(required = false) Boolean urgente) {
        List<QhDto> conversaciones = qhService.obtenerConversacionesDashboard(estado, tarjeta, urgente);
        // 200 OK: devuelve la lista (puede estar vacía)
        return ResponseEntity.status(HttpStatus.OK).body(conversaciones);
    }

    /* ============================================
       ENDPOINT PARA PROFESOR: Ver conversación completa (con todos los mensajes)
       GET /api/conversaciones/{id}
       ============================================ */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerConversacion(@PathVariable Long id) {
        try {
            QhDto conversacion = qhService.obtenerConversacionCompleta(id);
            // 200 OK: conversación encontrada
            return ResponseEntity.status(HttpStatus.OK).body(conversacion);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("mensaje", e.getMessage());
            // 404 NOT FOUND: no existe la conversación
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /* ============================================
       ENDPOINT PARA PROFESOR: Asignarse una conversación (tomar el caso)
       PATCH /api/conversaciones/{id}/asignar?revisorId=123&revisorNombre=María
       ============================================ */
    @PatchMapping("/{id}/asignar")
    public ResponseEntity<?> asignarConversacion(@PathVariable Long id, @RequestParam String revisorId, @RequestParam String revisorNombre) {
        try {
            QhDto conversacion = qhService.asignarConversacion(id, revisorId, revisorNombre);
            // 200 OK: asignación exitosa
            return ResponseEntity.status(HttpStatus.OK).body(conversacion);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("mensaje", e.getMessage());
            // 404 NOT FOUND: la conversación no existe
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /* ============================================
       ENDPOINT PARA PROFESOR: Responder a una conversación
       POST /api/conversaciones/{id}/responder
       Body: { "contenido": "mensaje", "revisorId": "123", "revisorNombre": "María" }
       ============================================ */
    @PostMapping("/{id}/responder")
    public ResponseEntity<?> responderConversacion(@PathVariable Long id, @RequestBody Map<String, String> body) {
        // Validar que el contenido no esté vacío
        if (!body.containsKey("contenido") || body.get("contenido") == null || body.get("contenido").trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "El contenido es obligatorio"));
        }

        // Validar que el ID del revisor esté presente
        if (!body.containsKey("revisorId") || body.get("revisorId") == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "El ID del revisor es obligatorio"));
        }

        // Validar que el nombre del revisor esté presente
        if (!body.containsKey("revisorNombre") || body.get("revisorNombre") == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "El nombre del revisor es obligatorio"));
        }

        try {
            String contenido = body.get("contenido");
            String revisorId = body.get("revisorId");
            String revisorNombre = body.get("revisorNombre");

            QhDto conversacion = qhService.responderConversacion(id, contenido, revisorId, revisorNombre);
            // 200 OK: respuesta enviada
            return ResponseEntity.status(HttpStatus.OK).body(conversacion);
        } catch (RuntimeException e) {
            // 404 NOT FOUND: la conversación no existe
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /* ============================================
       ENDPOINT PARA PROFESOR: Cambiar estado de la conversación
       PATCH /api/conversaciones/{id}/estado?nuevoEstado=RESUELTO
       ============================================ */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestParam String nuevoEstado) {
        // Validar que el estado sea uno de los permitidos
        List<String> estadosValidos = List.of("PENDIENTE", "EN_REVISION", "RESUELTO");
        if (!estadosValidos.contains(nuevoEstado)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of("error", "Estado no válido. Debe ser: PENDIENTE, EN_REVISION, RESUELTO")
            );
        }

        try {
            QhDto conversacion = qhService.cambiarEstadoConversacion(id, nuevoEstado);
            // 200 OK: estado actualizado
            return ResponseEntity.status(HttpStatus.OK).body(conversacion);
        } catch (RuntimeException e) {
            // 404 NOT FOUND: conversación no existe
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /* ============================================
       ENDPOINT PARA PROFESOR: Obtener contadores del dashboard
       GET /api/conversaciones/resumen
       Devuelve: { pendientes: 5, enRevision: 2, resueltos: 10, urgentes: 1 }
       ============================================ */
    @GetMapping("/resumen")
    public ResponseEntity<QhDashboardResumenDto> obtenerResumen() {
        QhDashboardResumenDto resumen = qhService.obtenerResumenDashboard();
        // 200 OK: resumen de contadores
        return ResponseEntity.status(HttpStatus.OK).body(resumen);
    }

    /* ============================================
       ENDPOINT PARA MARCAR MENSAJES COMO LEÍDOS
       POST /api/conversaciones/{id}/leidos?emisor=profesor
       ============================================ */
    @PostMapping("/{id}/leidos")
    public ResponseEntity<?> marcarMensajesComoLeidos(@PathVariable Long id, @RequestParam String emisor) {
        // Validar que el emisor sea "profesor" o "alumno"
        if (!"profesor".equals(emisor) && !"alumno".equals(emisor)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Emisor no válido. Debe ser 'profesor' o 'alumno'")
            );
        }

        try {
            qhService.marcarMensajesComoLeidos(id, emisor);
            // 200 OK: operación exitosa
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("mensaje", "Mensajes marcados como leídos"));
        } catch (Exception e) {
            // 500 INTERNAL SERVER ERROR: error inesperado
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    /* ============================================
       ENDPOINT PARA ALUMNO: Ver su conversación (cuando el profesor responde)
       GET /api/conversaciones/alumno/{id}?token=QUIET-1234
       ============================================ */
    @GetMapping("/alumno/{id}")
    public ResponseEntity<?> obtenerConversacionAlumno(@PathVariable Long id, @RequestParam String token) {
        // 1. Validar el token con Supabase
        boolean tokenValido = supabaseClient.validarToken(token);

        if (!tokenValido) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token inválido"));
        }

        try {
            QhDto conversacion = qhService.obtenerConversacionCompleta(id);

            // Verificar si la conversación tiene respuestas del profesor
            boolean tieneRespuestas = conversacion.getConversacion().getMensajes().stream()
                    .anyMatch(m -> "profesor".equals(m.getEmisor()));

            if (!tieneRespuestas) {
                // 404: no hay respuestas aún
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Aún no hay respuestas del profesor"));
            }

            // 200 OK: devuelve la conversación con mensajes
            return ResponseEntity.status(HttpStatus.OK).body(conversacion);

        } catch (RuntimeException e) {
            // 404 NOT FOUND: conversación no existe
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
    
    /* ============================================
    ENDPOINT PARA ALUMNO: Responder una converasción
 	POST http://localhost:8080/api/conversaciones/{id}/alumno-responder
    ============================================ */
    @PostMapping("/{id}/alumno-responder")
    public ResponseEntity<?> alumnoResponde(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String token = body.get("token"); //Para identificación 
        String contenido = body.get("contenido");
        if (token == null || contenido == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token y contenido son obligatorios"));
        }
        
        //Validación de token existente
        boolean tokenValido = supabaseClient.validarToken(token);
        if (!tokenValido) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token inválido o no autorizado")
            );
        }
        
        try {
            QhDto conversacion = qhService.alumnoResponde(id, contenido, token);
            return ResponseEntity.ok(conversacion);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
    
}