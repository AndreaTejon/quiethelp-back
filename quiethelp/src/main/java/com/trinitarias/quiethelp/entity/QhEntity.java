package com.trinitarias.quiethelp.entity;

import com.trinitarias.quiethelp.dto.QhDto;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="quiethelp")
public class QhEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	private String curso,grupo,tarjeta,mensaje;
	
	public QhEntity () {}
	
	public QhEntity(long id, String curso, String grupo, String tarjeta, String mensaje) {
		super();
		this.id = id;
		this.curso = curso;
		this.grupo = curso;
		this.tarjeta = tarjeta;
		this.mensaje = mensaje;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getCurso() {
		return curso;
	}

	public void setCurso(String curso) {
		this.curso = curso;
	}

	public String getGrupo() {
		return grupo;
	}

	public void setGrupo(String grupo) {
		this.grupo = grupo;
	}

	public String getTarjeta() {
		return tarjeta;
	}

	public void setTarjeta(String tarjeta) {
		this.tarjeta = tarjeta;
	}

	public String getMensaje() {
		return mensaje;
	}

	public void setMensaje(String mensaje) {
		this.mensaje = mensaje;
	}
	
	public static QhEntity fromDtoToEntity(QhDto dto) {
		QhEntity et = new QhEntity();
		et.setCurso(dto.getEmisor().getCurso());
		et.setGrupo(dto.getEmisor().getGrupo());
		et.setTarjeta(dto.getEmisor().getTarjeta());
		et.setMensaje(dto.getMensaje().getMensaje());
		return et;
	}
	
	
}
