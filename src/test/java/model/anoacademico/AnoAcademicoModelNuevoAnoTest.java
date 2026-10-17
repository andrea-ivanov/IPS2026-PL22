package model.anoacademico;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import model.fixtures.BaseModelTest;
import util.Util;
import util.exception.ValidationException;

/**
 * Pruebas unitarias para la configuración de un nuevo año académico (HU 35223, sección 8).
 * Año anterior del fixture: '2' (2027-09-01 → 2028-06-30).
 * Datos válidos de referencia (V): año 2028-09-01 → 2029-06-30, matriculación 2028-07-01 → 2028-09-30.
 * Salvo en los tests de R8, hoy es 2028-07-01 (el día siguiente al fin del año anterior).
 */
public class AnoAcademicoModelNuevoAnoTest extends BaseModelTest {
	private static final String HOY = "2028-07-01";
	private static final String INICIO_ANIO = "2028-09-01";
	private static final String FIN_ANIO = "2029-06-30";
	private static final String INICIO_MATRICULACION = "2028-07-01";
	private static final String FIN_MATRICULACION = "2028-09-30";

	private AnoAcademicoModel anoAcademicoModel;

	@BeforeEach
	public void initModel() {
		this.anoAcademicoModel = new AnoAcademicoModel(db);
	}

	// =========================================================================
	// 1. ESCENARIOS DE ÉXITO (HAPPY PATH)
	// =========================================================================
	@Nested
	@DisplayName("1. Escenarios de Éxito (Happy Path)")
	public class HappyPath {

		@Test
		@DisplayName("TA-01 Happy path: crea el año 2028-2029 con sus 4 fechas")
		public void testIniciarNuevoAno_HappyPath() {
			String id = iniciar(INICIO_ANIO, FIN_ANIO, INICIO_MATRICULACION, FIN_MATRICULACION);

			assertEquals("2028-2029", id);
			List<Object[]> rows = db.executeQueryArray(
					"SELECT fechaInicio, fechaFin, inicioMatriculacion, finMatriculacion FROM AnioAcademico WHERE anioAcademicoId = ?", id);
			assertEquals(1, rows.size(), "Debe existir el año creado");
			assertAll("Fechas del año creado",
					() -> assertEquals(INICIO_ANIO, String.valueOf(rows.get(0)[0])),
					() -> assertEquals(FIN_ANIO, String.valueOf(rows.get(0)[1])),
					() -> assertEquals(INICIO_MATRICULACION, String.valueOf(rows.get(0)[2])),
					() -> assertEquals(FIN_MATRICULACION, String.valueOf(rows.get(0)[3])),
					() -> assertEquals(3, contarAnios()));
		}

		@Test
		@DisplayName("TA-02 El nuevo año pasa a ser el actual")
		public void testIniciarNuevoAno_PasaASerActual() {
			iniciar(INICIO_ANIO, FIN_ANIO, INICIO_MATRICULACION, FIN_MATRICULACION);

			assertEquals("2028-2029", anoAcademicoModel.buscarActual().getAnioAcademicoId());
		}

		@Test
		@DisplayName("TA-03 La matriculación puede solaparse con el inicio del año")
		public void testIniciarNuevoAno_MatriculacionSolapada() {
			assertEquals("2028-2029", iniciar(INICIO_ANIO, FIN_ANIO, "2028-07-15", "2028-10-15"));
		}

		@Test
		@DisplayName("TA-06 R1/R2 límite válido: año y matriculación empiezan el día siguiente al fin del anterior")
		public void testIniciarNuevoAno_LimiteDiaSiguiente() {
			assertEquals("2028-2029", iniciar("2028-07-01", FIN_ANIO, "2028-07-01", FIN_MATRICULACION));
		}

		@Test
		@DisplayName("TA-11 R5 límite válido: la matriculación termina el día antes que el año")
		public void testIniciarNuevoAno_MatriculacionTerminaDiaAntes() {
			assertEquals("2028-2029", iniciar(INICIO_ANIO, FIN_ANIO, INICIO_MATRICULACION, "2029-06-29"));
		}

