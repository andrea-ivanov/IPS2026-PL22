package model.grado;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import util.Database;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Pruebas de la HU 36455: dictamen del revisor externo y extinción de los grados al iniciar el año (GradoModel).
 * Cada prueba parte de sus propios datos (loadCleanDatabase), independientes de data.sql.
 * Ventana de dictamen: entre el fin de 2025-2026 (2026-07-31) y el inicio de 2026-2027 (2026-09-08),
 * es decir, del 2026-08-01 al 2026-09-07. Los valores esperados están calculados a mano.
 */
public class GradoModelRevisionTest {
	private static final String EN_VENTANA = "2026-08-15";
	private static final String INICIO_ANIO = "2026-09-08";

	private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private GradoModel gradoModel;

	@BeforeEach
	public void setUp() {
		db.createDatabase(false);
		loadCleanDatabase(db);
		gradoModel = new GradoModel(db);
	}

	/**
	 * Datos de prueba (todas las asignaturas de 6 créditos):
	 * - G001 PENDIENTE_RENOVACION con una matrícula pagada en 2026-2027 (M6)
	 * - G002 PENDIENTE_RENOVACION sin matrículas
	 * - G003 VIGENTE, renovado para 2025-2026, con una matrícula pagada en 2026-2027 (M5)
	 * - G004 EN_EXTINCION sin matrículas
	 * - G005 EXTINTO
	 * - G006 PENDIENTE_RENOVACION: M1 (2025-2026, FINALIZADA) y en 2026-2027 M2 (pagada en parte), M3 (con una
	 *   asignatura cancelada por IMPAGO) y M4 (cancelada antes del inicio)
	 * - G007 VIGENTE sin matrículas; G008 VIGENTE nuevo de 2026-2027 sin matrículas
	 * - G009 VIGENTE con una matrícula pagada que se cancela después del inicio (M7)
	 * - G010 VIGENTE con una matrícula sin pagar (M8)
	 * - C001 está en G006 y en G003 (compartida)
	 */
	public static void loadCleanDatabase(Database db) {
		db.executeBatch(new String[] {
				"DELETE FROM RevisionGrado", "DELETE FROM Calificacion", "DELETE FROM MatriculaGrupo",
				"DELETE FROM MatriculaCurso", "DELETE FROM Matricula", "DELETE FROM Acta", "DELETE FROM Convocatoria",
				"DELETE FROM Imparte", "DELETE FROM Grupo", "DELETE FROM PropuestaCurso", "DELETE FROM GradoCurso",
				"DELETE FROM Curso", "DELETE FROM Grado", "DELETE FROM AnioAcademico", "DELETE FROM Profesor",
				"DELETE FROM Alumno",
				"INSERT INTO AnioAcademico VALUES ('2025-2026','2025-09-08','2026-07-31','2025-07-01','2025-09-30')",
				"INSERT INTO AnioAcademico VALUES ('2026-2027','2026-09-08','2027-07-31','2026-08-01','2026-09-30')",
				"INSERT INTO Grado VALUES ('G001','Biologia','Facultad de Ciencias','Ciencias','PENDIENTE_RENOVACION','2022-09-08')",
				"INSERT INTO Grado VALUES ('G002','Derecho','Facultad de Derecho','Ciencias Sociales y Juridicas','PENDIENTE_RENOVACION','2022-09-08')",
				"INSERT INTO Grado VALUES ('G003','Fisica','Facultad de Ciencias','Ciencias','VIGENTE','2025-09-08')",
				"INSERT INTO Grado VALUES ('G004','Historia','Facultad de Filosofia y Letras','Artes y Humanidades','EN_EXTINCION','2022-09-08')",
				"INSERT INTO Grado VALUES ('G005','Quimica','Facultad de Quimica','Ciencias','EXTINTO','2019-09-01')",
				"INSERT INTO Grado VALUES ('G006','Medicina','Facultad de Medicina','Ciencias de la Salud','PENDIENTE_RENOVACION','2022-09-08')",
				"INSERT INTO Grado VALUES ('G007','Arquitectura','Escuela de Arquitectura','Ingenieria y Arquitectura','VIGENTE','2024-09-08')",
				"INSERT INTO Grado VALUES ('G008','Geologia','Facultad de Geologia','Ciencias','VIGENTE','2026-09-08')",
				"INSERT INTO Grado VALUES ('G009','Economia','Facultad de Economia y Empresa','Ciencias Sociales y Juridicas','VIGENTE','2024-09-08')",
				"INSERT INTO Grado VALUES ('G010','Filologia','Facultad de Filosofia y Letras','Artes y Humanidades','VIGENTE','2024-09-08')",
				"INSERT INTO Curso VALUES ('C001',NULL,'Anatomia',6)",
				"INSERT INTO Curso VALUES ('C002',NULL,'Fisiologia',6)",
				"INSERT INTO Curso VALUES ('C003',NULL,'Historia Antigua',6)",
				"INSERT INTO Curso VALUES ('C004',NULL,'Dibujo Tecnico',6)",
				"INSERT INTO Curso VALUES ('C005',NULL,'Genetica',6)",
				"INSERT INTO Curso VALUES ('C006',NULL,'Microeconomia',6)",
				"INSERT INTO Curso VALUES ('C007',NULL,'Latin',6)",
				"INSERT INTO GradoCurso VALUES ('G006','C001',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G003','C001',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G006','C002',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G004','C003',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G007','C004',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G001','C005',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G009','C006',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G010','C007',1,NULL,'ACTIVO')",
				"INSERT INTO RevisionGrado VALUES ('G003','2025-2026','FAVORABLE','2025-08-04')",
				"INSERT INTO Alumno VALUES ('AL01','40000001A','Ana','Alonso','al01@uni.es')",
				"INSERT INTO Alumno VALUES ('AL02','40000002B','Blas','Barrio','al02@uni.es')",
				"INSERT INTO Alumno VALUES ('AL03','40000003C','Celia','Cano','al03@uni.es')",
				"INSERT INTO Alumno VALUES ('AL04','40000004D','Dario','Diez','al04@uni.es')",
				"INSERT INTO Alumno VALUES ('AL05','40000005E','Eva','Escobar','al05@uni.es')",
				"INSERT INTO Alumno VALUES ('AL06','40000006F','Fede','Fuentes','al06@uni.es')",
				"INSERT INTO Alumno VALUES ('AL07','40000007G','Gloria','Gil','al07@uni.es')",
				// G006: una matrícula del año anterior y tres del año que empieza
				"INSERT INTO Matricula VALUES ('M1','AL01','G006','2025-2026','2025-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO MatriculaCurso VALUES ('M1','G006','C001','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-10',1)",
				"INSERT INTO Matricula VALUES ('M2','AL01','G006','2026-2027','2026-08-10','SOLICITADA',NULL,NULL)",
				"INSERT INTO MatriculaCurso VALUES ('M2','G006','C001','PAGADA',NULL,NULL,120,'TARJETA','2026-08-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M2','G006','C002','SOLICITADA',NULL,NULL,120,NULL,NULL,0)",
				"INSERT INTO Matricula VALUES ('M3','AL02','G006','2026-2027','2026-08-10','SOLICITADA',NULL,NULL)",
				"INSERT INTO MatriculaCurso VALUES ('M3','G006','C001','CANCELADA','IMPAGO','2026-08-20',120,NULL,NULL,0)",
				"INSERT INTO MatriculaCurso VALUES ('M3','G006','C002','PAGADA',NULL,NULL,120,'TARJETA','2026-08-10',1)",
				"INSERT INTO Matricula VALUES ('M4','AL03','G006','2026-2027','2026-08-10','CANCELADA','FUERZA_MAYOR','2026-08-25')",
				"INSERT INTO MatriculaCurso VALUES ('M4','G006','C001','CANCELADA','FUERZA_MAYOR','2026-08-25',120,'TARJETA','2026-08-10',1)",
				// G003, G001, G009 y G010
				"INSERT INTO Matricula VALUES ('M5','AL04','G003','2026-2027','2026-08-10','SOLICITADA',NULL,NULL)",
				"INSERT INTO MatriculaCurso VALUES ('M5','G003','C001','PAGADA',NULL,NULL,120,'TARJETA','2026-08-10',1)",
				"INSERT INTO Matricula VALUES ('M6','AL05','G001','2026-2027','2026-08-10','SOLICITADA',NULL,NULL)",
				"INSERT INTO MatriculaCurso VALUES ('M6','G001','C005','PAGADA',NULL,NULL,120,'TARJETA','2026-08-10',1)",
				"INSERT INTO Matricula VALUES ('M7','AL06','G009','2026-2027','2026-08-10','CANCELADA','FUERZA_MAYOR','2026-10-01')",
				"INSERT INTO MatriculaCurso VALUES ('M7','G009','C006','CANCELADA','FUERZA_MAYOR','2026-10-01',120,'TARJETA','2026-08-10',1)",
				"INSERT INTO Matricula VALUES ('M8','AL07','G010','2026-2027','2026-08-10','SOLICITADA',NULL,NULL)",
				"INSERT INTO MatriculaCurso VALUES ('M8','G010','C007','SOLICITADA',NULL,NULL,120,NULL,NULL,0)"
		});
	}

