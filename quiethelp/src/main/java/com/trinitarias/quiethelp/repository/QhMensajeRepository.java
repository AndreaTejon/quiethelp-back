package com.trinitarias.quiethelp.repository;

import com.trinitarias.quiethelp.entity.QhMensajeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QhMensajeRepository extends JpaRepository<QhMensajeEntity, Long> {
    
    // Buscar mensajes por conversación
    List<QhMensajeEntity> findByConversacionId(Long conversacionId);
    
    // Contar mensajes no leídos en una conversación
    long countByConversacionIdAndLeidoFalse(Long conversacionId);
    
    // Marcar mensajes como leídos
    @Query("UPDATE QhMensajeEntity m SET m.leido = true WHERE m.conversacion.id = :conversacionId AND m.emisor = 'profesor'")
    void marcarMensajesProfesorComoLeidos(@Param("conversacionId") Long conversacionId);
}