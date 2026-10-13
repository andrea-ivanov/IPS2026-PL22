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
		db.executeBatchNoFail(new String[] {
				"DELETE FROM Calificacion",
				"DELETE FROM MatriculaGrupo",
				"DELETE FROM MatriculaCurso",
				"DELETE FROM Matricula",
				"DELETE FROM Acta",
				"DELETE FROM Convocatoria",
				"DELETE FROM Imparte",
				"DELETE FROM Grupo",
				"DELETE FROM PropuestaCurso",
				"DELETE FROM GradoCurso",
				"DELETE FROM Curso",
				"DELETE FROM Grado",
				"DELETE FROM AnioAcademico",
				"DELETE FROM Profesor",
				"DELETE FROM Alumno",
				"DELETE FROM AnoAcademico",
				"CREATE TABLE IF NOT EXISTS AnoAcademico (" +
						"id_ano_academico INTEGER PRIMARY KEY, " +
						"nombre VARCHAR(50), " +
						"inicio_ano_academico DATE, " +
						"fin_ano_academico DATE, " +
						"inicio_periodo_matriculacion DATE, " +
						"fin_periodo_matriculacion DATE, " +
						"actual INTEGER DEFAULT 1)"
		});

		db.executeBatch(new String[] {
				// 1. Datos de Alumnos
				"INSERT INTO Alumno (alumnoId, dni, nombre, apellidos, email) VALUES ('1', '10000001A', 'Juan Perez', '', 'juan@uni.es')",
				"INSERT INTO Alumno (alumnoId, dni, nombre, apellidos, email) VALUES ('2', '10000002B', 'Maria Gomez', '', 'maria@uni.es')",

				// 2. Datos de Profesores
				"INSERT INTO Profesor (profesorId, dni, nombre, apellidos, email) VALUES ('1', '10000003C', 'Profesor Martinez', '', 'martinez@uni.es')",
				"INSERT INTO Profesor (profesorId, dni, nombre, apellidos, email) VALUES ('2', '10000004D', 'Profesora Lopez', '', 'lopez@uni.es')",

				// 3. Datos de Año Académico (AnioAcademico oficial y AnoAcademico compatibilidad)
				"INSERT INTO AnioAcademico (anioAcademicoId, fechaInicio, fechaFin, inicioMatriculacion, finMatriculacion) VALUES ('1', '2026-09-01', '2027-06-30', '2026-07-01', '2026-08-31')",
				"INSERT INTO AnioAcademico (anioAcademicoId, fechaInicio, fechaFin, inicioMatriculacion, finMatriculacion) VALUES ('2', '2027-09-01', '2028-06-30', '2027-07-01', '2027-08-31')",
				"INSERT INTO AnioAcademico (anioAcademicoId, fechaInicio, fechaFin, inicioMatriculacion, finMatriculacion) VALUES ('2026/2027', '2026-09-01', '2027-06-30', '2026-07-01', '2026-08-31')",
				"INSERT INTO AnoAcademico (id_ano_academico, nombre, inicio_ano_academico, fin_ano_academico, inicio_periodo_matriculacion, fin_periodo_matriculacion, actual) VALUES (1, '2026/2027', '2026-09-01', '2027-06-30', '2026-07-01', '2026-08-31', 1)",

				// 4. Datos de Grados
				"INSERT INTO Grado (gradoId, nombre, facultad, ramaConocimiento, estado, fechaInicioVigencia) VALUES ('1', 'Ingeniería Informática', 'Escuela de Ingeniería Informática', 'Ingeniería y Arquitectura', 'VIGENTE', '2024-09-01')",
				"INSERT INTO Grado (gradoId, nombre, facultad, ramaConocimiento, estado, fechaInicioVigencia) VALUES ('2', 'Ingeniería de Telecomunicaciones', 'Escuela de Ingeniería', 'Ingeniería y Arquitectura', 'VIGENTE', '2024-09-01')",

				// 5. Datos de Cursos
				"INSERT INTO Curso (cursoId, coordinadorId, nombre, creditos) VALUES ('1', '1', 'Programación', 6)",
				"INSERT INTO Curso (cursoId, coordinadorId, nombre, creditos) VALUES ('2', '2', 'Bases de Datos', 6)",

				// 6. Cursos asignados a Grado
				"INSERT INTO GradoCurso (gradoId, cursoId, anio, itinerario, estado) VALUES ('1', '1', 1, 'General', 'ACTIVO')",
				"INSERT INTO GradoCurso (gradoId, cursoId, anio, itinerario, estado) VALUES ('1', '2', 1, 'General', 'ACTIVO')",

				// 7. Datos de Matrículas
				"INSERT INTO Matricula (matriculaId, alumnoId, gradoId, anioAcademicoId, fechaSolicitud, estado, motivoCancelacion, fechaCancelacion) VALUES ('1', '1', '1', '1', '2026-09-01', 'ACTIVA', NULL, NULL)",
				"INSERT INTO Matricula (matriculaId, alumnoId, gradoId, anioAcademicoId, fechaSolicitud, estado, motivoCancelacion, fechaCancelacion) VALUES ('2', '2', '2', '1', '2026-09-02', 'CANCELADA', 'IMPAGO', '2026-09-02')",

				// 8. Asignaturas de cada matrícula
				"INSERT INTO MatriculaCurso (matriculaId, gradoId, cursoId, estado, motivoCancelacion, fechaCancelacion, importe, metodoPago, fechaPago, pagoValidado) VALUES ('1', '1', '1', 'ACTIVA', NULL, NULL, 120, 'TARJETA', '2026-09-01', 1)",
				"INSERT INTO MatriculaCurso (matriculaId, gradoId, cursoId, estado, motivoCancelacion, fechaCancelacion, importe, metodoPago, fechaPago, pagoValidado) VALUES ('1', '1', '2', 'ACTIVA', NULL, NULL, 120, 'TARJETA', '2026-09-01', 1)"
		});
	}
}
