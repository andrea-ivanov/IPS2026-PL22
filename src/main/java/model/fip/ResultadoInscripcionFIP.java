package model.fip;

/**
 * Resultado de la inscripción en una FIP (HU 37046):
 * - Al matricular, la inscripción queda con resultado PENDIENTE hasta que la califique el profesor responsable.
 * - Posteriormente el responsable califica como APTO o NO_APTO.
 */
public enum ResultadoInscripcionFIP {
	PENDIENTE,
	APTO,
	NO_APTO
}
