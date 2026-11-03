package model.curso;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import model.fixtures.BaseModelTest;
import model.matriculacion.MatriculacionModel;
import util.exception.ValidationException;

/**
 * Pruebas unitarias para el alta y la baja de Cursos dentro de un Grado (HU 35223).
 * Año '1': los cursos 1 y 2 del grado 1 tienen matrículas activas. Año '2': sin matrículas.
 */
public class CursoModelGestionTest extends BaseModelTest {
	private CursoModel cursoModel;

	@BeforeEach
	public void initModel() {
		this.cursoModel = new CursoModel(db);
	}

	// =========================================================================
	// 1. AÑADIR CURSO
	// =========================================================================
	@Nested
	@DisplayName("1. Añadir curso")
	public class CrearCurso {

		@Test
		@DisplayName("TC-01 Happy path: crea el curso y lo asocia ACTIVO al grado")
		public void testCrearCurso_HappyPath() {
			String cursoId = cursoModel.crearCurso("1", "Redes", "6", "2", "1", "Software");

			assertEquals("C001", cursoId);
			List<Object[]> curso = db.executeQueryArray(
					"SELECT coordinadorId, nombre, creditos FROM Curso WHERE cursoId = ?", cursoId);
			List<Object[]> gradoCurso = db.executeQueryArray(
					"SELECT anio, itinerario, estado FROM GradoCurso WHERE gradoId = '1' AND cursoId = ?", cursoId);
			assertEquals(1, curso.size(), "Debe existir el curso creado");
			assertEquals(1, gradoCurso.size(), "Debe existir la asociación con el grado");
			assertAll("Campos del curso creado",
					() -> assertEquals("1", curso.get(0)[0]),
					() -> assertEquals("Redes", curso.get(0)[1]),
					() -> assertEquals(6.0, ((Number) curso.get(0)[2]).doubleValue()),
					() -> assertEquals(2, ((Number) gradoCurso.get(0)[0]).intValue()),
					() -> assertEquals("Software", gradoCurso.get(0)[1]),
					() -> assertEquals("ACTIVO", gradoCurso.get(0)[2]));
		}

		@ParameterizedTest(name = "nombre = ''{0}''")
		@NullAndEmptySource
		@ValueSource(strings = { "   ", "\t" })
		@DisplayName("TC-02 Falta el nombre: lanza ValidationException y no inserta")
		public void testCrearCurso_SinNombre(String valor) {
			assertThrows(ValidationException.class, () -> cursoModel.crearCurso("1", valor, "6", "2", "1", "Software"));
			assertSinAltas();
		}

		@ParameterizedTest(name = "créditos = ''{0}''")
		@NullAndEmptySource
		@ValueSource(strings = { "   ", "\t" })
		@DisplayName("TC-02 Faltan los créditos: lanza ValidationException y no inserta")
		public void testCrearCurso_SinCreditos(String valor) {
			assertThrows(ValidationException.class, () -> cursoModel.crearCurso("1", "Redes", valor, "2", "1", "Software"));
			assertSinAltas();
		}

		@ParameterizedTest(name = "año = ''{0}''")
		@NullAndEmptySource
		@ValueSource(strings = { "   ", "\t" })
		@DisplayName("TC-02 Falta el año: lanza ValidationException y no inserta")
		public void testCrearCurso_SinAnio(String valor) {
			assertThrows(ValidationException.class, () -> cursoModel.crearCurso("1", "Redes", "6", valor, "1", "Software"));
			assertSinAltas();
		}

		@ParameterizedTest(name = "coordinador = ''{0}''")
		@NullAndEmptySource
		@ValueSource(strings = { "   ", "\t" })
		@DisplayName("TC-02 Falta el coordinador: lanza ValidationException y no inserta")
		public void testCrearCurso_SinCoordinador(String valor) {
			assertThrows(ValidationException.class, () -> cursoModel.crearCurso("1", "Redes", "6", "2", valor, "Software"));
			assertSinAltas();
		}

		@ParameterizedTest(name = "itinerario = ''{0}''")
		@NullAndEmptySource
		@ValueSource(strings = { "   ", "\t" })
		@DisplayName("TC-02 Falta el itinerario: lanza ValidationException y no inserta")
		public void testCrearCurso_SinItinerario(String valor) {
			assertThrows(ValidationException.class, () -> cursoModel.crearCurso("1", "Redes", "6", "2", "1", valor));
			assertSinAltas();
		}

