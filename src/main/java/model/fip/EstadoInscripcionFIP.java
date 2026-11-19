package model.fip;

/**
 * Estado administrativo de la inscripción en una FIP (HU 37046):
 * - ACTIVA: matrícula en vigor.
 * - ANULADA: secretaría puede anular una inscripción antes de que empiece la FIP.
 */
public enum EstadoInscripcionFIP {
	ACTIVA,
	ANULADA
}
