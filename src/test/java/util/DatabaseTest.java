package util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pruebas de la comprobación de arranque de Database: solo crea y carga una BD sin tablas,
 * y nunca recrea una BD que ya tiene tablas (aunque estén vacías) ni una que no puede leer.
 */
public class DatabaseTest {
	@TempDir
	Path dir;

	@Test
	@DisplayName("BD sin tablas: ejecuta schema.sql y data.sql")
	public void testBaseDeDatosNueva() {
		Database db = new Database(url("nueva.db"));

		assertTrue(contar(db, "SELECT COUNT(*) FROM Grado") > 0, "Debe cargar los grados de data.sql");
		assertTrue(contar(db, "SELECT COUNT(*) FROM Alumno") > 0, "Debe cargar los alumnos de data.sql");
	}

	@Test
	@DisplayName("BD con tablas vacías: no se toca (antes se recargaba data.sql y se perdían los datos)")
	public void testBaseDeDatosConTablasVaciasNoSeRecarga() throws SQLException {
		String url = url("vacia.db");
		try (Connection conn = DriverManager.getConnection(url); Statement st = conn.createStatement()) {
			st.execute("CREATE TABLE Grado (gradoId TEXT)");
		}

		Database db = new Database(url);

		assertEquals(0, contar(db, "SELECT COUNT(*) FROM Grado"));
		assertEquals(1, contar(db, "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table'"),
				"No debe crear el resto de tablas");
	}

	@Test
	@DisplayName("BD que no se puede leer: lanza la excepción en vez de recrearla")
	public void testBaseDeDatosIlegible() {
		String url = "jdbc:sqlite:" + dir.resolve("carpeta-que-no-existe").resolve("x.db");

		assertThrows(ApplicationException.class, () -> new Database(url));
	}

	private String url(String fichero) {
		return "jdbc:sqlite:" + dir.resolve(fichero);
	}

	private static int contar(Database db, String sql) {
		List<Object[]> filas = db.executeQueryArray(sql);
		return ((Number) filas.get(0)[0]).intValue();
	}
}