	// =========================================================================
	// 1. EMITIR EL DICTAMEN
	// =========================================================================
	@Nested
	@DisplayName("1. Emitir el dictamen")
	public class EmitirDictamen {

		@Test
		@DisplayName("TR-01 Favorable: se guarda el dictamen y el grado pasa a VIGENTE desde el inicio del año nuevo")
		public void testFavorable() {
			gradoModel.emitirDictamen("G001", "FAVORABLE", EN_VENTANA);

			assertAll(
					() -> assertEquals(List.of("2026-2027 FAVORABLE 2026-08-15"), dictamenes("G001")),
					() -> assertEquals("VIGENTE", estadoGrado("G001")),
					() -> assertEquals("2026-09-08", vigenciaGrado("G001")));
		}

		@Test
		@DisplayName("TR-02 Desfavorable: se guarda el dictamen y el grado pasa a EN_EXTINCION sin cambiar su vigencia")
		public void testDesfavorable() {
			gradoModel.emitirDictamen("G002", "DESFAVORABLE", EN_VENTANA);

			assertAll(
					() -> assertEquals(List.of("2026-2027 DESFAVORABLE 2026-08-15"), dictamenes("G002")),
					() -> assertEquals("EN_EXTINCION", estadoGrado("G002")),
					() -> assertEquals("2022-09-08", vigenciaGrado("G002")));
		}

