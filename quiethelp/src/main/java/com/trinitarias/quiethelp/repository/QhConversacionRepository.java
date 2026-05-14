package com.trinitarias.quiethelp.repository;

import com.trinitarias.quiethelp.entity.QhConversacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface QhConversacionRepository extends JpaRepository<QhConversacionEntity, Long> {
    
    // Buscar por estado (para dashboard del profesor)
    List<QhConversacionEntity> findByEstado(String estado);
    
    // Buscar por estado y tarjeta (filtros)
    List<QhConversacionEntity> findByEstadoAndTarjeta(String estado, String tarjeta);
    
    // Buscar urgentes
    List<QhConversacionEntity> findByUrgenteTrue();
    
    // Buscar por revisor (conversaciones asignadas a un profesor)
    List<QhConversacionEntity> findByRevisorId(String revisorId);
    
    // Contar por estado (para las cards de resumen)
    long countByEstado(String estado);
    
    // Contar po urgente
    long countByUrgenteTrue();
    
    // Query personalizada para dashboard con filtros múltiples
    @Query("SELECT c FROM QhConversacionEntity c WHERE " +
           "(:estado IS NULL OR c.estado = :estado) AND " +
           "(:tarjeta IS NULL OR c.tarjeta = :tarjeta) AND " +
           "(:urgente IS NULL OR c.urgente = :urgente)")
    List<QhConversacionEntity> filtrarConversaciones(
            @Param("estado") String estado,
            @Param("tarjeta") String tarjeta,
            @Param("urgente") Boolean urgente);
<<<<<<< HEAD
=======
    
 // Buscar conversaciones por token del alumno (todos los estados)
    List<QhConversacionEntity> findByToken(String token);

    // Buscar conversaciones por token excluyendo un estado específico
    @Query("SELECT c FROM QhConversacionEntity c WHERE c.token = :token AND c.estado != :estado")
    List<QhConversacionEntity> findByTokenAndEstadoNot(@Param("token") String token, @Param("estado") String estado);
>>>>>>> back-jesus
}