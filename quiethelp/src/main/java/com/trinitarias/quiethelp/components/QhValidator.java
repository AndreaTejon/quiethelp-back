package com.trinitarias.quiethelp.components;

import com.trinitarias.quiethelp.dto.QhDto;
import com.trinitarias.quiethelp.dto.QhDtoConToken;
import com.trinitarias.quiethelp.dto.QhEmisorDto;
import com.trinitarias.quiethelp.dto.QhMensajeDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class QhValidator {

    private static final List<String> CATEGORIAS_VALIDAS = 
        List.of("Bullying", "Académico", "Emocional", "Otro");
    
    private static final Pattern TOKEN_PATTERN = 
        Pattern.compile("^[A-Z0-9]{4,20}$"); // Ejemplo: QUIET-1234 o similar

    /* Valida el DTO completo CON TOKEN (para el endpoint del alumno) */
    public List<String> validarQhDtoConToken(QhDtoConToken dtoConToken) {
        List<String> errores = new ArrayList<>();
        
        if (dtoConToken == null) {
            errores.add("El DTO no puede ser null");
            return errores;
        }
        
        // 1. VALIDAR TOKEN
        if (dtoConToken.getToken() == null || dtoConToken.getToken().trim().isEmpty()) {
            errores.add("El token es obligatorio");
        } else if (!TOKEN_PATTERN.matcher(dtoConToken.getToken()).matches()) {
            errores.add("Formato de token inválido");
        } else if (dtoConToken.getToken().length() < 4 || dtoConToken.getToken().length() > 20) {
            errores.add("El token debe tener entre 4 y 20 caracteres");
        }
        
        // 2. Extraer QhDto y validar el resto
        if (dtoConToken.getEmisor() == null && dtoConToken.getConversacion() == null) {
            errores.add("No hay datos de mensaje");
        } else {
            QhDto dto = new QhDto();
            dto.setEmisor(dtoConToken.getEmisor());
            dto.setConversacion(dtoConToken.getConversacion());
            
            errores.addAll(validarQhDto(dto));
        }
        
        return errores;
    }

    /* Valida el QhDto (sin token) - para el resto de endpoints */
    public List<String> validarQhDto(QhDto dto) {
        List<String> errores = new ArrayList<>();
        
        if (dto == null) {
            errores.add("El DTO no puede ser null");
            return errores;
        }
        
        // Validar emisor
        if (dto.getEmisor() == null) {
            errores.add("Los datos del emisor son obligatorios");
        } else {
            errores.addAll(validarEmisor(dto.getEmisor()));
        }
        
        // Validar conversación/mensaje
        if (dto.getConversacion() == null) {
            errores.add("Los datos de la conversación son obligatorios");
        } else if (dto.getConversacion().getMensajes() == null || 
                   dto.getConversacion().getMensajes().isEmpty()) {
            errores.add("Debe incluir al menos un mensaje");
        } else {
            errores.addAll(validarMensaje(dto.getConversacion().getMensajes().get(0)));
        }
        
        return errores;
    }

    /*	Valida los datos del emisor */
    private List<String> validarEmisor(QhEmisorDto emisor) {
        List<String> errores = new ArrayList<>();
        
        // Validar categoría (OBLIGATORIA)
        if (emisor.getTarjeta() == null || emisor.getTarjeta().trim().isEmpty()) {
            errores.add("La categoría es obligatoria");
        } else if (!CATEGORIAS_VALIDAS.contains(emisor.getTarjeta())) {
            errores.add("Categoría no válida. Debe ser: Bullying, Académico, Emocional, Otro");
        }

        // Solo validar longitud si vienen
        if (emisor.getCurso() != null && emisor.getCurso().length() > 10) {
            errores.add("El curso no puede exceder 10 caracteres");
        }
        
        if (emisor.getGrupo() != null && emisor.getGrupo().length() > 20) {
            errores.add("El grupo no puede exceder 20 caracteres");
        }
        
        return errores;
    }

    /* Valida el mensaje */
    private List<String> validarMensaje(QhMensajeDto mensaje) {
        List<String> errores = new ArrayList<>();
        
        if (mensaje == null) {
            errores.add("El mensaje no puede ser null");
            return errores;
        }
        
        // Validar contenido del mensaje (OBLIGATORIO)
        if (mensaje.getMensaje() == null || mensaje.getMensaje().trim().isEmpty()) {
            errores.add("El mensaje no puede estar vacío");
        } else if (mensaje.getMensaje().length() < 5) {
            errores.add("El mensaje debe tener al menos 5 caracteres");
        } else if (mensaje.getMensaje().length() > 2000) {
            errores.add("El mensaje no puede exceder los 2000 caracteres");
        }
        
        return errores;
    }

    /* Valida IDs para búsquedas */
    public void validarId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido");
        }
    }
}