		@Test
		@DisplayName("TR-03 Cambiar un desfavorable a favorable sobrescribe el dictamen con la fecha del cambio")
		public void testCambioDesfavorableAFavorable() {
			gradoModel.emitirDictamen("G002", "DESFAVORABLE", EN_VENTANA);
			gradoModel.emitirDictamen("G002", "FAVORABLE", "2026-08-20");

			assertAll(
					() -> assertEquals(List.of("2026-2027 FAVORABLE 2026-08-20"), dictamenes("G002")),
					() -> assertEquals("VIGENTE", estadoGrado("G002")),
					() -> assertEquals("2026-09-08", vigenciaGrado("G002")));
		}

		@Test
		@DisplayName("TR-04 Un grado EN_EXTINCION sin dictamen de este año admite dictamen")
		public void testEnExtincionSinDictamen() {
			gradoModel.emitirDictamen("G004", "FAVORABLE", EN_VENTANA);

			assertAll(
					() -> assertEquals(List.of("2026-2027 FAVORABLE 2026-08-15"), dictamenes("G004")),
					() -> assertEquals("VIGENTE", estadoGrado("G004")));
		}

		@Test
		@DisplayName("TR-10 Cambiar un favorable a desfavorable sobrescribe el dictamen")
		public void testCambioFavorableADesfavorable() {
			gradoModel.emitirDictamen("G001", "FAVORABLE", EN_VENTANA);
			gradoModel.emitirDictamen("G001", "DESFAVORABLE", "2026-08-20");

			assertAll(
					() -> assertEquals(List.of("2026-2027 DESFAVORABLE 2026-08-20"), dictamenes("G001")),
					() -> assertEquals("EN_EXTINCION", estadoGrado("G001")));
		}

