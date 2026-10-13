package util.exception;

import util.ApplicationException;

/**
 * Excepción lanzada cuando los parámetros de entrada no cumplen con las reglas
 * de validación o presentan un formato incorrecto (Bad Request).
 */
public class ValidationException extends ApplicationException {
	private static final long serialVersionUID = 1L;

	public ValidationException(String message) {
		super(message);
	}

	public ValidationException(Throwable cause) {
		super(cause);
	}
}

