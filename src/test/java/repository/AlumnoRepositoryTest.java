package repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import model.fixtures.BaseModelTest;

/**
 * Pruebas de la lista de alumnos del login. Añade sus propios alumnos a la carga limpia de BaseModelTest.
 */
class AlumnoRepositoryTest extends BaseModelTest {

	@BeforeEach
	void insertarAlumnos() {
		db.executeBatch(new String[] {
				"INSERT INTO Alumno VALUES ('AL9998','29999998A','Alejandro','Garcia Lopez','prueba.agarcia@alumno.es')",
				"INSERT INTO Alumno VALUES ('AL9999','29999999B','Alejandro','Perez Diaz','prueba.aperez@alumno.es')",
		});
	}

	@Test
	void laListaDelLoginIncluyeNombreYApellidos() {
		List<String> lista = new ArrayList<>();
		for (Object[] fila : new AlumnoRepository(db).getListaAlumnos()) {
			lista.add(fila[0].toString());
		}
		// Con los apellidos se distinguen los alumnos con el mismo nombre
		assertTrue(lista.contains("UO9998 - Alejandro Garcia Lopez"), lista.toString());
		assertTrue(lista.contains("UO9999 - Alejandro Perez Diaz"), lista.toString());
		// Ordenada por alumno
		assertTrue(lista.indexOf("UO9998 - Alejandro Garcia Lopez") < lista.indexOf("UO9999 - Alejandro Perez Diaz"));
	}
}