		@Test
		@DisplayName("TA-14 Sin años previos: se crea sin aplicar R1 ni R2")
		public void testIniciarNuevoAno_SinAniosPrevios() {
			db.executeBatch(new String[] {
					"DELETE FROM MatriculaCurso",
					"DELETE FROM Matricula",
					"DELETE FROM AnioAcademico"
			});

			assertEquals("2028-2029", iniciar(INICIO_ANIO, FIN_ANIO, INICIO_MATRICULACION, FIN_MATRICULACION));
			assertEquals(1, contarAnios());
		}

		@Test
		@DisplayName("TA-21 validarNuevoAnoAcademico devuelve el ID y no modifica nada")
		public void testValidarNuevoAno_NoModificaNada() {
			String id = anoAcademicoModel.validarNuevoAnoAcademico(HOY, d(INICIO_ANIO), d(FIN_ANIO),
					d(INICIO_MATRICULACION), d(FIN_MATRICULACION));

			assertEquals("2028-2029", id);
			assertSinCambios();
		}
	}

	// =========================================================================
	// R8: NO SE PUEDE CONFIGURAR HASTA QUE TERMINE EL AÑO ANTERIOR
	// =========================================================================
	@Nested
	@DisplayName("R8. El año anterior debe haber terminado")
	public class AnoAnteriorTerminado {

		@ParameterizedTest(name = "hoy = {0}")
		@ValueSource(strings = { "2028-06-30", "2027-10-17" })
		@DisplayName("TA-22/TA-23 R8: hoy es el último día del año anterior o cae en pleno año")
		public void testR8_AnoAnteriorNoTerminado(String hoy) {
			assertThrows(ValidationException.class, () -> anoAcademicoModel.validarAnoAnteriorTerminado(hoy));
			assertThrows(ValidationException.class, () -> anoAcademicoModel.validarNuevoAnoAcademico(hoy,
					d(INICIO_ANIO), d(FIN_ANIO), d(INICIO_MATRICULACION), d(FIN_MATRICULACION)));
			assertThrows(ValidationException.class,
					() -> iniciarEn(hoy, INICIO_ANIO, FIN_ANIO, INICIO_MATRICULACION, FIN_MATRICULACION));
			assertSinCambios();
		}

		@Test
		@DisplayName("TA-24 R8 límite válido: hoy es el día siguiente al fin del año anterior")
		public void testR8_DiaSiguiente() {
			assertDoesNotThrow(() -> anoAcademicoModel.validarAnoAnteriorTerminado("2028-07-01"));
			assertEquals("2028-2029", iniciarEn("2028-07-01", INICIO_ANIO, FIN_ANIO, INICIO_MATRICULACION, FIN_MATRICULACION));
		}

		@Test
		@DisplayName("TA-25 R8 no se aplica si no hay años registrados")
		public void testR8_SinAnios() {
			db.executeBatch(new String[] {
					"DELETE FROM MatriculaCurso",
					"DELETE FROM Matricula",
					"DELETE FROM AnioAcademico"
			});

			assertDoesNotThrow(() -> anoAcademicoModel.validarAnoAnteriorTerminado("2020-01-01"));
			assertEquals("2028-2029", iniciarEn("2020-01-01", INICIO_ANIO, FIN_ANIO, INICIO_MATRICULACION, FIN_MATRICULACION));
		}
	}

	// =========================================================================
	// 2. VALIDACIONES DE FECHAS (R1-R7)
	// =========================================================================
	@Nested
	@DisplayName("2. Validaciones de fechas")
	public class Validaciones {

		@ParameterizedTest(name = "inicio año = {0}")
		@ValueSource(strings = { "2028-06-30", "2028-01-01" })
		@DisplayName("TA-04/TA-05 R1: el año empieza el mismo día o antes del fin del año anterior")
		public void testR1_InicioAnioNoPosteriorAlAnterior(String inicioAnio) {
			assertRechazado(inicioAnio, FIN_ANIO, INICIO_MATRICULACION, FIN_MATRICULACION);
		}

		@ParameterizedTest(name = "inicio matriculación = {0}")
		@ValueSource(strings = { "2028-06-30", "2028-06-01" })
		@DisplayName("TA-07 R2: la matriculación empieza el mismo día o antes del fin del año anterior")
		public void testR2_InicioMatriculacionNoPosteriorAlAnterior(String inicioMatriculacion) {
			assertRechazado(INICIO_ANIO, FIN_ANIO, inicioMatriculacion, FIN_MATRICULACION);
		}

