package util.exception;

import util.ApplicationException;

/**
 * Excepción lanzada cuando un recurso o entidad solicitada por su identificador
 * no existe en la base de datos o sistema.
 */
public class ResourceNotFoundException extends ApplicationException {
	private static final long serialVersionUID = 1L;

	public ResourceNotFoundException(String message) {
		super(message);
	}

	public ResourceNotFoundException(String resourceName, Object id) {
		super(String.format("No se ha encontrado el recurso '%s' con identificador: %s", resourceName, id));
	}
}