		@Test
		@DisplayName("TR-10b Se puede cambiar de opinión varias veces: queda el último dictamen")
		public void testCambiosEncadenados() {
			gradoModel.emitirDictamen("G001", "FAVORABLE", EN_VENTANA);
			gradoModel.emitirDictamen("G001", "DESFAVORABLE", "2026-08-20");
			gradoModel.emitirDictamen("G001", "FAVORABLE", "2026-08-25");

			assertAll(
					() -> assertEquals(List.of("2026-2027 FAVORABLE 2026-08-25"), dictamenes("G001")),
					() -> assertEquals("VIGENTE", estadoGrado("G001")),
					() -> assertEquals("2026-09-08", vigenciaGrado("G001")));
		}
	}

	// =========================================================================
	// 2. CUÁNDO SE PUEDE EMITIR
	// =========================================================================
	@Nested
	@DisplayName("2. Ventana y estados que admiten dictamen")
	public class VentanaYEstados {

		@ParameterizedTest(name = "hoy = {0}")
		@ValueSource(strings = { "2026-08-01", "2026-09-07" })
		@DisplayName("TR-05 Límites válidos de la ventana: el día siguiente al fin del año anterior y el día antes del inicio")
		public void testLimitesValidos(String hoy) {
			gradoModel.emitirDictamen("G001", "FAVORABLE", hoy);

			assertEquals(List.of("2026-2027 FAVORABLE " + hoy), dictamenes("G001"));
		}

		@ParameterizedTest(name = "hoy = {0}")
		@ValueSource(strings = { "2026-07-31", "2026-09-08" })
		@DisplayName("TR-06 Límites no válidos: el último día del año anterior y el primero del año nuevo")
		public void testLimitesNoValidos(String hoy) {
			assertThrows(ValidationException.class, () -> gradoModel.emitirDictamen("G001", "FAVORABLE", hoy));

			assertSinDictamenNuevo("G001", "PENDIENTE_RENOVACION");
		}

		@Test
		@DisplayName("TR-07 En pleno año académico no se puede emitir")
		public void testEnPlenoAnio() {
			assertThrows(ValidationException.class,
					() -> gradoModel.emitirDictamen("G001", "FAVORABLE", "2026-03-01"));

			assertSinDictamenNuevo("G001", "PENDIENTE_RENOVACION");
		}

		@Test
		@DisplayName("TR-08 Sin año anterior basta con que no haya empezado el año nuevo")
		public void testSinAnioAnterior() {
			db.executeBatch(new String[] {
					"DELETE FROM MatriculaCurso WHERE matriculaId = 'M1'", "DELETE FROM Matricula WHERE matriculaId = 'M1'",
					"DELETE FROM RevisionGrado", "DELETE FROM AnioAcademico WHERE anioAcademicoId = '2025-2026'"
			});

			gradoModel.emitirDictamen("G001", "FAVORABLE", "2026-01-10");

			assertEquals(List.of("2026-2027 FAVORABLE 2026-01-10"), dictamenes("G001"));
		}

		@ParameterizedTest(name = "grado = {0}")
		@ValueSource(strings = { "G003", "G007", "G005" })
		@DisplayName("TR-09 VIGENTE renovado en otro año, VIGENTE sin revisar o EXTINTO: no admiten dictamen")
		public void testEstadoNoAdmiteDictamen(String gradoId) {
			String estado = estadoGrado(gradoId);

			assertThrows(ValidationException.class, () -> gradoModel.emitirDictamen(gradoId, "FAVORABLE", EN_VENTANA));

			assertAll(
					() -> assertEquals(1, contarFilas("RevisionGrado")),
					() -> assertEquals(estado, estadoGrado(gradoId)));
		}

