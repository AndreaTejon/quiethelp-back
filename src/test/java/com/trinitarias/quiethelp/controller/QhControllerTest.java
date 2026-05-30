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
    void crearConversacion_TokenValido_MensajeReal_Retorna201() throws Exception {
        // Arrange
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

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated());
    }

    @Test
    void crearConversacion_TokenInvalido_Retorna401() throws Exception {
        // Arrange
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

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearConversacion_MensajeValidacion_Retorna201SinGuardar() throws Exception {
        // Arrange
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

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valido").value(true))
                .andExpect(jsonPath("$.validacion").value(true));

        // Verificar que NO se llamó al servicio
        verify(qhService, never()).crearConversacion(any());
    }

    @Test
    void obtenerDashboard_SinFiltros_Retorna200() throws Exception {
        // Arrange
        List<QhDto> mockConversaciones = Arrays.asList(new QhDto(), new QhDto());
        when(qhService.obtenerConversacionesDashboard(any(), any(), any())).thenReturn(mockConversaciones);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void obtenerDashboard_ConRevisorId_Retorna200() throws Exception {
        // Arrange
        List<QhDto> mockConversaciones = Arrays.asList(new QhDto());
        when(qhService.obtenerConversacionesDashboardPorRevisor(any(), any(), any(), any())).thenReturn(mockConversaciones);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/dashboard")
                .param("revisorId", "8675d645-8521-44d6-a199-8be6f722b706"))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerConversacion_Existente_Retorna200() throws Exception {
        // Arrange
        QhDto mockConversacion = new QhDto();
        QhConversacionDto convDto = new QhConversacionDto();
        convDto.setEstado("PENDIENTE");
        mockConversacion.setConversacion(convDto);
        
        when(qhService.obtenerConversacionCompleta(1L)).thenReturn(mockConversacion);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerConversacion_NoExistente_Retorna404() throws Exception {
        // Arrange
        when(qhService.obtenerConversacionCompleta(999L))
                .thenThrow(new RuntimeException("Conversacion no encontrada"));

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerConversacion_SinPermisos_Retorna403() throws Exception {
        // Arrange
        QhDto mockConversacion = new QhDto();
        QhConversacionDto convDto = new QhConversacionDto();
        convDto.setEstado("EN_REVISION");
        convDto.setRevisorId("otro-profesor-id");
        mockConversacion.setConversacion(convDto);
        
        when(qhService.obtenerConversacionCompleta(1L)).thenReturn(mockConversacion);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/1")
                .param("revisorId", "mi-id-diferente"))
                .andExpect(status().isForbidden());
    }

    @Test
    void asignarConversacion_Existente_Retorna200() throws Exception {
        // Arrange
        QhDto mockResponse = new QhDto();
        when(qhService.asignarConversacion(eq(1L), anyString(), anyString())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/1/asignar")
                .param("revisorId", "profesor-123")
                .param("revisorNombre", "María"))
                .andExpect(status().isOk());
    }

    @Test
    void asignarConversacion_NoExistente_Retorna404() throws Exception {
        // Arrange
        when(qhService.asignarConversacion(eq(999L), anyString(), anyString()))
                .thenThrow(new RuntimeException("Conversacion no encontrada"));

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/999/asignar")
                .param("revisorId", "profesor-123")
                .param("revisorNombre", "María"))
                .andExpect(status().isNotFound());
    }

    @Test
    void responderConversacion_DatosValidos_Retorna200() throws Exception {
        // Arrange
        String requestJson = """
            {
                "contenido": "Gracias por compartir",
                "revisorId": "profesor-123",
                "revisorNombre": "María"
            }
            """;

        QhDto mockResponse = new QhDto();
        when(qhService.responderConversacion(anyLong(), anyString(), anyString(), anyString())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/1/responder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk());
    }

    @Test
    void responderConversacion_SinContenido_Retorna400() throws Exception {
        // Arrange
        String requestJson = """
            {
                "revisorId": "profesor-123",
                "revisorNombre": "María"
            }
            """;

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/1/responder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarEstado_EstadoValido_Retorna200() throws Exception {
        // Arrange
        QhDto mockResponse = new QhDto();
        when(qhService.cambiarEstadoConversacion(eq(1L), eq("RESUELTO"))).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/1/estado")
                .param("nuevoEstado", "RESUELTO"))
                .andExpect(status().isOk());
    }

    @Test
    void cambiarEstado_EstadoInvalido_Retorna400() throws Exception {
        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/1/estado")
                .param("nuevoEstado", "INVALIDO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerResumen_Retorna200() throws Exception {
        // Arrange
        QhDashboardResumenDto mockResumen = new QhDashboardResumenDto();
        mockResumen.setPendientes(10);
        mockResumen.setEnRevision(5);
        mockResumen.setResueltos(3);
        mockResumen.setUrgentes(2);
        
        when(qhService.obtenerResumenDashboard()).thenReturn(mockResumen);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendientes").value(10))
                .andExpect(jsonPath("$.enRevision").value(5));
    }

    @Test
    void obtenerConversacionesAlumno_TokenValido_Retorna200() throws Exception {
        // Arrange
        List<QhConversacionEntity> mockConversaciones = Arrays.asList(new QhConversacionEntity());
        when(qhService.obtenerConversacionesPorToken(TOKEN_VALIDO)).thenReturn(mockConversaciones);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/alumno")
                .param("token", TOKEN_VALIDO))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerConversacionesAlumno_TokenInvalido_Retorna401() throws Exception {
        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/alumno")
                .param("token", "INVALIDO"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verificarCadena_Valida_Retorna200ConTrue() throws Exception {
        // Arrange
        when(qhService.verificarIntegridadCadena(1L)).thenReturn(true);
        when(qhService.obtenerConversacionCompleta(1L)).thenReturn(new QhDto());

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/1/verificar-cadena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valida").value(true));
    }

    @Test
    void verificarCadena_NoValida_Retorna500() throws Exception {
        // Arrange
        when(qhService.verificarIntegridadCadena(1L)).thenReturn(false);
        when(qhService.obtenerConversacionCompleta(1L)).thenReturn(new QhDto());

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/1/verificar-cadena"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.valida").value(false));
    }

    @Test
    void actualizarMensajeAnonimizado_DatosValidos_Retorna200() throws Exception {
        // Arrange
        String requestJson = """
            {
                "contenidoAnonimizado": "Me siento mal"
            }
            """;

        QhDto mockResponse = new QhDto();
        when(qhService.actualizarMensajeAnonimizado(eq(1L), anyString())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/mensajes/1/anonimizar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk());
    }

    @Test
    void marcarMensajesComoLeidos_DatosValidos_Retorna200() throws Exception {
        // Arrange
        String requestJson = """
            {
                "revisorId": 123
            }
            """;

        when(qhService.marcarMensajesAlumnoComoLeidos(eq(1L), eq(123L))).thenReturn(5);

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/1/marcar-leidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.mensajesActualizados").value(5));
    }

    @Test
    void alumnoResponde_DatosValidos_Retorna200() throws Exception {
        // Arrange
        String requestJson = """
            {
                "token": "QUIET-1234",
                "contenido": "Gracias por la ayuda"
            }
            """;

        QhDto mockResponse = new QhDto();
        when(qhService.alumnoResponde(eq(1L), anyString(), anyString())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/1/alumno-responder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk());
    }
}