		@ParameterizedTest(name = "fin año = {0}")
		@ValueSource(strings = { "2028-09-01", "2028-08-01" })
		@DisplayName("TA-08 R3: el fin del año es igual o anterior a su inicio")
		public void testR3_FinAnioNoPosteriorAInicio(String finAnio) {
			assertRechazado(INICIO_ANIO, finAnio, INICIO_MATRICULACION, FIN_MATRICULACION);
		}

		@ParameterizedTest(name = "fin matriculación = {0}")
		@ValueSource(strings = { "2028-07-01", "2028-06-15" })
		@DisplayName("TA-09 R4: el fin de la matriculación es igual o anterior a su inicio")
		public void testR4_FinMatriculacionNoPosteriorAInicio(String finMatriculacion) {
			assertRechazado(INICIO_ANIO, FIN_ANIO, INICIO_MATRICULACION, finMatriculacion);
		}

		@ParameterizedTest(name = "fin matriculación = {0}")
		@ValueSource(strings = { "2029-06-30", "2029-07-15" })
		@DisplayName("TA-10 R5: la matriculación termina el mismo día que el año o después")
		public void testR5_FinMatriculacionNoAnteriorAFinAnio(String finMatriculacion) {
			assertRechazado(INICIO_ANIO, FIN_ANIO, INICIO_MATRICULACION, finMatriculacion);
		}

		@ParameterizedTest(name = "fecha nula en la posición {0}")
		@ValueSource(ints = { 0, 1, 2, 3 })
		@DisplayName("TA-12 R6: falta alguna de las 4 fechas")
		public void testR6_FechaNula(int posicion) {
			String[] fechas = { INICIO_ANIO, FIN_ANIO, INICIO_MATRICULACION, FIN_MATRICULACION };
			fechas[posicion] = null;

			assertRechazado(fechas[0], fechas[1], fechas[2], fechas[3]);
		}

		@Test
		@DisplayName("TA-13 R7: ya existe un año con el ID generado")
		public void testR7_IdDuplicado() {
			db.executeUpdate("INSERT INTO AnioAcademico (anioAcademicoId, fechaInicio, fechaFin, inicioMatriculacion, finMatriculacion) "
					+ "VALUES ('2028-2029', '2010-09-01', '2011-06-30', '2010-07-01', '2010-08-31')");

			assertThrows(ValidationException.class, () -> anoAcademicoModel.validarNuevoAnoAcademico(HOY,
					d(INICIO_ANIO), d(FIN_ANIO), d(INICIO_MATRICULACION), d(FIN_MATRICULACION)));
			assertThrows(ValidationException.class,
					() -> iniciar(INICIO_ANIO, FIN_ANIO, INICIO_MATRICULACION, FIN_MATRICULACION));
			assertEquals(3, contarAnios());
			assertEquals("2010-09-01", String.valueOf(db.executeQueryArray(
					"SELECT fechaInicio FROM AnioAcademico WHERE anioAcademicoId = '2028-2029'").get(0)[0]));
		}

		private void assertRechazado(String inicioAnio, String finAnio, String inicioMatriculacion, String finMatriculacion) {
			assertThrows(ValidationException.class, () -> anoAcademicoModel.validarNuevoAnoAcademico(HOY,
					d(inicioAnio), d(finAnio), d(inicioMatriculacion), d(finMatriculacion)));
			assertThrows(ValidationException.class,
					() -> iniciar(inicioAnio, finAnio, inicioMatriculacion, finMatriculacion));
			assertSinCambios();
		}
	}

	// =========================================================================
	// 3. VIGENCIA DE GRADOS Y DESACTIVACIÓN DE CURSOS
	// =========================================================================
	@Nested
	@DisplayName("3. Vigencia de grados y desactivación de cursos")
	public class EfectosSobreGrados {

		@Test
		@DisplayName("TA-16 Vigencia caducada: el grado VIGENTE pasa a PENDIENTE_RENOVACION")
		public void testVigenciaCaducada() {
			iniciar(INICIO_ANIO, FIN_ANIO, INICIO_MATRICULACION, FIN_MATRICULACION);

			assertEquals("PENDIENTE_RENOVACION", estadoGrado("1"));
		}