		@Test
		@DisplayName("TR-11 Grado inexistente: ResourceNotFoundException")
		public void testGradoInexistente() {
			assertAll(
					() -> assertThrows(ResourceNotFoundException.class,
							() -> gradoModel.emitirDictamen("G999", "FAVORABLE", EN_VENTANA)),
					() -> assertThrows(ResourceNotFoundException.class, () -> gradoModel.getGradoRevision("G999")));
		}

		@ParameterizedTest(name = "resultado = ''{0}''")
		@NullAndEmptySource
		@ValueSource(strings = { "   ", "OTRO" })
		@DisplayName("TR-12 Resultado no válido: ValidationException y nada cambia")
		public void testResultadoNoValido(String resultado) {
			assertThrows(ValidationException.class, () -> gradoModel.emitirDictamen("G001", resultado, EN_VENTANA));

			assertSinDictamenNuevo("G001", "PENDIENTE_RENOVACION");
		}

		@Test
		@DisplayName("TR-13 admiteDictamen: pendientes, en extinción y renovados en esta ventana, solo dentro de ella")
		public void testAdmiteDictamen() {
			assertAll("Antes de decidir nada",
					() -> assertTrue(gradoModel.admiteDictamen("G001", EN_VENTANA)),
					() -> assertTrue(gradoModel.admiteDictamen("G004", EN_VENTANA)),
					() -> assertFalse(gradoModel.admiteDictamen("G003", EN_VENTANA)),
					() -> assertFalse(gradoModel.admiteDictamen("G007", EN_VENTANA)),
					() -> assertFalse(gradoModel.admiteDictamen("G005", EN_VENTANA)),
					() -> assertFalse(gradoModel.admiteDictamen("G001", "2026-07-31")),
					() -> assertFalse(gradoModel.admiteDictamen("G001", INICIO_ANIO)),
					() -> assertEquals(1, contarFilas("RevisionGrado"), "admiteDictamen no modifica nada"),
					() -> assertEquals("PENDIENTE_RENOVACION", estadoGrado("G001")));

			gradoModel.emitirDictamen("G001", "FAVORABLE", EN_VENTANA);

			assertTrue(gradoModel.admiteDictamen("G001", "2026-08-20"), "Un renovado en esta ventana puede cambiar");
		}
	}

	// =========================================================================
	// 3. CABECERA DE LA PANTALLA DEL GRADO
	// =========================================================================
	@Nested
	@DisplayName("3. Datos del grado para la pantalla de revisión")
	public class CabeceraGrado {

		@Test
		@DisplayName("TR-14 VIGENTE sin dictamen de este año: decisión nula y fecha del último dictamen")
		public void testVigente() {
			assertCabecera(gradoModel.getGradoRevision("G003"),
					"G003", "Fisica", "Facultad de Ciencias", "Ciencias", "VIGENTE", null, "2025-08-04");
		}

		@Test
		@DisplayName("TR-15 Pendiente sin dictamen: Desfavorable por defecto y sin fecha")
		public void testPendienteSinDictamen() {
			assertCabecera(gradoModel.getGradoRevision("G001"),
					"G001", "Biologia", "Facultad de Ciencias", "Ciencias", "PENDIENTE_RENOVACION", "DESFAVORABLE", null);
		}

		@Test
		@DisplayName("TR-16 Renovado en esta ventana: su dictamen y su fecha")
		public void testRenovado() {
			gradoModel.emitirDictamen("G001", "FAVORABLE", EN_VENTANA);

			assertCabecera(gradoModel.getGradoRevision("G001"),
					"G001", "Biologia", "Facultad de Ciencias", "Ciencias", "VIGENTE", "FAVORABLE", "2026-08-15");
		}
	}

	// =========================================================================
	// 4. EXTINCIÓN AL INICIAR EL AÑO
	// =========================================================================
	@Nested
	@DisplayName("4. Extinción al iniciar el año")
	public class ExtincionInicioAnio {

