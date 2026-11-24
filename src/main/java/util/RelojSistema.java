package util;

import java.time.LocalDate;

/**
 * Reloj global del sistema (Singleton) para centralizar la fecha actual.
 * Por defecto devuelve la fecha real del sistema ({@link LocalDate#now()}),
 * pero permite fijar una fecha simulada desde el menú inicial para probar
 * plazos, aperturas/cierres y transiciones automáticas sin cambiar la hora del equipo.
 */
public final class RelojSistema {
	private static final RelojSistema INSTANCIA = new RelojSistema();

	private LocalDate fechaSimulada;

	private RelojSistema() {
	}

	public static RelojSistema getInstance() {
		return INSTANCIA;
	}

	/** Fecha actual del sistema (simulada si se ha fijado una, o la real del equipo). */
	public LocalDate hoy() {
		return fechaSimulada != null ? fechaSimulada : LocalDate.now();
	}

	/** Fecha actual del sistema en formato ISO (AAAA-MM-DD). */
	public String hoyIso() {
		return hoy().toString();
	}

	/** Indica si actualmente se está usando una fecha simulada. */
	public boolean esSimulada() {
		return fechaSimulada != null;
	}

	/** Fija una fecha simulada (si es null, vuelve a usar la fecha real del equipo). */
	public void fijarFecha(LocalDate fecha) {
		this.fechaSimulada = fecha;
	}

	/** Restablece el reloj a la fecha real del ordenador. */
	public void restablecerFechaReal() {
		this.fechaSimulada = null;
	}
}
