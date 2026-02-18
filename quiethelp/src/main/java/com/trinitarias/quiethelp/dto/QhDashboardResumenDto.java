package com.trinitarias.quiethelp.dto;

public class QhDashboardResumenDto {
	  	private long pendientes;
	    private long enRevision;
	    private long resueltos;
	    private long urgentes;
	    
	    public QhDashboardResumenDto () {}
	    
	    public QhDashboardResumenDto(long pendientes, long enRevision, long resueltos, long urgentes) {
	    	this.pendientes = pendientes;
	    	this.enRevision = enRevision;
	    	this.resueltos = resueltos;
	    	this.urgentes = urgentes;
	    }
	    
	    public long getPendientes() {
	        return pendientes;
	    }
	    
	    public void setPendientes(long pendientes) {
	        this.pendientes = pendientes;
	    }
	    
	    public long getEnRevision() {
	        return enRevision;
	    }
	    
	    public void setEnRevision(long enRevision) {
	        this.enRevision = enRevision;
	    }
	    
	    public long getResueltos() {
	        return resueltos;
	    }
	    
	    public void setResueltos(long resueltos) {
	        this.resueltos = resueltos;
	    }
	    
	    public long getUrgentes() {
	        return urgentes;
	    }
	    
	    public void setUrgentes(long urgentes) {
	        this.urgentes = urgentes;
	    }
}
