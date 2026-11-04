package util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import model.fixtures.BaseModelTest;
import util.exception.ValidationException;

/**
 * Pruebas de DbUtil.enTransaccion: o se guardan todos los cambios o ninguno.
 */
public class DbUtilTest extends BaseModelTest {
	private static final String INSERT_PROFESOR =
			"INSERT INTO Profesor VALUES ('P9', '99999999Z', 'Prueba', 'Transaccion', 'p9@uni.es')";

	@Test
	@DisplayName("Sin errores: guarda los cambios y devuelve el resultado de la operación")
	public void testCommit() {
		int filas = db.enTransaccion(conn -> DbUtil.update(conn, INSERT_PROFESOR));

		assertEquals(1, filas);
		assertEquals(1, profesoresP9());
	}

	@Test
	@DisplayName("Error de validación después de escribir: deshace lo escrito y relanza el error")
	public void testRollbackConRuntimeException() {
		assertThrows(ValidationException.class, () -> db.enTransaccion(conn -> {
			DbUtil.update(conn, INSERT_PROFESOR);
			throw new ValidationException("fallo después de insertar");
		}));
		assertEquals(0, profesoresP9());
	}

	@Test
	@DisplayName("Error SQL después de escribir: deshace lo escrito y lanza UnexpectedException")
	public void testRollbackConSQLException() {
		assertThrows(UnexpectedException.class, () -> db.enTransaccion(conn -> {
			DbUtil.update(conn, INSERT_PROFESOR);
			return DbUtil.update(conn, "INSERT INTO TablaQueNoExiste VALUES (1)");
		}));
		assertEquals(0, profesoresP9());
	}

	private int profesoresP9() {
		return ((Number) db.executeQueryArray("SELECT COUNT(*) FROM Profesor WHERE profesorId = 'P9'").get(0)[0])
				.intValue();
	}
}
