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
    
    // Contar mensajes no leídos en una conversación
    long countByConversacionIdAndLeidoFalse(Long conversacionId);
    
    // Marcar mensajes como leídos
    @Modifying //Update
    @Transactional
    @Query("UPDATE QhMensajeEntity m SET m.leido = true WHERE m.conversacion.id = :conversacionId AND m.emisor = 'profesor'")
    void marcarMensajesProfesorComoLeidos(@Param("conversacionId") Long conversacionId);
    
    @Modifying
    @Transactional
    @Query("UPDATE QhMensajeEntity m SET m.leido = true WHERE m.conversacion.id = :conversacionId AND m.emisor = 'alumno'")
    void marcarMensajesAlumnoComoLeidos(@Param("conversacionId") Long conversacionId);
}