		@Test
		@DisplayName("TX-01 Primer día del año: se extinguen los no renovados y los que no tienen matrículas pagadas")
		public void testPrimerDiaDelAnio() {
			assertEquals(7, gradoModel.extinguirGradosAlIniciarAnio(INICIO_ANIO));

			assertAll(
					() -> assertEquals("EXTINTO", estadoGrado("G001")),
					() -> assertEquals("EXTINTO", estadoGrado("G002")),
					() -> assertEquals("EXTINTO", estadoGrado("G004")),
					() -> assertEquals("EXTINTO", estadoGrado("G006")),
					() -> assertEquals("EXTINTO", estadoGrado("G007")),
					() -> assertEquals("EXTINTO", estadoGrado("G008")),
					() -> assertEquals("EXTINTO", estadoGrado("G010")),
					() -> assertEquals("VIGENTE", estadoGrado("G003")),
					() -> assertEquals("VIGENTE", estadoGrado("G009")),
					() -> assertEquals("EXTINTO", estadoGrado("G005")));
		}

		@Test
		@DisplayName("TX-02 Antes de empezar el año no cambia nada")
		public void testAntesDelInicio() {
			assertEquals(0, gradoModel.extinguirGradosAlIniciarAnio("2026-09-07"));

			assertAll(
					() -> assertEquals("PENDIENTE_RENOVACION", estadoGrado("G001")),
					() -> assertEquals("EN_EXTINCION", estadoGrado("G004")),
					() -> assertEquals("VIGENTE", estadoGrado("G007")),
					() -> assertEquals(List.of("SOLICITADA null null"), matricula("M2")));
		}

		@Test
		@DisplayName("TX-03 Flujo completo: el favorable con matrícula pagada sigue VIGENTE; el desfavorable se extingue")
		public void testFlujoCompleto() {
			gradoModel.emitirDictamen("G001", "FAVORABLE", EN_VENTANA);
			gradoModel.emitirDictamen("G002", "DESFAVORABLE", EN_VENTANA);

			assertEquals(6, gradoModel.extinguirGradosAlIniciarAnio(INICIO_ANIO));

			assertAll(
					() -> assertEquals("VIGENTE", estadoGrado("G001")),
					() -> assertEquals("EXTINTO", estadoGrado("G002")),
					() -> assertEquals(List.of("SOLICITADA null null"), matricula("M6")));
		}

		@Test
		@DisplayName("TX-03b Manda el último dictamen: FAVORABLE, DESFAVORABLE y FAVORABLE deja el grado VIGENTE")
		public void testUltimoDictamenManda() {
			gradoModel.emitirDictamen("G001", "FAVORABLE", EN_VENTANA);
			gradoModel.emitirDictamen("G001", "DESFAVORABLE", "2026-08-20");
			gradoModel.emitirDictamen("G001", "FAVORABLE", "2026-08-25");

			gradoModel.extinguirGradosAlIniciarAnio(INICIO_ANIO);

			assertEquals("VIGENTE", estadoGrado("G001"));
		}

		@Test
		@DisplayName("TX-03c Una matrícula sin pagar no evita la extinción y se cancela por EXTINCION_GRADO")
		public void testMatriculaSinPagar() {
			gradoModel.extinguirGradosAlIniciarAnio(INICIO_ANIO);

			assertAll(
					() -> assertEquals("EXTINTO", estadoGrado("G010")),
					() -> assertEquals(List.of("CANCELADA EXTINCION_GRADO 2026-09-08"), matricula("M8")),
					() -> assertEquals("CANCELADA EXTINCION_GRADO 2026-09-08", matriculaCurso("M8", "C007")));
		}

		@Test
		@DisplayName("TX-04 Un favorable sin matrículas se extingue igualmente")
		public void testFavorableSinMatriculas() {
			gradoModel.emitirDictamen("G004", "FAVORABLE", EN_VENTANA);

			gradoModel.extinguirGradosAlIniciarAnio(INICIO_ANIO);

			assertEquals("EXTINTO", estadoGrado("G004"));
		}

