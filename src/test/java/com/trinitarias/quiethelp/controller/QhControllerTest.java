package com.trinitarias.quiethelp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.trinitarias.quiethelp.components.QhValidator;
import com.trinitarias.quiethelp.components.SupabaseClient;
import com.trinitarias.quiethelp.dto.QhConversacionDto;
import com.trinitarias.quiethelp.dto.QhDashboardResumenDto;
import com.trinitarias.quiethelp.dto.QhDto;
import com.trinitarias.quiethelp.dto.QhMensajeDto;
import com.trinitarias.quiethelp.entity.QhConversacionEntity;
import com.trinitarias.quiethelp.service.QhService;

@WebMvcTest(QhController.class)
class QhControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private QhService qhService;

	@MockitoBean
	private QhValidator qhValidator;

	@MockitoBean
	private SupabaseClient supabaseClient;

	private final String BASE_URL = "/api/conversaciones";
	private final String TOKEN_VALIDO = "QUIET-1234";

	@BeforeEach
	void setUp() {
		// Configurar mocks comunes
		when(supabaseClient.validarToken(TOKEN_VALIDO)).thenReturn(true);
		when(supabaseClient.validarToken("INVALIDO")).thenReturn(false);
		when(qhValidator.validarQhDto(any())).thenReturn(List.of());
	}

	@Test
	void crearConversacionTokenValidoMensajeRealRetorna201() throws Exception {

		String requestJson = """
				{
				    "token": "QUIET-1234",
				    "emisor": {
				        "tarjeta": "Emocional",
				        "curso": "1º ESO",
				        "grupo": "F",
				        "urgente": false
				    },
				    "conversacion": {
				        "mensajes": [
				            {
				                "emisor": "alumno",
				                "mensaje": "Me siento mal"
				            }
				        ]
				    }
				}
				""";

		QhDto mockResponse = new QhDto();
		when(qhService.crearConversacion(any())).thenReturn(mockResponse);

		mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(requestJson))
				.andExpect(status().isCreated());
	}

	@Test
	void crearConversacionTokenInvalidoRetorna401() throws Exception {

		String requestJson = """
				{
				    "token": "INVALIDO",
				    "emisor": {
				        "tarjeta": "Emocional"
				    },
				    "conversacion": {
				        "mensajes": [
				            {
				                "emisor": "alumno",
				                "mensaje": "validación"
				            }
				        ]
				    }
				}
				""";

		mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(requestJson))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void crearConversacionMensajeValidacionRetorna201SinGuardar() throws Exception {

		String requestJson = """
				{
				    "token": "QUIET-1234",
				    "emisor": {
				        "tarjeta": "Otro"
				    },
				    "conversacion": {
				        "mensajes": [
				            {
				                "emisor": "alumno",
				                "mensaje": "validación"
				            }
				        ]
				    }
				}
				""";

		mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(requestJson))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.valido").value(true))
				.andExpect(jsonPath("$.validacion").value(true));

		// Verificar que NO se llamó al servicio
		verify(qhService, never()).crearConversacion(any());
	}

	@Test
	void obtenerDashboardSinFiltrosRetorna200() throws Exception {

		List<QhDto> mockConversaciones = Arrays.asList(new QhDto(), new QhDto());
		when(qhService.obtenerConversacionesDashboard(any(), any(), any())).thenReturn(mockConversaciones);

		mockMvc.perform(get(BASE_URL + "/dashboard")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void obtenerDashboardConRevisorIdRetorna200() throws Exception {

		List<QhDto> mockConversaciones = Arrays.asList(new QhDto());
		when(qhService.obtenerConversacionesDashboardPorRevisor(any(), any(), any(), any()))
				.thenReturn(mockConversaciones);

		mockMvc.perform(get(BASE_URL + "/dashboard").param("revisorId", "8675d645-8521-44d6-a199-8be6f722b706"))
				.andExpect(status().isOk());
	}

	@Test
	void obtenerConversacionExistenteRetorna200() throws Exception {

		QhDto mockConversacion = new QhDto();
		QhConversacionDto convDto = new QhConversacionDto();
		convDto.setEstado("PENDIENTE");
		mockConversacion.setConversacion(convDto);

		when(qhService.obtenerConversacionCompleta(1L)).thenReturn(mockConversacion);

		mockMvc.perform(get(BASE_URL + "/1")).andExpect(status().isOk());
	}

	@Test
	void obtenerConversacionNoExistenteRetorna404() throws Exception {

		when(qhService.obtenerConversacionCompleta(999L)).thenThrow(new RuntimeException("Conversacion no encontrada"));

		mockMvc.perform(get(BASE_URL + "/999")).andExpect(status().isNotFound());
	}

	@Test
	void obtenerConversacionSinPermisosRetorna403() throws Exception {

		QhDto mockConversacion = new QhDto();
		QhConversacionDto convDto = new QhConversacionDto();
		convDto.setEstado("EN_REVISION");
		convDto.setRevisorId("otro-profesor-id");
		mockConversacion.setConversacion(convDto);

		when(qhService.obtenerConversacionCompleta(1L)).thenReturn(mockConversacion);

		mockMvc.perform(get(BASE_URL + "/1").param("revisorId", "mi-id-diferente")).andExpect(status().isForbidden());
	}

	@Test
	void asignarConversacionExistenteRetorna200() throws Exception {

		QhDto mockResponse = new QhDto();
		when(qhService.asignarConversacion(eq(1L), anyString(), anyString())).thenReturn(mockResponse);

		mockMvc.perform(
				patch(BASE_URL + "/1/asignar").param("revisorId", "profesor-123").param("revisorNombre", "María"))
				.andExpect(status().isOk());
	}

	@Test
	void asignarConversacionNoExistenteRetorna404() throws Exception {

		when(qhService.asignarConversacion(eq(999L), anyString(), anyString()))
				.thenThrow(new RuntimeException("Conversacion no encontrada"));

		mockMvc.perform(
				patch(BASE_URL + "/999/asignar").param("revisorId", "profesor-123").param("revisorNombre", "María"))
				.andExpect(status().isNotFound());
	}

	@Test
	void responderConversacionDatosValidosRetorna200() throws Exception {

		String requestJson = """
				{
				    "contenido": "Gracias por compartir",
				    "revisorId": "profesor-123",
				    "revisorNombre": "María"
				}
				""";

		QhDto mockResponse = new QhDto();
		when(qhService.responderConversacion(anyLong(), anyString(), anyString(), anyString()))
				.thenReturn(mockResponse);

		mockMvc.perform(post(BASE_URL + "/1/responder").contentType(MediaType.APPLICATION_JSON).content(requestJson))
				.andExpect(status().isOk());
	}

	@Test
	void responderConversacionSinContenidoRetorna400() throws Exception {

		String requestJson = """
				{
				    "revisorId": "profesor-123",
				    "revisorNombre": "María"
				}
				""";

		mockMvc.perform(post(BASE_URL + "/1/responder").contentType(MediaType.APPLICATION_JSON).content(requestJson))
				.andExpect(status().isBadRequest());
	}

	@Test
	void cambiarEstadoEstadoValidoRetorna200() throws Exception {

		QhDto mockResponse = new QhDto();
		when(qhService.cambiarEstadoConversacion(eq(1L), eq("RESUELTO"))).thenReturn(mockResponse);

		mockMvc.perform(patch(BASE_URL + "/1/estado").param("nuevoEstado", "RESUELTO")).andExpect(status().isOk());
	}

	@Test
	void cambiarEstadoEstadoInvalidoRetorna400() throws Exception {

		mockMvc.perform(patch(BASE_URL + "/1/estado").param("nuevoEstado", "INVALIDO"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void obtenerResumenRetorna200() throws Exception {

		QhDashboardResumenDto mockResumen = new QhDashboardResumenDto();
		mockResumen.setPendientes(10);
		mockResumen.setEnRevision(5);
		mockResumen.setResueltos(3);
		mockResumen.setUrgentes(2);

		when(qhService.obtenerResumenDashboard()).thenReturn(mockResumen);

		mockMvc.perform(get(BASE_URL + "/resumen")).andExpect(status().isOk())
				.andExpect(jsonPath("$.pendientes").value(10)).andExpect(jsonPath("$.enRevision").value(5));
	}

	@Test
	void obtenerConversacionesAlumnoTokenValidoRetorna200() throws Exception {

		List<QhConversacionEntity> mockConversaciones = Arrays.asList(new QhConversacionEntity());
		when(qhService.obtenerConversacionesPorToken(TOKEN_VALIDO)).thenReturn(mockConversaciones);

		mockMvc.perform(get(BASE_URL + "/alumno").param("token", TOKEN_VALIDO)).andExpect(status().isOk());
	}

	@Test
	void obtenerConversacionesAlumnoTokenInvalidoRetorna401() throws Exception {

		mockMvc.perform(get(BASE_URL + "/alumno").param("token", "INVALIDO")).andExpect(status().isUnauthorized());
	}

	@Test
	void verificarCadenaValidaRetorna200ConTrue() throws Exception {

		when(qhService.verificarIntegridadCadena(1L)).thenReturn(true);
		when(qhService.obtenerConversacionCompleta(1L)).thenReturn(new QhDto());

		mockMvc.perform(get(BASE_URL + "/1/verificar-cadena")).andExpect(status().isOk())
				.andExpect(jsonPath("$.valida").value(true));
	}

	@Test
	void verificarCadenaNoValidaRetorna500() throws Exception {

		when(qhService.verificarIntegridadCadena(1L)).thenReturn(false);
		when(qhService.obtenerConversacionCompleta(1L)).thenReturn(new QhDto());

		mockMvc.perform(get(BASE_URL + "/1/verificar-cadena")).andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.valida").value(false));
	}

	@Test
	void actualizarMensajeAnonimizadoDatosValidosRetorna200() throws Exception {

		String requestJson = """
				{
				    "contenidoAnonimizado": "Me siento mal"
				}
				""";

		QhDto mockResponse = new QhDto();
		when(qhService.actualizarMensajeAnonimizado(eq(1L), anyString())).thenReturn(mockResponse);

		mockMvc.perform(
				patch(BASE_URL + "/mensajes/1/anonimizar").contentType(MediaType.APPLICATION_JSON).content(requestJson))
				.andExpect(status().isOk());
	}

	@Test
	void marcarMensajesComoLeidosDatosValidosRetorna200() throws Exception {

		String requestJson = """
				{
				    "revisorId": 123
				}
				""";

		when(qhService.marcarMensajesAlumnoComoLeidos(eq(1L), eq(123L))).thenReturn(5);

		mockMvc.perform(put(BASE_URL + "/1/marcar-leidos").contentType(MediaType.APPLICATION_JSON).content(requestJson))
				.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.mensajesActualizados").value(5));
	}

	@Test
	void alumnoRespondeDatosValidosRetorna200() throws Exception {

		String requestJson = """
				{
				    "token": "QUIET-1234",
				    "contenido": "Gracias por la ayuda"
				}
				""";

		QhDto mockResponse = new QhDto();
		when(qhService.alumnoResponde(eq(1L), anyString(), anyString())).thenReturn(mockResponse);

		mockMvc.perform(
				post(BASE_URL + "/1/alumno-responder").contentType(MediaType.APPLICATION_JSON).content(requestJson))
				.andExpect(status().isOk());
	}

	@Test
	void obtenerConversacionAlumnoTokenValidoRetorna200() throws Exception {
		QhDto mockConversacion = new QhDto();
		QhConversacionDto convDto = new QhConversacionDto();
		QhMensajeDto msgDto = new QhMensajeDto();
		msgDto.setEmisor("profesor");
		convDto.setMensajes(List.of(msgDto));
		mockConversacion.setConversacion(convDto);

		when(qhService.obtenerConversacionCompleta(1L)).thenReturn(mockConversacion);
		when(supabaseClient.validarToken(TOKEN_VALIDO)).thenReturn(true);

		mockMvc.perform(get(BASE_URL + "/alumno/1").param("token", TOKEN_VALIDO)).andExpect(status().isOk());
	}

	@Test
	void obtenerConversacionAlumnoSinRespuestasRetorna404() throws Exception {
		QhDto mockConversacion = new QhDto();
		QhConversacionDto convDto = new QhConversacionDto();
		convDto.setMensajes(List.of()); // Sin mensajes del profesor
		mockConversacion.setConversacion(convDto);

		when(qhService.obtenerConversacionCompleta(1L)).thenReturn(mockConversacion);
		when(supabaseClient.validarToken(TOKEN_VALIDO)).thenReturn(true);

		mockMvc.perform(get(BASE_URL + "/alumno/1").param("token", TOKEN_VALIDO)).andExpect(status().isNotFound());
	}

	@Test
	void obtenerConversacionAlumnoTokenInvalidoRetorna401() throws Exception {
		mockMvc.perform(get(BASE_URL + "/alumno/1").param("token", "INVALIDO")).andExpect(status().isUnauthorized());
	}

	@Test
	void verificarCadenaConversacionNoExisteRetorna404() throws Exception {
		when(qhService.obtenerConversacionCompleta(999L)).thenThrow(new RuntimeException("Conversacion no encontrada"));

		mockMvc.perform(get(BASE_URL + "/999/verificar-cadena")).andExpect(status().isNotFound());
	}
	
	// ==================== TESTS PARA VALIDATOR ====================

    @Test
    void crearConversacionContenidoVacioRetorna400() throws Exception {
        String requestJson = """
            {
                "token": "QUIET-1234",
                "emisor": {
                    "tarjeta": "Emocional"
                },
                "conversacion": {
                    "mensajes": [
                        {
                            "emisor": "alumno",
                            "mensaje": ""
                        }
                    ]
                }
            }
            """;
        
        when(qhValidator.validarQhDto(any())).thenReturn(List.of("El mensaje no puede estar vacío"));
        
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearConversacionCategoriaInvalidaRetorna400() throws Exception {
        String requestJson = """
            {
                "token": "QUIET-1234",
                "emisor": {
                    "tarjeta": "Invalida"
                },
                "conversacion": {
                    "mensajes": [
                        {
                            "emisor": "alumno",
                            "mensaje": "Mensaje de prueba"
                        }
                    ]
                }
            }
            """;
        
        when(qhValidator.validarQhDto(any())).thenReturn(List.of("Categoría no válida"));
        
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void crearConversacionSinTokenRetorna400() throws Exception {
        // Arrange
        String requestJson = """
            {
                "emisor": {
                    "tarjeta": "Emocional"
                },
                "conversacion": {
                    "mensajes": [
                        {
                            "emisor": "alumno",
                            "mensaje": "test"
                        }
                    ]
                }
            }
            """;

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void marcarLeidosSinRevisorIdRetorna400() throws Exception {
        String requestJson = "{}";

        mockMvc.perform(put(BASE_URL + "/1/marcar-leidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }
}