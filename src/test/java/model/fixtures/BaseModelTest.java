package model.fixtures;

import org.junit.jupiter.api.BeforeEach;

import util.Database;

/**
 * Clase base para pruebas unitarias de modelos y casos de uso.
 * Sigue la metodología de pruebas de GIIS (TestInscripcion):
 * - Garantiza que el esquema esté creado.
 * - Restablece una base de datos limpia y predecible antes de cada test.
 */
public abstract class BaseModelTest {
	protected static Database db = new Database();

	@BeforeEach
	public void setUp() {
		db.createDatabase(true);
		loadCleanDatabase(db);
	}

	public static void loadCleanDatabase(Database db) {
		db.executeBatch(new String[] {
				"DELETE FROM Profesor_Curso",
				"DELETE FROM Matricula",
				"DELETE FROM Curso",
				"DELETE FROM Grado",
				"DELETE FROM Alumno",
				"DELETE FROM Profesor",
				"DELETE FROM Administrador",
				"DELETE FROM Secretario",

				// Datos de Alumnos
				"INSERT INTO Alumno (id_alumno, nombre, email) VALUES (1, 'Juan Perez', 'juan@uni.es')",
				"INSERT INTO Alumno (id_alumno, nombre, email) VALUES (2, 'Maria Gomez', 'maria@uni.es')",

				// Datos de Grados
				"INSERT INTO Grado (id_grado, nombre) VALUES (1, 'Ingeniería Informática')",
				"INSERT INTO Grado (id_grado, nombre) VALUES (2, 'Ingeniería de Telecomunicaciones')",

				// Datos de Profesores
				"INSERT INTO Profesor (id_profesor, nombre, email) VALUES (1, 'Profesor Martinez', 'martinez@uni.es')",
				"INSERT INTO Profesor (id_profesor, nombre, email) VALUES (2, 'Profesora Lopez', 'lopez@uni.es')",

				// Datos de Cursos
				"INSERT INTO Curso (id_curso, nombre, id_grado, id_coordinador) VALUES (1, 'Programación', 1, 1)",
				"INSERT INTO Curso (id_curso, nombre, id_grado, id_coordinador) VALUES (2, 'Bases de Datos', 1, 2)",

				// Datos de Matrículas
				"INSERT INTO Matricula (id_matricula, id_alumno, id_grado, estado, fecha) VALUES (1, 1, 1, 'Activa', '2026-09-01')",
				"INSERT INTO Matricula (id_matricula, id_alumno, id_grado, estado, fecha) VALUES (2, 2, 2, 'Cancelada', '2026-09-02')",

				// Profesores que imparten cursos
				"INSERT INTO Profesor_Curso (id_profesor, id_curso) VALUES (1, 1)",
				"INSERT INTO Profesor_Curso (id_profesor, id_curso) VALUES (2, 2)"
		});
	}
}

