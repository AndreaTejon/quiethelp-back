package com.trinitarias.quiethelp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.trinitarias.quiethelp.components.SupabaseClient;
import com.trinitarias.quiethelp.dto.QhConversacionDto;
import com.trinitarias.quiethelp.dto.QhDashboardResumenDto;
import com.trinitarias.quiethelp.dto.QhDto;
import com.trinitarias.quiethelp.dto.QhEmisorDto;
import com.trinitarias.quiethelp.dto.QhMensajeDto;
import com.trinitarias.quiethelp.entity.QhConversacionEntity;
import com.trinitarias.quiethelp.entity.QhMensajeEntity;
import com.trinitarias.quiethelp.repository.QhConversacionRepository;
import com.trinitarias.quiethelp.repository.QhMensajeRepository;

@ExtendWith(MockitoExtension.class)
class QhServiceTest {

    // ==================== MOCKS ====================
    
    @Mock
    private QhConversacionRepository conversacionRepository;

    @Mock
    private QhMensajeRepository mensajeRepository;

    @Mock
    private RestTemplate restTemplate;  // Mock para n8n y IA

    @Mock
    private SupabaseClient supabaseClient;  // Mock para validar tokens

    @InjectMocks
    private QhService qhService;

    // ==================== DATOS DE PRUEBA ====================
    
    private QhDto testDto;
    private QhConversacionEntity testConversacion;
    private QhMensajeEntity testMensaje;
    private final Long TEST_ID = 1L;
    private final String TEST_TOKEN = "QUIET-1234";
    private final String TEST_REVISOR_ID = "8675d645-8521-44d6-a199-8be6f722b706";
    private final String TEST_REVISOR_NOMBRE = "Profesor Test";
    private final String IA_URL = "https://quiethelp-ia-production.up.railway.app/predict";
    private final String N8N_URL = "http://187.124.160.195:32768/webhook/quiethelp-anonimizar";

    @BeforeEach
    void setUp() {
        testDto = new QhDto();
        QhEmisorDto emisor = new QhEmisorDto();
        emisor.setTarjeta("Emocional");
        emisor.setCurso("1º ESO");
        emisor.setGrupo("F");
        emisor.setUrgente(false);
        testDto.setEmisor(emisor);
        testDto.setToken(TEST_TOKEN);

        QhConversacionDto conversacionDto = new QhConversacionDto();
        QhMensajeDto mensajeDto = new QhMensajeDto();
        mensajeDto.setMensaje("Test message");
        mensajeDto.setEmisor("alumno");
        conversacionDto.setMensajes(List.of(mensajeDto));
        testDto.setConversacion(conversacionDto);

        // Configurar entidad conversación
        testConversacion = new QhConversacionEntity();
        testConversacion.setId(TEST_ID);
        testConversacion.setEstado("PENDIENTE");
        testConversacion.setToken(TEST_TOKEN);
        testConversacion.setUrgente(false);

        // Configurar entidad mensaje
        testMensaje = new QhMensajeEntity();
        testMensaje.setId(TEST_ID);
        testMensaje.setContenido("Test message");
        testMensaje.setEmisor("alumno");
        testMensaje.setConversacion(testConversacion);
    }

    // ==================== CREAR CONVERSACIÓN (con IA y n8n) ====================
    
    @Test
    void crearConversacionExitoSinUrgenteRetornaQhDto() {
		ReflectionTestUtils.setField(qhService, "iaUrl", "https://quiethelp-ia-production.up.railway.app/predict");
		ReflectionTestUtils.setField(qhService, "n8nUrl", "http://187.124.160.195:32768/webhook/quiethelp-anonimizar");
		Map<String, Object> iaResponse = new HashMap<>();
        iaResponse.put("urgent", false);
        
        when(restTemplate.postForObject(eq(IA_URL), any(), eq(Map.class)))
            .thenReturn(iaResponse);
        when(conversacionRepository.save(any(QhConversacionEntity.class)))
            .thenReturn(testConversacion);
        when(mensajeRepository.save(any(QhMensajeEntity.class)))
            .thenReturn(testMensaje);
        when(restTemplate.postForObject(eq(N8N_URL), any(), eq(String.class)))
            .thenReturn("{}");

        QhDto resultado = qhService.crearConversacion(testDto);

        assertNotNull(resultado);
        verify(conversacionRepository, atLeastOnce()).save(any());
        verify(mensajeRepository, atLeastOnce()).save(any());
        verify(restTemplate, times(1)).postForObject(eq(IA_URL), any(), eq(Map.class));
        verify(restTemplate, times(1)).postForObject(eq(N8N_URL), any(), eq(String.class));
    }

