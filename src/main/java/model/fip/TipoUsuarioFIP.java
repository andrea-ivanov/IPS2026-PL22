package model.fip;

/**
 * Tipo de usuario participante en una FIP (HU 37046):
 * - ALUMNO: solo puede matricularse si tiene un contrato registrado vigente en la fecha de inicio de la FIP.
 * - PROFESOR: profesor de plantilla, puede matricularse directamente (salvo que sea el responsable de la FIP).
 * - FORMADOR_EXTERNO: experto externo, solo imparte FIP y no puede matricularse como asistente.
 */
public enum TipoUsuarioFIP {
	ALUMNO,
	PROFESOR,
	FORMADOR_EXTERNO;

	public boolean esMatriculable() {
		return this != FORMADOR_EXTERNO;
	}
}
