package model.matriculacion;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import model.fixtures.BaseModelTest;

/**
 * Pruebas de la HU 35232: el estudiante consulta sus matrículas, sus calificaciones y su permanencia.
 * Parte de la carga limpia de BaseModelTest (alumno 1 matriculado en el grado 1 el año 1 de los cursos 1 y 2)
 * y añade:
 * - Año 1: suspenso (4) en el curso 1 y aprobado (7) en el curso 2, que tiene 6 créditos.
 * - Año 2: el alumno 1 repite el curso 1 (segunda matrícula), todavía sin notas.
 * - Alumno 3 registrado sin ninguna matrícula, y alumnos AL0005 y UO0007 para la conversión UO/AL.
 */
public class MatriculacionConsultaModelTest extends BaseModelTest {
	private MatriculacionModel model;

	@BeforeEach
	public void initModel() {
		db.executeBatch(new String[] {
				"INSERT INTO Convocatoria VALUES ('1', 'ORDINARIA_ENERO', '2027-01-07', '2027-01-31')",
				"INSERT INTO Acta VALUES ('1', '1', 'ORDINARIA_ENERO', 'CERRADA', '2027-02-01', 0)",
				"INSERT INTO Acta VALUES ('2', '1', 'ORDINARIA_ENERO', 'CERRADA', '2027-02-01', 0)",
				"INSERT INTO Calificacion VALUES ('1', '1', '1', 'ORDINARIA_ENERO', 4.0)",
				"INSERT INTO Calificacion VALUES ('1', '2', '1', 'ORDINARIA_ENERO', 7.0)",
				"INSERT INTO Matricula VALUES ('3', '1', '1', '2', '2027-07-10', 'ACTIVA', NULL, NULL)",
				"INSERT INTO MatriculaCurso VALUES ('3', '1', '1', 'ACTIVA', NULL, NULL, 120, 'TARJETA', '2027-07-10', 1)",
				"INSERT INTO Alumno VALUES ('3', '10000009Z', 'Sin', 'Matriculas', 'sin@uni.es')",
				"INSERT INTO Alumno VALUES ('AL0005', '10000005E', 'Ana', 'Lopez', 'ana@uni.es')",
				"INSERT INTO Alumno VALUES ('UO0007', '10000007G', 'Luis', 'Diaz', 'luis@uni.es')" });
		model = new MatriculacionModel(db);
	}

	@Test
	@DisplayName("Asignaturas matriculadas: la que se repite tiene número de matrícula 2 y sale primero")
	public void testMatriculasPorGrado_NumeroDeMatricula() {
		List<MatriculacionDisplayDTO> filas = model.getMatriculasPorEstudianteYGrado("1", "1");

		assertEquals(3, filas.size());
		MatriculacionDisplayDTO repetida = filas.get(0);
		assertAll("Curso 1 repetido en el año 2",
				() -> assertEquals("3", repetida.getId()),
				() -> assertEquals("Programación", repetida.getCurso()),
				() -> assertEquals("2", repetida.getAnioAcademicoId()),
				() -> assertEquals(2, repetida.getNumMatricula()));
		assertTrue(filas.stream().anyMatch(f -> "1".equals(f.getId()) && "Programación".equals(f.getCurso())
				&& f.getNumMatricula() == 1), "La primera matrícula del curso 1 cuenta como 1");
	}

	@Test
	@DisplayName("Calificaciones: una por convocatoria, con su resultado")
	public void testCalificaciones() {
		List<CalificacionDisplayDTO> notas = model.getCalificacionesPorEstudianteYGrado("1", "1");

		assertEquals(2, notas.size());
		assertAll("Ordenadas por asignatura dentro del año",
				() -> assertEquals("Bases de Datos", notas.get(0).getCurso()),
				() -> assertEquals("APROBADO", notas.get(0).getResultado()),
				() -> assertEquals("Programación", notas.get(1).getCurso()),
				() -> assertEquals("SUSPENSO", notas.get(1).getResultado()));
	}

	@Test
	@DisplayName("Grados del estudiante para el desplegable")
	public void testGradosDelEstudiante() {
		List<Object[]> grados = model.getGradosPorEstudiante("1");

		assertEquals(1, grados.size());
		assertEquals("1 - Ingeniería Informática", grados.get(0)[0]);
	}

	@Test
	@DisplayName("Alumno sin matrículas: listas vacías, sin datos de ejemplo de otro alumno")
	public void testAlumnoSinMatriculas() {
		assertAll(
				() -> assertTrue(model.getGradosPorEstudiante("3").isEmpty()),
				() -> assertTrue(model.getMatriculasPorEstudiante("3").isEmpty()),
				() -> assertTrue(model.getMatriculasPorEstudianteYGrado("3", null).isEmpty()),
				() -> assertTrue(model.getCalificacionesPorEstudianteYGrado("3", null).isEmpty()));
	}

	@Test
	@DisplayName("Permanencia: el año en curso sin notas se salta y se evalúa el último año con notas")
	public void testHitosPermanencia_SaltaElAnioEnCurso() {
		HitosPermanenciaDTO hitos = model.getHitosPermanencia("1", "1");

		assertAll(
				() -> assertEquals("1", hitos.getUltimoAnoEvaluadoId(), "El año 2 está ACTIVO y sin créditos"),
				() -> assertEquals(6, hitos.getCreditosAnualesSuperados()),
				() -> assertFalse(hitos.isCumpleHitoAnual(), "6 créditos no llegan a los 12 exigidos"),
				() -> assertEquals(2, hitos.getAniosEnGrado()),
				() -> assertEquals(6, hitos.getCreditosTotalesSuperados()),
				() -> assertTrue(hitos.isCumpleHitoTercerAnio(), "Con menos de 3 años todavía no se exige"));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@CsvSource({
			"UO0005, AL0005",
			"'UO0005 - Ana Lopez', AL0005",
			"AL0007, UO0007",
			"1, 1",
			"UO9999, UO9999" })
	@DisplayName("resolverAlumnoId: el UO del login se traduce al id de la tabla Alumno")
	public void testResolverAlumnoId(String entrada, String esperado) {
		assertEquals(esperado, model.resolverAlumnoId(entrada));
	}
}