    @Test
    void crearConversacion_IADetectaUrgenteGuardaComoUrgente() {
    	ReflectionTestUtils.setField(qhService, "iaUrl", "https://quiethelp-ia-production.up.railway.app/predict");
    	ReflectionTestUtils.setField(qhService, "n8nUrl", "http://187.124.160.195:32768/webhook/quiethelp-anonimizar");
        Map<String, Object> iaResponse = new HashMap<>();
        iaResponse.put("urgent", true);
        
        when(restTemplate.postForObject(eq(IA_URL), any(), eq(Map.class)))
            .thenReturn(iaResponse);
        when(conversacionRepository.save(any(QhConversacionEntity.class)))
            .thenReturn(testConversacion);
        when(mensajeRepository.save(any(QhMensajeEntity.class)))
            .thenReturn(testMensaje);
        when(restTemplate.postForObject(eq(N8N_URL), any(), eq(String.class)))
            .thenReturn("{}");

        QhDto resultado = qhService.crearConversacion(testDto);

        assertNotNull(resultado);
        verify(conversacionRepository, times(2)).save(any());
    }

    @Test
    void crearConversacionN8NFallaLanzaExcepcion() {
        // Given - Mock IA ok, pero n8n falla
        Map<String, Object> iaResponse = new HashMap<>();
        iaResponse.put("urgent", false);
        
        when(restTemplate.postForObject(eq(IA_URL), any(), eq(Map.class)))
            .thenReturn(iaResponse);
        when(conversacionRepository.save(any(QhConversacionEntity.class)))
            .thenReturn(testConversacion);
        when(mensajeRepository.save(any(QhMensajeEntity.class)))
            .thenReturn(testMensaje);
        when(restTemplate.postForObject(eq(N8N_URL), any(), eq(String.class)))
            .thenReturn(null);  // Simula que n8n no responde

        assertThrows(RuntimeException.class, () -> {
            qhService.crearConversacion(testDto);
        });
    }

    // ==================== OBTENER CONVERSACIONES POR TOKEN ====================
    
    @Test
    void obtenerConversacionesPorTokenExitoRetornaLista() {
        List<QhConversacionEntity> conversaciones = List.of(testConversacion);
        when(conversacionRepository.findByTokenAndEstadoNot(eq(TEST_TOKEN), eq("RESUELTO")))
            .thenReturn(conversaciones);

        List<QhConversacionEntity> resultado = qhService.obtenerConversacionesPorToken(TEST_TOKEN);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(conversacionRepository, times(1)).findByTokenAndEstadoNot(TEST_TOKEN, "RESUELTO");
    }

    @Test
    void obtenerConversacionesPorTokenSinConversacionesRetornaListaVacia() {
        when(conversacionRepository.findByTokenAndEstadoNot(eq(TEST_TOKEN), eq("RESUELTO")))
            .thenReturn(Collections.emptyList());

        List<QhConversacionEntity> resultado = qhService.obtenerConversacionesPorToken(TEST_TOKEN);

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    // ==================== OBTENER CONVERSACIÓN COMPLETA ====================
    
    @Test
    void obtenerConversacionCompletaExitoRetornaQhDto() {
        when(conversacionRepository.findById(TEST_ID)).thenReturn(Optional.of(testConversacion));

        QhDto resultado = qhService.obtenerConversacionCompleta(TEST_ID);

        assertNotNull(resultado);
        verify(conversacionRepository, times(1)).findById(TEST_ID);
    }

    @Test
    void obtenerConversacionCompletaNoExistenteLanzaExcepcion() {
        when(conversacionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            qhService.obtenerConversacionCompleta(999L);
        });
    }

    // ==================== ASIGNAR CONVERSACIÓN ====================
    
