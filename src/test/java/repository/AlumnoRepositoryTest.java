package repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import util.Database;

/**
 * Pruebas de la lista de alumnos del login. Usa sus propios alumnos (no se vacía la tabla: si Alumno
 * quedara vacía, Database volvería a cargar data.sql al crear el repositorio).
 */
class AlumnoRepositoryTest {
	private static Database db = new Database();

	@BeforeEach
	void setUp() {
		db.executeBatch(new String[] {
				"DELETE FROM Alumno WHERE alumnoId IN ('AL9998', 'AL9999')",
				"INSERT INTO Alumno VALUES ('AL9998','29999998A','Alejandro','Garcia Lopez','prueba.agarcia@alumno.es')",
				"INSERT INTO Alumno VALUES ('AL9999','29999999B','Alejandro','Perez Diaz','prueba.aperez@alumno.es')",
		});
	}

	@Test
	void laListaDelLoginIncluyeNombreYApellidos() {
		List<String> lista = new ArrayList<>();
		for (Object[] fila : new AlumnoRepository().getListaAlumnos()) {
			lista.add(fila[0].toString());
		}
		// Con los apellidos se distinguen los alumnos con el mismo nombre
		assertTrue(lista.contains("UO9998 - Alejandro Garcia Lopez"), lista.toString());
		assertTrue(lista.contains("UO9999 - Alejandro Perez Diaz"), lista.toString());
		// Ordenada por alumno
		assertTrue(lista.indexOf("UO9998 - Alejandro Garcia Lopez") < lista.indexOf("UO9999 - Alejandro Perez Diaz"));
	}
}
