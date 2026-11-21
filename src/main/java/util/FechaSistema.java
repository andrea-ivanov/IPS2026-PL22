package util;

import java.time.LocalDate;

/**
 * Fecha "de hoy" de la aplicación. Por defecto es la fecha real, pero desde el login (Archivo &gt; Cambiar fecha
 * del sistema) se puede fijar otra para probar los procesos que dependen de la fecha. No se guarda: al volver a
 * arrancar la aplicación se usa otra vez la fecha real
 */
public final class FechaSistema {
	/** Fecha fijada por el usuario; nula si se usa la fecha real */
	private static LocalDate fechaSimulada;

	private FechaSistema() {
	}

	public static LocalDate hoy() {
		return fechaSimulada != null ? fechaSimulada : LocalDate.now();
	}

	/** Fecha de hoy en formato yyyy-MM-dd */
	public static String hoyStr() {
		return hoy().toString();
	}

	public static void setFecha(LocalDate fecha) {
		fechaSimulada = fecha;
	}

	/** Vuelve a usar la fecha real */
	public static void restablecer() {
		fechaSimulada = null;
	}

	public static boolean esSimulada() {
		return fechaSimulada != null;
	}
}