    @Test
    void asignarConversacionExitoRetornaQhDto() {
        when(conversacionRepository.findById(TEST_ID)).thenReturn(Optional.of(testConversacion));
        when(conversacionRepository.save(any(QhConversacionEntity.class))).thenReturn(testConversacion);

        QhDto resultado = qhService.asignarConversacion(TEST_ID, TEST_REVISOR_ID, TEST_REVISOR_NOMBRE);

        assertNotNull(resultado);
        assertEquals("EN_REVISION", testConversacion.getEstado());
        assertEquals(TEST_REVISOR_ID, testConversacion.getRevisorId());
        assertEquals(TEST_REVISOR_NOMBRE, testConversacion.getRevisorNombre());
        verify(conversacionRepository, times(1)).save(testConversacion);
    }

    @Test
    void asignarConversacionNoExistenteLanzaExcepcion() {
        when(conversacionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            qhService.asignarConversacion(999L, TEST_REVISOR_ID, TEST_REVISOR_NOMBRE);
        });
    }

    // ==================== RESPONDER CONVERSACIÓN ====================
    
    @Test
    void responderConversacionExitoRetornaQhDto() {
        testConversacion.setEstado("PENDIENTE");
        when(conversacionRepository.findById(TEST_ID)).thenReturn(Optional.of(testConversacion));
        when(mensajeRepository.save(any(QhMensajeEntity.class))).thenReturn(testMensaje);
        when(conversacionRepository.save(any(QhConversacionEntity.class))).thenReturn(testConversacion);
        when(mensajeRepository.findByConversacionIdOrderById(anyLong())).thenReturn(List.of(testMensaje));

        QhDto resultado = qhService.responderConversacion(TEST_ID, "Respuesta test", TEST_REVISOR_ID, TEST_REVISOR_NOMBRE);

        assertNotNull(resultado);
        verify(mensajeRepository, times(2)).save(any());
    }

    @Test
    void responderConversacionNoExistenteLanzaExcepcion() {
        when(conversacionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            qhService.responderConversacion(999L, "Respuesta", TEST_REVISOR_ID, TEST_REVISOR_NOMBRE);
        });
    }

    // ==================== ALUMNO RESPONDE (con n8n) ====================
    @Test
    void alumnoRespondeTokenValidoRetornaQhDto() throws Exception {
		ReflectionTestUtils.setField(qhService, "n8nUrl", "http://187.124.160.195:32768/webhook/quiethelp-anonimizar");

		when(conversacionRepository.findById(TEST_ID)).thenReturn(Optional.of(testConversacion));
		when(supabaseClient.validarToken(TEST_TOKEN)).thenReturn(true);
		when(mensajeRepository.save(any(QhMensajeEntity.class))).thenReturn(testMensaje);

		when(restTemplate.postForObject(anyString(), any(), eq(String.class))).thenReturn("OK");

		QhDto resultado = qhService.alumnoResponde(TEST_ID, "Respuesta alumno", TEST_TOKEN);

        assertNotNull(resultado);
        verify(mensajeRepository, times(1)).save(any());
    }

    @Test
    void alumnoRespondeTokenInvalidoLanzaExcepcion() {
        when(conversacionRepository.findById(TEST_ID)).thenReturn(Optional.of(testConversacion));
        when(supabaseClient.validarToken("INVALIDO")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> {
            qhService.alumnoResponde(TEST_ID, "Respuesta", "INVALIDO");
        });
    }

    @Test
    void alumnoRespondeConversacionNoExistenteLanzaExcepcion() {
        when(conversacionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            qhService.alumnoResponde(999L, "Respuesta", TEST_TOKEN);
        });
    }

    @Test
    void alumnoRespondeN8NFallaLanzaExcepcion() {
        when(conversacionRepository.findById(TEST_ID)).thenReturn(Optional.of(testConversacion));
        when(supabaseClient.validarToken(TEST_TOKEN)).thenReturn(true);
        when(mensajeRepository.save(any(QhMensajeEntity.class))).thenReturn(testMensaje);
        when(restTemplate.postForObject(eq(N8N_URL), any(), eq(String.class)))
            .thenThrow(new RuntimeException("N8N no respondió"));

        assertThrows(RuntimeException.class, () -> {
            qhService.alumnoResponde(TEST_ID, "Respuesta alumno", TEST_TOKEN);
        });
    }

    // ==================== CAMBIAR ESTADO ====================
    
    @Test
    void cambiarEstadoConversacionExitoRetornaQhDto() {
        when(conversacionRepository.findById(TEST_ID)).thenReturn(Optional.of(testConversacion));
        when(conversacionRepository.save(any())).thenReturn(testConversacion);

        QhDto resultado = qhService.cambiarEstadoConversacion(TEST_ID, "RESUELTO");

        assertNotNull(resultado);
        assertEquals("RESUELTO", testConversacion.getEstado());
        verify(conversacionRepository, times(1)).save(testConversacion);
    }

    @Test
    void cambiarEstadoConversacionNoExistenteLanzaExcepcion() {
        when(conversacionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            qhService.cambiarEstadoConversacion(999L, "RESUELTO");
        });
    }

    // ==================== OBTENER RESUMEN DASHBOARD ====================
    
    @Test
    void obtenerResumenDashboardExitoRetornaDto() {
        when(conversacionRepository.countByEstado("PENDIENTE")).thenReturn(5L);
        when(conversacionRepository.countByEstado("EN_REVISION")).thenReturn(3L);
        when(conversacionRepository.countByEstado("RESUELTO")).thenReturn(10L);
        when(conversacionRepository.countByUrgenteTrue()).thenReturn(2L);

        QhDashboardResumenDto resultado = qhService.obtenerResumenDashboard();

        assertNotNull(resultado);
        assertEquals(5, resultado.getPendientes());
        assertEquals(3, resultado.getEnRevision());
        assertEquals(10, resultado.getResueltos());
        assertEquals(2, resultado.getUrgentes());
        
        verify(conversacionRepository, times(1)).countByEstado("PENDIENTE");
        verify(conversacionRepository, times(1)).countByEstado("EN_REVISION");
        verify(conversacionRepository, times(1)).countByEstado("RESUELTO");
        verify(conversacionRepository, times(1)).countByUrgenteTrue();
    }

    // ==================== VERIFICAR INTEGRIDAD CADENA (Blockchain) ====================
    
    @Test
    void verificarIntegridadCadenaConversacionVaciaRetornaTrue() {
        when(mensajeRepository.findByConversacionIdOrderById(TEST_ID)).thenReturn(Collections.emptyList());

        boolean resultado = qhService.verificarIntegridadCadena(TEST_ID);

        assertTrue(resultado);
        verify(mensajeRepository, times(1)).findByConversacionIdOrderById(TEST_ID);
    }

    @Test
    void verificarIntegridadCadenaPrimerMensajeHashIncorrectoRetornaFalse() {
        testMensaje.setHashAnterior("123");  // Debería ser "0"
        when(mensajeRepository.findByConversacionIdOrderById(TEST_ID)).thenReturn(List.of(testMensaje));

        boolean resultado = qhService.verificarIntegridadCadena(TEST_ID);

        assertFalse(resultado);
    }

    // ==================== ACTUALIZAR MENSAJE ANONIMIZADO ====================
    
    @Test
    void actualizarMensajeAnonimizadoExitoRetornaQhDto() {
        when(mensajeRepository.findById(TEST_ID)).thenReturn(Optional.of(testMensaje));
        when(mensajeRepository.save(any())).thenReturn(testMensaje);

        QhDto resultado = qhService.actualizarMensajeAnonimizado(TEST_ID, "Texto anonimizado");

        assertNotNull(resultado);
        verify(mensajeRepository, times(1)).save(testMensaje);
    }

    @Test
    void actualizarMensajeAnonimizadoMensajeNoExistenteLanzaExcepcion() {
        when(mensajeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            qhService.actualizarMensajeAnonimizado(999L, "Texto");
        });
    }

    // ==================== OBTENER CONVERSACIONES DASHBOARD POR REVISOR ====================
    
    @Test
    void obtenerConversacionesDashboardPorRevisorExitoRetornaLista() {
        List<QhConversacionEntity> conversaciones = List.of(testConversacion);
        when(conversacionRepository.findPendientesYAsignadasARevisor(any(), any(), any(), any()))
            .thenReturn(conversaciones);

        List<QhDto> resultado = qhService.obtenerConversacionesDashboardPorRevisor(
            "PENDIENTE", null, null, TEST_REVISOR_ID);

        assertNotNull(resultado);
        verify(conversacionRepository, times(1))
            .findPendientesYAsignadasARevisor(any(), any(), any(), any());
    }

    @Test
    void obtenerConversacionesDashboardPorRevisorSinConversacionesRetornaListaVacia() {
        when(conversacionRepository.findPendientesYAsignadasARevisor(any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());

        List<QhDto> resultado = qhService.obtenerConversacionesDashboardPorRevisor(
            "PENDIENTE", null, null, TEST_REVISOR_ID);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    // ==================== MARCAR MENSAJES COMO LEÍDOS ====================
    
    @Test
    void marcarMensajesAlumnoComoLeidosExitoRetornaNumero() {
        Long revisorIdLong = 123L;
        testConversacion.setRevisorId(String.valueOf(revisorIdLong));  // "123"
        
        when(conversacionRepository.findById(TEST_ID)).thenReturn(Optional.of(testConversacion));
        when(mensajeRepository.marcarMensajesAlumnoComoLeidos(TEST_ID)).thenReturn(3);

        int resultado = qhService.marcarMensajesAlumnoComoLeidos(TEST_ID, revisorIdLong);  // 123L

        assertEquals(3, resultado);
        verify(mensajeRepository, times(1)).marcarMensajesAlumnoComoLeidos(TEST_ID);
    }

    @Test
    void marcarMensajesAlumnoComoLeidosNoAutorizadoLanzaExcepcion() {
        testConversacion.setRevisorId("otro-id-diferente");
        when(conversacionRepository.findById(TEST_ID)).thenReturn(Optional.of(testConversacion));

        assertThrows(RuntimeException.class, () -> {
            qhService.marcarMensajesAlumnoComoLeidos(TEST_ID, 123L);
        });
    }

    @Test
    void marcarMensajesAlumnoComoLeidosConversacionSinRevisorLanzaExcepcion() {
        // La conversación no tiene revisor asignado
        testConversacion.setRevisorId(null);
        when(conversacionRepository.findById(TEST_ID)).thenReturn(Optional.of(testConversacion));

        assertThrows(RuntimeException.class, () -> {
            qhService.marcarMensajesAlumnoComoLeidos(TEST_ID, 123L);
        });
    }

    // ==================== OBTENER ÚLTIMO HASH (método privado, pero probado indirectamente) ====================
    // Este método se prueba a través de verificarIntegridadCadena y responderConversacion
    
    @Test
    void isCadenaVerificadaCacheExpiradoRecalcula() {
        testConversacion.setCadenaVerificada(false);
        when(conversacionRepository.findById(TEST_ID)).thenReturn(Optional.of(testConversacion));
        when(mensajeRepository.findByConversacionIdOrderById(TEST_ID)).thenReturn(Collections.emptyList());
        when(conversacionRepository.save(any())).thenReturn(testConversacion);

        boolean resultado = qhService.isCadenaVerificada(TEST_ID);

        assertTrue(resultado);
        verify(conversacionRepository, times(1)).save(any());
    }
    
 // ==================== TEST FALTANTES PARA VALIDATOR Y BLOCKCHAIN ====================

    @Test
    void verificarIntegridadCadenaUnSoloMensajeValidoRetornaTrue() {
        testMensaje.setHashAnterior("0");
        testMensaje.setHashActual("hashCalculado");
        
        when(mensajeRepository.findByConversacionIdOrderById(TEST_ID))
                .thenReturn(List.of(testMensaje));
        
        qhService.verificarIntegridadCadena(TEST_ID);
        
        verify(mensajeRepository, times(1)).findByConversacionIdOrderById(TEST_ID);
    }

    @Test
    void verificarIntegridadCadenaDosMensajesEnlazadosRetornaTrue() {
        QhMensajeEntity primero = new QhMensajeEntity();
        primero.setId(1L);
        primero.setHashAnterior("0");
        primero.setHashActual("hash1");
        
        QhMensajeEntity segundo = new QhMensajeEntity();
        segundo.setId(2L);
        segundo.setHashAnterior("hash1");
        segundo.setHashActual("hash2");
        
        when(mensajeRepository.findByConversacionIdOrderById(TEST_ID))
                .thenReturn(List.of(primero, segundo));
        
        qhService.verificarIntegridadCadena(TEST_ID);
        
        verify(mensajeRepository, times(1)).findByConversacionIdOrderById(TEST_ID);
    }
    
    
 // ==================== TESTS PARA MEJORAR COBERTURA DE RAMAS ====================

    @Test
    void verificarIntegridadCadenaCadenaRotaRetornaFalse() {
        testMensaje.setHashAnterior("0");
        testMensaje.setHashActual("hashFalso"); // Hash incorrecto
        
        when(mensajeRepository.findByConversacionIdOrderById(TEST_ID))
            .thenReturn(List.of(testMensaje));

        boolean resultado = qhService.verificarIntegridadCadena(TEST_ID);

        assertFalse(resultado);
    }

    @Test
    void verificarIntegridadCadenaEnlaceIncorrectoRetornaFalse() {
        QhMensajeEntity primero = new QhMensajeEntity();
        primero.setId(1L);
        primero.setHashAnterior("0");
        primero.setHashActual("hash1");
        
        QhMensajeEntity segundo = new QhMensajeEntity();
        segundo.setId(2L);
        segundo.setHashAnterior("hashIncorrecto"); // No coincide con hash1
        segundo.setHashActual("hash2");
        
        when(mensajeRepository.findByConversacionIdOrderById(TEST_ID))
            .thenReturn(List.of(primero, segundo));

        boolean resultado = qhService.verificarIntegridadCadena(TEST_ID);

        assertFalse(resultado);
    }
    
    @Test
    void crearConversacionConversacionRepositorySavePrimeroFallaLanzaExcepcion() {
        ReflectionTestUtils.setField(qhService, "iaUrl", IA_URL);
        ReflectionTestUtils.setField(qhService, "n8nUrl", N8N_URL);
        
        Map<String, Object> iaResponse = new HashMap<>();
        iaResponse.put("urgent", false);
        
        when(restTemplate.postForObject(eq(IA_URL), any(), eq(Map.class)))
            .thenReturn(iaResponse);
        when(conversacionRepository.save(any(QhConversacionEntity.class)))
            .thenThrow(new RuntimeException("Error de base de datos"));

        assertThrows(RuntimeException.class, () -> {
            qhService.crearConversacion(testDto);
        });
    }

    @Test
    void obtenerConversacionesDashboardPorRevisorConFiltrosValidosRetornaLista() {
        List<QhConversacionEntity> conversaciones = List.of(testConversacion);
        when(conversacionRepository.findPendientesYAsignadasARevisor(eq("PENDIENTE"), eq("Emocional"), eq(true), eq(TEST_REVISOR_ID)))
            .thenReturn(conversaciones);

        List<QhDto> resultado = qhService.obtenerConversacionesDashboardPorRevisor(
            "PENDIENTE", "Emocional", true, TEST_REVISOR_ID);

        assertNotNull(resultado);
        assertFalse(resultado.isEmpty());
        verify(conversacionRepository, times(1))
            .findPendientesYAsignadasARevisor(eq("PENDIENTE"), eq("Emocional"), eq(true), eq(TEST_REVISOR_ID));
    }

    @Test
    void obtenerConversacionesDashboardPorRevisorConEstadoNullRetornaLista() {
        List<QhConversacionEntity> conversaciones = List.of(testConversacion);
        when(conversacionRepository.findPendientesYAsignadasARevisor(eq(null), eq(null), eq(null), eq(TEST_REVISOR_ID)))
            .thenReturn(conversaciones);

        List<QhDto> resultado = qhService.obtenerConversacionesDashboardPorRevisor(
            null, null, null, TEST_REVISOR_ID);

        assertNotNull(resultado);
        verify(conversacionRepository, times(1))
            .findPendientesYAsignadasARevisor(eq(null), eq(null), eq(null), eq(TEST_REVISOR_ID));
    }

    @Test
    void isCadenaVerificadaConversacionYaVerificadaYRecienteRetornaTrue() {
        testConversacion.setCadenaVerificada(true);
        testConversacion.setUltimaVerificacion(java.time.LocalDateTime.now().minusMinutes(2));
        when(conversacionRepository.findById(TEST_ID)).thenReturn(Optional.of(testConversacion));

        boolean resultado = qhService.isCadenaVerificada(TEST_ID);

        assertTrue(resultado);
        verify(conversacionRepository, never()).save(any());
        verify(mensajeRepository, never()).findByConversacionIdOrderById(anyLong());
    }

    @Test
    void isCadenaVerificadaConversacionVerificadaAntiguaRecalcula() {
        testConversacion.setCadenaVerificada(true);
        testConversacion.setUltimaVerificacion(java.time.LocalDateTime.now().minusMinutes(10));
        when(conversacionRepository.findById(TEST_ID)).thenReturn(Optional.of(testConversacion));
        when(mensajeRepository.findByConversacionIdOrderById(TEST_ID)).thenReturn(Collections.emptyList());
        when(conversacionRepository.save(any())).thenReturn(testConversacion);

        boolean resultado = qhService.isCadenaVerificada(TEST_ID);

        assertTrue(resultado);
        verify(conversacionRepository, times(1)).save(any());
    }

    @Test
    void actualizarMensajeAnonimizadoCalculaHashYActualizaRetornaQhDto() {
        String contenidoAnonimizado = "Texto anonimizado de prueba";
        testMensaje.setContenido(contenidoAnonimizado);
        
        when(mensajeRepository.findById(TEST_ID)).thenReturn(Optional.of(testMensaje));
        when(mensajeRepository.save(any(QhMensajeEntity.class))).thenReturn(testMensaje);
        when(mensajeRepository.findByConversacionIdOrderById(anyLong())).thenReturn(List.of(testMensaje));

        QhDto resultado = qhService.actualizarMensajeAnonimizado(TEST_ID, contenidoAnonimizado);

        assertNotNull(resultado);
        verify(mensajeRepository, times(1)).save(testMensaje);
    }

    @Test
    void cambiarEstadoConversacionConEstadoResuelto_asignaFechaResolucion() {
        when(conversacionRepository.findById(TEST_ID)).thenReturn(Optional.of(testConversacion));
        when(conversacionRepository.save(any())).thenReturn(testConversacion);

        QhDto resultado = qhService.cambiarEstadoConversacion(TEST_ID, "RESUELTO");

        assertNotNull(resultado);
        assertEquals("RESUELTO", testConversacion.getEstado());
        assertNotNull(testConversacion.getFechaResolucion());
        verify(conversacionRepository, times(1)).save(testConversacion);
    }

    @Test
    void responderConversacionConversacionPendienteCambiaEstadoYAsigna() {
        testConversacion.setEstado("PENDIENTE");
        when(conversacionRepository.findById(TEST_ID)).thenReturn(Optional.of(testConversacion));
        when(mensajeRepository.save(any(QhMensajeEntity.class))).thenReturn(testMensaje);
        when(conversacionRepository.save(any(QhConversacionEntity.class))).thenReturn(testConversacion);
        when(mensajeRepository.findByConversacionIdOrderById(anyLong())).thenReturn(List.of(testMensaje));

        QhDto resultado = qhService.responderConversacion(TEST_ID, "Respuesta test", TEST_REVISOR_ID, TEST_REVISOR_NOMBRE);

        assertNotNull(resultado);
        assertEquals("EN_REVISION", testConversacion.getEstado());
        assertEquals(TEST_REVISOR_ID, testConversacion.getRevisorId());
        verify(conversacionRepository, times(1)).save(any());
    }

    @Test
    void marcarMensajesComoLeidosConEmisorProfesorLlamaMetodoCorrecto() {
        Long conversacionId = TEST_ID;
        String emisor = "profesor";
        
        qhService.marcarMensajesComoLeidos(conversacionId, emisor);
        
        verify(mensajeRepository, times(1)).marcarMensajesProfesorComoLeidos(conversacionId);
        verify(mensajeRepository, never()).marcarMensajesAlumnoComoLeidos(anyLong());
    }

    @Test
    void marcarMensajesComoLeidosConEmisorAlumnoLlamaMetodoCorrecto() {
        Long conversacionId = TEST_ID;
        String emisor = "alumno";
        
        qhService.marcarMensajesComoLeidos(conversacionId, emisor);
        
        verify(mensajeRepository, times(1)).marcarMensajesAlumnoComoLeidos(conversacionId);
        verify(mensajeRepository, never()).marcarMensajesProfesorComoLeidos(anyLong());
    }

}