		@Test
		@DisplayName("TX-05 Se cancelan todas las matrículas del año de los grados extinguidos; las asignaturas ya canceladas conservan su motivo")
		public void testCancelacionMatriculas() {
			gradoModel.extinguirGradosAlIniciarAnio(INICIO_ANIO);

			assertAll(
					() -> assertEquals(List.of("CANCELADA EXTINCION_GRADO 2026-09-08"), matricula("M2")),
					() -> assertEquals("CANCELADA EXTINCION_GRADO 2026-09-08", matriculaCurso("M2", "C001")),
					() -> assertEquals("CANCELADA EXTINCION_GRADO 2026-09-08", matriculaCurso("M2", "C002")),
					() -> assertEquals(List.of("CANCELADA EXTINCION_GRADO 2026-09-08"), matricula("M3")),
					() -> assertEquals("CANCELADA IMPAGO 2026-08-20", matriculaCurso("M3", "C001")),
					() -> assertEquals("CANCELADA EXTINCION_GRADO 2026-09-08", matriculaCurso("M3", "C002")),
					() -> assertEquals(List.of("CANCELADA EXTINCION_GRADO 2026-09-08"), matricula("M6")),
					() -> assertEquals("CANCELADA EXTINCION_GRADO 2026-09-08", matriculaCurso("M6", "C005")));
		}

		@Test
		@DisplayName("TX-06 No se tocan las matrículas de otros años, las ya canceladas ni las de grados que siguen")
		public void testMatriculasQueNoCambian() {
			gradoModel.extinguirGradosAlIniciarAnio(INICIO_ANIO);

			assertAll(
					() -> assertEquals(List.of("FINALIZADA null null"), matricula("M1")),
					() -> assertEquals("FINALIZADA null null", matriculaCurso("M1", "C001")),
					() -> assertEquals(List.of("CANCELADA FUERZA_MAYOR 2026-08-25"), matricula("M4")),
					() -> assertEquals("CANCELADA FUERZA_MAYOR 2026-08-25", matriculaCurso("M4", "C001")),
					() -> assertEquals(List.of("SOLICITADA null null"), matricula("M5")),
					() -> assertEquals("PAGADA null null", matriculaCurso("M5", "C001")),
					() -> assertEquals(List.of("CANCELADA FUERZA_MAYOR 2026-10-01"), matricula("M7")));
		}

		@Test
		@DisplayName("TX-07 Los cursos de los grados extinguidos pasan a INACTIVO solo en esos grados")
		public void testCursos() {
			gradoModel.extinguirGradosAlIniciarAnio(INICIO_ANIO);

			assertAll(
					() -> assertEquals("INACTIVO", estadoGradoCurso("G001", "C005")),
					() -> assertEquals("INACTIVO", estadoGradoCurso("G004", "C003")),
					() -> assertEquals("INACTIVO", estadoGradoCurso("G006", "C001")),
					() -> assertEquals("INACTIVO", estadoGradoCurso("G006", "C002")),
					() -> assertEquals("INACTIVO", estadoGradoCurso("G007", "C004")),
					() -> assertEquals("INACTIVO", estadoGradoCurso("G010", "C007")),
					() -> assertEquals("ACTIVO", estadoGradoCurso("G003", "C001")),
					() -> assertEquals("ACTIVO", estadoGradoCurso("G009", "C006")));
		}

		@Test
		@DisplayName("TX-08 Es idempotente: la segunda ejecución no cambia nada")
		public void testIdempotente() {
			gradoModel.extinguirGradosAlIniciarAnio(INICIO_ANIO);

			assertEquals(0, gradoModel.extinguirGradosAlIniciarAnio(INICIO_ANIO));
			assertAll(
					() -> assertEquals("VIGENTE", estadoGrado("G003")),
					() -> assertEquals("VIGENTE", estadoGrado("G009")),
					() -> assertEquals("CANCELADA IMPAGO 2026-08-20", matriculaCurso("M3", "C001")));
		}

		@Test
		@DisplayName("TX-09 Una matrícula pagada cancelada después del inicio cuenta: el grado no se extingue")
		public void testCanceladaDespuesDelInicio() {
			gradoModel.extinguirGradosAlIniciarAnio("2026-10-15");

			assertAll(
					() -> assertEquals("VIGENTE", estadoGrado("G009")),
					() -> assertEquals("VIGENTE", estadoGrado("G003")));
		}

