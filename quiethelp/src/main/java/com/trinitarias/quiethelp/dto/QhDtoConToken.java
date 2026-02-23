package com.trinitarias.quiethelp.dto;

public class QhDtoConToken { //Se crea SOLO para validar el token del alumno y no modificar la lógica de negocio
	//Importante por futuros cambios en la validación
	
    private String token;
    
    private QhEmisorDto emisor;
    
    private QhConversacionDto conversacion;
    
    public QhDtoConToken() {}
    
    public QhDtoConToken(String token, QhEmisorDto emisor, QhConversacionDto conversacion) {
        this.token = token;
        this.emisor = emisor;
        this.conversacion = conversacion;
    }
    
    // Getters y Setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public QhEmisorDto getEmisor() {
        return emisor;
    }
    
    public void setEmisor(QhEmisorDto emisor) {
        this.emisor = emisor;
    }
    
    public QhConversacionDto getConversacion() {
        return conversacion;
    }
    
    public void setConversacion(QhConversacionDto conversacion) {
        this.conversacion = conversacion;
    }
}