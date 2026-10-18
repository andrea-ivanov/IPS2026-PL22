package model.matriculacion;

/**
 * Grupo de un curso con sus plazas ocupadas en un año académico y si está asignado
 * a una matrícula concreta (HU 35229, asignación de grupos por el secretario).
 *
 * Sin Lombok a propósito: el compilador de VS Code/Eclipse generaba clases corruptas.
 */
public class GrupoPlazasDTO {
	private String grupoCodigo;
	private String tipo;
	private int capacidad;
	private int ocupadas; // Matrículas PAGADAS o ACTIVAS del mismo año en este grupo
	private int asignado; // 1 si la matrícula consultada ya está en este grupo

	public GrupoPlazasDTO() {
		super();
	}

	public int getPlazasLibres() {
		return capacidad - ocupadas;
	}

	/** No se llama isAsignado para no chocar con getAsignado/setAsignado, que usa DbUtils para mapear la columna. */
	public boolean estaAsignado() {
		return asignado == 1;
	}

	public String getGrupoCodigo() { return grupoCodigo; }
	public void setGrupoCodigo(String grupoCodigo) { this.grupoCodigo = grupoCodigo; }

	public String getTipo() { return tipo; }
	public void setTipo(String tipo) { this.tipo = tipo; }

	public int getCapacidad() { return capacidad; }
	public void setCapacidad(int capacidad) { this.capacidad = capacidad; }

	public int getOcupadas() { return ocupadas; }
	public void setOcupadas(int ocupadas) { this.ocupadas = ocupadas; }

	public int getAsignado() { return asignado; }
	public void setAsignado(int asignado) { this.asignado = asignado; }

	@Override
	public String toString() {
		return grupoCodigo + " (" + tipo + ", " + getPlazasLibres() + " plazas libres)";
	}
}