		@Test
		@DisplayName("TX-10 Sin años académicos no hace nada")
		public void testSinAnios() {
			db.executeBatch(new String[] {
					"DELETE FROM RevisionGrado", "DELETE FROM MatriculaCurso", "DELETE FROM Matricula",
					"DELETE FROM AnioAcademico"
			});

			assertEquals(0, gradoModel.extinguirGradosAlIniciarAnio(INICIO_ANIO));
			assertEquals("PENDIENTE_RENOVACION", estadoGrado("G001"));
		}
	}

	private void assertSinDictamenNuevo(String gradoId, String estado) {
		assertAll(
				() -> assertTrue(dictamenes(gradoId).isEmpty(), "No debe guardarse ningún dictamen"),
				() -> assertEquals(estado, estadoGrado(gradoId)));
	}

	private static void assertCabecera(GradoRevisionDisplayDTO cabecera, String idGrado, String nombre, String facultad,
			String rama, String estado, String decision, String fechaUltimaRevision) {
		assertAll(idGrado,
				() -> assertEquals(idGrado, cabecera.getIdGrado(), "idGrado"),
				() -> assertEquals(nombre, cabecera.getNombre(), "nombre"),
				() -> assertEquals(facultad, cabecera.getFacultad(), "facultad"),
				() -> assertEquals(rama, cabecera.getRama(), "rama"),
				() -> assertEquals(estado, cabecera.getEstado(), "estado"),
				() -> {
					if (decision == null) {
						assertNull(cabecera.getDecision(), "decision");
					} else {
						assertEquals(decision, cabecera.getDecision(), "decision");
					}
				},
				() -> assertEquals(fechaUltimaRevision, cabecera.getFechaUltimaRevision(), "fechaUltimaRevision"));
	}

	private static int contarFilas(String tabla) {
		return ((Number) db.executeQueryArray("SELECT COUNT(*) FROM " + tabla).get(0)[0]).intValue();
	}

	private static String estadoGrado(String gradoId) {
		return (String) db.executeQueryArray("SELECT estado FROM Grado WHERE gradoId = ?", gradoId).get(0)[0];
	}

	private static String vigenciaGrado(String gradoId) {
		return String.valueOf(
				db.executeQueryArray("SELECT fechaInicioVigencia FROM Grado WHERE gradoId = ?", gradoId).get(0)[0]);
	}

	private static String estadoGradoCurso(String gradoId, String cursoId) {
		return (String) db.executeQueryArray("SELECT estado FROM GradoCurso WHERE gradoId = ? AND cursoId = ?",
				gradoId, cursoId).get(0)[0];
	}

	/** "anio resultado fecha" de cada dictamen del grado */
	private static List<String> dictamenes(String gradoId) {
		return db.executeQueryArray("SELECT anioAcademicoId, resultado, fechaRevision FROM RevisionGrado "
				+ "WHERE gradoId = ? ORDER BY anioAcademicoId", gradoId).stream()
				.map(f -> f[0] + " " + f[1] + " " + f[2]).collect(Collectors.toList());
	}

	/** "estado motivo fecha" de la matrícula (lista de un elemento) */
	private static List<String> matricula(String matriculaId) {
		return db.executeQueryArray("SELECT estado, motivoCancelacion, fechaCancelacion FROM Matricula "
				+ "WHERE matriculaId = ?", matriculaId).stream()
				.map(f -> f[0] + " " + f[1] + " " + f[2]).collect(Collectors.toList());
	}

	/** "estado motivo fecha" de una asignatura de la matrícula */
	private static String matriculaCurso(String matriculaId, String cursoId) {
		Object[] f = db.executeQueryArray("SELECT estado, motivoCancelacion, fechaCancelacion FROM MatriculaCurso "
				+ "WHERE matriculaId = ? AND cursoId = ?", matriculaId, cursoId).get(0);
		return f[0] + " " + f[1] + " " + f[2];
	}
}
