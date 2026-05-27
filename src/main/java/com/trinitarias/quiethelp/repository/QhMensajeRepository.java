package com.trinitarias.quiethelp.repository;

import com.trinitarias.quiethelp.entity.QhMensajeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface QhMensajeRepository extends JpaRepository<QhMensajeEntity, Long> {
    
    // Buscar mensajes por conversación
    List<QhMensajeEntity> findByConversacionId(Long conversacionId);
    
    // Buscar mensajes por conversación en orden para blockchain
    List<QhMensajeEntity> findByConversacionIdOrderById(Long conversacionId);
    
    // Contar mensajes no leídos en una conversación
    long countByConversacionIdAndLeidoFalse(Long conversacionId);
    
    // Contar mensajes no leídos del alumno en una conversación específica
    @Query("SELECT COUNT(m) FROM QhMensajeEntity m WHERE m.conversacion.id = :conversacionId AND m.emisor = 'alumno' AND m.leido = false")
    int countMensajesAlumnoNoLeidos(@Param("conversacionId") Long conversacionId);
    
    // Marcar mensajes del profesor como leídos (solo los no leídos)
    @Modifying
    @Transactional
    @Query("UPDATE QhMensajeEntity m SET m.leido = true WHERE m.conversacion.id = :conversacionId AND m.emisor = 'profesor' AND m.leido = false")
    int marcarMensajesProfesorComoLeidos(@Param("conversacionId") Long conversacionId);
    
    // Marcar mensajes del alumno como leídos (solo los no leídos) - DEVUELVE int
    @Modifying
    @Transactional
    @Query("UPDATE QhMensajeEntity m SET m.leido = true WHERE m.conversacion.id = :conversacionId AND m.emisor = 'alumno' AND m.leido = false")
    int marcarMensajesAlumnoComoLeidos(@Param("conversacionId") Long conversacionId);
}