		@ParameterizedTest(name = "créditos = ''{0}''")
		@ValueSource(strings = { "abc", "0", "-3", "Infinity", "NaN", "4.55", "1000" })
		@DisplayName("TC-03 Créditos no válidos: lanza ValidationException y no inserta")
		public void testCrearCurso_CreditosInvalidos(String creditos) {
			assertThrows(ValidationException.class, () -> cursoModel.crearCurso("1", "Redes", creditos, "2", "1", "Software"));
			assertSinAltas();
		}

		@Test
		@DisplayName("TC-03b Créditos con coma decimal: se aceptan como en las propuestas")
		public void testCrearCurso_CreditosConComa() {
			String cursoId = cursoModel.crearCurso("1", "Redes", "6,5", "2", "1", "Software");
			assertEquals(6.5, ((Number) db.executeQueryArray("SELECT creditos FROM Curso WHERE cursoId = ?", cursoId)
					.get(0)[0]).doubleValue());
		}

		@ParameterizedTest(name = "año = ''{0}''")
		@ValueSource(strings = { "abc", "0", "7" })
		@DisplayName("TC-04 Año fuera de 1-6: lanza ValidationException y no inserta")
		public void testCrearCurso_AnioInvalido(String anio) {
			assertThrows(ValidationException.class, () -> cursoModel.crearCurso("1", "Redes", "6", anio, "1", "Software"));
			assertSinAltas();
		}

		@Test
		@DisplayName("TC-05 Duplicado de PK: el ID generado no colisiona con uno existente")
		public void testCrearCurso_IdNoColisiona() {
			db.executeUpdate("INSERT INTO Curso (cursoId, coordinadorId, nombre, creditos) VALUES ('C001', '2', 'Existente', 3)");

			String cursoId = cursoModel.crearCurso("1", "Redes", "6", "2", "1", "Software");

			assertEquals("C002", cursoId);
			assertEquals("Existente", db.executeQueryArray("SELECT nombre FROM Curso WHERE cursoId = 'C001'").get(0)[0]);
		}

		@Test
		@DisplayName("TC-06 El nombre no es UNIQUE: dos altas iguales crean dos cursos distintos")
		public void testCrearCurso_MismoNombreDosVeces() {
			String id1 = cursoModel.crearCurso("1", "Redes", "6", "2", "1", "Software");
			String id2 = cursoModel.crearCurso("1", "Redes", "6", "2", "1", "Software");

			assertNotEquals(id1, id2);
			assertEquals(4, contarFilas("Curso"));
			assertEquals(4, contarFilas("GradoCurso"));
		}

		@ParameterizedTest(name = "estado del grado = {0}")
		@ValueSource(strings = { "PENDIENTE_RENOVACION", "EN_EXTINCION", "EXTINTO" })
		@DisplayName("TC-07 Grado no VIGENTE: lanza ValidationException y no inserta")
		public void testCrearCurso_GradoNoVigente(String estado) {
			db.executeUpdate("UPDATE Grado SET estado = ? WHERE gradoId = '1'", estado);

			assertThrows(ValidationException.class, () -> cursoModel.crearCurso("1", "Redes", "6", "2", "1", "Software"));
			assertSinAltas();
		}

		private void assertSinAltas() {
			assertEquals(2, contarFilas("Curso"), "No debe insertarse ningún Curso");
			assertEquals(2, contarFilas("GradoCurso"), "No debe insertarse ningún GradoCurso");
		}
	}

	// =========================================================================
	// 2. ELIMINAR CURSO
	// =========================================================================
	@Nested
	@DisplayName("2. Eliminar curso")
	public class EliminarCurso {

		@Test
		@DisplayName("TE-C01 Comprobación: el curso no tiene matrícula en el año")
		public void testValidarEliminacion_SinMatriculas() {
			assertDoesNotThrow(() -> cursoModel.validarEliminacion("1", "1", "2"));
		}

		@Test
		@DisplayName("TE-C02 Comprobación: el curso tiene matrícula en el año")
		public void testValidarEliminacion_ConMatriculas() {
			assertThrows(ValidationException.class, () -> cursoModel.validarEliminacion("1", "1", "1"));
		}

		@Test
		@DisplayName("TE-C03 Happy path: el curso pasa a INACTIVO en el grado, sin borrar filas")
		public void testEliminarCurso_HappyPath() {
			cursoModel.eliminarCurso("1", "1", "2");

			assertAll("Estados tras eliminar el curso",
					() -> assertEquals("INACTIVO", estadoGradoCurso("1", "1")),
					() -> assertEquals("ACTIVO", estadoGradoCurso("1", "2")),
					() -> assertEquals(2, contarFilas("Curso")),
					() -> assertEquals(2, contarFilas("GradoCurso")));
		}

