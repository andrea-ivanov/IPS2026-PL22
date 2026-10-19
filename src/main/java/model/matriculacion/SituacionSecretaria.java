package model.matriculacion;

/**
 * Situaciones que revisa el secretario en el listado de matrículas (HU 35229).
 * Cada una equivale a una de las comprobaciones de MatriculaSecretariaDTO.
 */
public enum SituacionSecretaria {
	/** Sin filtrar por situación */
	TODAS,
	/** Pagada por transferencia o carta de pago y pendiente de validar (isPagoPendienteValidar) */
	PAGO_PENDIENTE,
	/** Solicitada sin ningún pago con el año académico ya iniciado (isSinPagarAnioIniciado) */
	SIN_PAGAR,
	/** Pagada o activa a la que le falta algún grupo por asignar (isFaltanGrupos) */
	FALTAN_GRUPOS
}
