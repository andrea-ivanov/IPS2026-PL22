package model.matriculacion;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO que consolida toda la información de una solicitud de matrícula
 * antes de confirmar su registro y pago en el sistema.
 */
public class ResumenSolicitudDTO {
	private String alumnoId;
	private String dni;
	private String nombreCompleto;
	private String email;
	private String gradoId;
	private String nombreGrado;
	private String anioAcademicoId;
	private String fechaSolicitud;
	private List<OfertaCursoDTO> asignaturas;
	private double totalCreditos;
	private double importeTotal;
	private String metodoPago; // "TARJETA", "TRANSFERENCIA", "CARTA_PAGO"

	public ResumenSolicitudDTO() {
		this.asignaturas = new ArrayList<>();
		this.totalCreditos = 0.0;
		this.importeTotal = 0.0;
	}

	public String getAlumnoId() {
		return alumnoId;
	}

	public void setAlumnoId(String alumnoId) {
		this.alumnoId = alumnoId;
	}

	public String getDni() {
		return dni;
	}

	public void setDni(String dni) {
		this.dni = dni;
	}

	public String getNombreCompleto() {
		return nombreCompleto;
	}

	public void setNombreCompleto(String nombreCompleto) {
		this.nombreCompleto = nombreCompleto;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getGradoId() {
		return gradoId;
	}

	public void setGradoId(String gradoId) {
		this.gradoId = gradoId;
	}

	public String getNombreGrado() {
		return nombreGrado;
	}

	public void setNombreGrado(String nombreGrado) {
		this.nombreGrado = nombreGrado;
	}

	public String getAnioAcademicoId() {
		return anioAcademicoId;
	}

	public void setAnioAcademicoId(String anioAcademicoId) {
		this.anioAcademicoId = anioAcademicoId;
	}

	public String getFechaSolicitud() {
		return fechaSolicitud;
	}

	public void setFechaSolicitud(String fechaSolicitud) {
		this.fechaSolicitud = fechaSolicitud;
	}

	public List<OfertaCursoDTO> getAsignaturas() {
		return asignaturas;
	}

	public void setAsignaturas(List<OfertaCursoDTO> asignaturas) {
		this.asignaturas = asignaturas != null ? asignaturas : new ArrayList<>();
		recalcularTotales();
	}

	public void addAsignatura(OfertaCursoDTO curso) {
		if (this.asignaturas == null) {
			this.asignaturas = new ArrayList<>();
		}
		this.asignaturas.add(curso);
		recalcularTotales();
	}

	public double getTotalCreditos() {
		return totalCreditos;
	}

	public double getImporteTotal() {
		return importeTotal;
	}

	public String getMetodoPago() {
		return metodoPago;
	}

	public void setMetodoPago(String metodoPago) {
		this.metodoPago = metodoPago;
	}

	/**
	 * Recalcula el total de créditos e importe total (20 euros/crédito)
	 * a partir de la lista de asignaturas asignadas a la solicitud.
	 */
	public void recalcularTotales() {
		double creditos = 0.0;
		double importe = 0.0;
		if (asignaturas != null) {
			for (OfertaCursoDTO c : asignaturas) {
				creditos += c.getCreditos();
				importe += c.getPrecio();
			}
		}
		this.totalCreditos = creditos;
		this.importeTotal = importe;
	}
}