		@Test
		@DisplayName("TE-C04 Bloqueo: con matriculaciones en el año no se hace nada")
		public void testEliminarCurso_ConMatriculas_NoHaceNada() {
			assertThrows(ValidationException.class, () -> cursoModel.eliminarCurso("1", "1", "1"));

			assertEquals("ACTIVO", estadoGradoCurso("1", "1"));
		}

		@Test
		@DisplayName("TE-C05 Las matrículas canceladas no cuentan: pasa a INACTIVO")
		public void testEliminarCurso_SoloMatriculasCanceladas() {
			db.executeBatch(new String[] {
					"INSERT INTO Matricula (matriculaId, alumnoId, gradoId, anioAcademicoId, fechaSolicitud, estado, motivoCancelacion, fechaCancelacion) "
							+ "VALUES ('3', '1', '1', '2', '2027-09-01', 'CANCELADA', 'IMPAGO', '2027-09-10')",
					"INSERT INTO MatriculaCurso (matriculaId, gradoId, cursoId, estado, motivoCancelacion, fechaCancelacion, importe, metodoPago, fechaPago, pagoValidado) "
							+ "VALUES ('3', '1', '1', 'CANCELADA', 'IMPAGO', '2027-09-10', 120, NULL, NULL, 0)"
			});

			cursoModel.eliminarCurso("1", "1", "2");

			assertEquals("INACTIVO", estadoGradoCurso("1", "1"));
		}

		@Test
		@DisplayName("TE-C06 Curso ya INACTIVO: lanza ValidationException")
		public void testEliminarCurso_YaInactivo() {
			db.executeUpdate("UPDATE GradoCurso SET estado = 'INACTIVO' WHERE gradoId = '1' AND cursoId = '1'");

			assertThrows(ValidationException.class, () -> cursoModel.validarEliminacion("1", "1", "2"));
			assertThrows(ValidationException.class, () -> cursoModel.eliminarCurso("1", "1", "2"));
		}

		@Test
		@DisplayName("TE-C07 Sin año académico actual: lanza ValidationException y no hace nada")
		public void testEliminarCurso_SinAnoAcademico() {
			assertThrows(ValidationException.class, () -> cursoModel.validarEliminacion("1", "1", null));
			assertThrows(ValidationException.class, () -> cursoModel.eliminarCurso("1", "1", null));

			assertEquals("ACTIVO", estadoGradoCurso("1", "1"));
		}

		@Test
		@DisplayName("TE-C08 Curso compartido: solo cuentan las matrículas de ese grado")
		public void testEliminarCurso_CursoCompartido() {
			db.executeUpdate("INSERT INTO GradoCurso (gradoId, cursoId, anio, itinerario, estado) VALUES ('2', '1', 1, 'General', 'ACTIVO')");

			cursoModel.eliminarCurso("2", "1", "1");

			assertEquals("INACTIVO", estadoGradoCurso("2", "1"));
			assertEquals("ACTIVO", estadoGradoCurso("1", "1"));
		}
	}

	// =========================================================================
	// 3. SIN DATOS MOCK (F-1)
	// =========================================================================
	@Nested
	@DisplayName("3. Sin datos mock")
	public class SinMocks {

		@Test
		@DisplayName("TF-01 Grado sin cursos: getCursosPorGrado devuelve una lista vacía")
		public void testGetCursosPorGrado_SinCursos_ListaVacia() {
			assertTrue(new CursoModel(db).getCursosPorGrado("2").isEmpty());
		}

		@Test
		@DisplayName("TF-02 Curso sin matrículas en el año: getMatriculacionesPorCursoYAnoActual devuelve una lista vacía")
		public void testGetMatriculaciones_SinMatriculas_ListaVacia() {
			assertTrue(new MatriculacionModel(db).getMatriculacionesPorCursoYAnoActual("1", "2").isEmpty());
		}
	}

	private int contarFilas(String tabla) {
		return ((Number) db.executeQueryArray("SELECT COUNT(*) FROM " + tabla).get(0)[0]).intValue();
	}

	private String estadoGradoCurso(String gradoId, String cursoId) {
		return (String) db.executeQueryArray("SELECT estado FROM GradoCurso WHERE gradoId = ? AND cursoId = ?",
				gradoId, cursoId).get(0)[0];
	}
}