		@Test
		@DisplayName("TA-17 Vigencia en el límite: termina un día después del inicio del año y sigue VIGENTE")
		public void testVigenciaEnElLimite() {
			db.executeUpdate("UPDATE Grado SET fechaInicioVigencia = '2024-09-02' WHERE gradoId = '2'");

			iniciar(INICIO_ANIO, FIN_ANIO, INICIO_MATRICULACION, FIN_MATRICULACION);

			assertEquals("VIGENTE", estadoGrado("2"));
			assertEquals("PENDIENTE_RENOVACION", estadoGrado("1"));
		}

		@Test
		@DisplayName("TA-18 La vigencia solo afecta a grados VIGENTES")
		public void testVigenciaSoloGradosVigentes() {
			db.executeUpdate("UPDATE Grado SET estado = 'EN_EXTINCION' WHERE gradoId = '1'");

			iniciar(INICIO_ANIO, FIN_ANIO, INICIO_MATRICULACION, FIN_MATRICULACION);

			assertEquals("EN_EXTINCION", estadoGrado("1"));
		}

		@ParameterizedTest(name = "estado del grado = {0}")
		@ValueSource(strings = { "EN_EXTINCION", "EXTINTO" })
		@DisplayName("TA-19 Se desactivan los cursos de grados en extinción")
		public void testDesactivaCursosDeGradosEnExtincion(String estado) {
			db.executeUpdate("UPDATE Grado SET estado = ? WHERE gradoId = '1'", estado);

			iniciar(INICIO_ANIO, FIN_ANIO, INICIO_MATRICULACION, FIN_MATRICULACION);

			assertEquals("INACTIVO", estadoGradoCurso("1", "1"));
			assertEquals("INACTIVO", estadoGradoCurso("1", "2"));
		}

		@Test
		@DisplayName("TA-20 No se desactivan cursos sin matrículas de grados que no están en extinción")
		public void testNoDesactivaCursosSinMatriculas() {
			iniciar(INICIO_ANIO, FIN_ANIO, INICIO_MATRICULACION, FIN_MATRICULACION);

			assertEquals("PENDIENTE_RENOVACION", estadoGrado("1"));
			assertEquals("ACTIVO", estadoGradoCurso("1", "1"));
			assertEquals("ACTIVO", estadoGradoCurso("1", "2"));
		}
	}

	private String iniciar(String inicioAnio, String finAnio, String inicioMatriculacion, String finMatriculacion) {
		return iniciarEn(HOY, inicioAnio, finAnio, inicioMatriculacion, finMatriculacion);
	}

	private String iniciarEn(String hoy, String inicioAnio, String finAnio, String inicioMatriculacion,
			String finMatriculacion) {
		return anoAcademicoModel.iniciarNuevoAnoAcademico(hoy, d(inicioAnio), d(finAnio), d(inicioMatriculacion),
				d(finMatriculacion));
	}

	/** TA-15: ningún año creado y ningún grado o curso modificado. */
	private void assertSinCambios() {
		assertAll("Sin cambios en años, grados ni cursos",
				() -> assertEquals(2, contarAnios()),
				() -> assertEquals("VIGENTE", estadoGrado("1")),
				() -> assertEquals("VIGENTE", estadoGrado("2")),
				() -> assertEquals("ACTIVO", estadoGradoCurso("1", "1")),
				() -> assertEquals("ACTIVO", estadoGradoCurso("1", "2")));
	}

	private static Date d(String iso) {
		return iso != null ? Util.isoStringToDate(iso) : null;
	}

	private int contarAnios() {
		return ((Number) db.executeQueryArray("SELECT COUNT(*) FROM AnioAcademico").get(0)[0]).intValue();
	}

	private String estadoGrado(String gradoId) {
		return (String) db.executeQueryArray("SELECT estado FROM Grado WHERE gradoId = ?", gradoId).get(0)[0];
	}

	private String estadoGradoCurso(String gradoId, String cursoId) {
		return (String) db.executeQueryArray("SELECT estado FROM GradoCurso WHERE gradoId = ? AND cursoId = ?",
				gradoId, cursoId).get(0)[0];
	}
}
