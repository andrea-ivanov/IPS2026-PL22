package model.microcredencial;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import util.Database;
import util.exception.ValidationException;

/**
 * Pruebas unitarias de la HU 36410: cancelación automática por baja demanda de microcredenciales
 * y consulta en las vistas del profesor y del estudiante.
 */
public class MicrocredencialBajaDemandaTest {
	private static final String ANIO = "2026-2027";
	private static final String PROFESOR = "P001";
	private static final String RAMA = "Ingenieria y Arquitectura";

	private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private MicrocredencialModel microModel;
	private InscripcionMicrocredencialModel inscripcionModel;

	@BeforeEach
	public void setUp() {
		db.createDatabase(false);
		loadCleanDatabase(db);
		microModel = new MicrocredencialModel(db);
		inscripcionModel = new InscripcionMicrocredencialModel(db);
	}

	/**
	 * Escenario de datos:
	 * - Año 2026-2027, Grado G1 (Ingenieria y Arquitectura), Profesor P001
	 * - 3 alumnos con matrícula ACTIVA en G1 (M1 -> AL1, M2 -> AL2, M3 -> AL3)
	 * - MC1: 20 plazas, finMatriculacion = '2026-10-15' (evaluación el '2026-10-14').
	 *        Tiene 1 inscripción INSCRITA (pagada, 5% < 10%) y 1 SOLICITADA (sin pagar).
	 * - MC2: 20 plazas, finMatriculacion = '2026-10-15' (evaluación el '2026-10-14').
	 *        Tiene 2 inscripciones INSCRITAS (pagadas, 10% >= 10%) y 1 SOLICITADA (sin pagar).
	 * - MC3: 20 plazas, finMatriculacion = '2026-10-25' (evaluación el '2026-10-24', aún abierta el '2026-10-14').
	 *        Tiene 0 inscripciones.
	 */
	public static void loadCleanDatabase(Database db) {
		db.executeBatch(new String[] {
				"DELETE FROM MatriculaMicrocredencial",
				"DELETE FROM Microcredencial",
				"DELETE FROM Matricula",
				"DELETE FROM Grado",
				"DELETE FROM Alumno",
				"DELETE FROM Profesor",
				"DELETE FROM AnioAcademico",
				"INSERT INTO AnioAcademico VALUES ('2026-2027','2026-09-01','2027-08-31','2026-07-01','2026-09-30')",
				"INSERT INTO Grado VALUES ('G1','Ingenieria Informatica','Escuela','Ingenieria y Arquitectura','VIGENTE','2024-09-01')",
				"INSERT INTO Profesor VALUES ('P001','10000001A','Ana','Garcia','p001@uni.es')",
				"INSERT INTO Alumno VALUES ('AL1','20000001A','Juan','Perez','al1@uni.es')",
				"INSERT INTO Alumno VALUES ('AL2','20000002B','Maria','Gomez','al2@uni.es')",
				"INSERT INTO Alumno VALUES ('AL3','20000003C','Pedro','Lopez','al3@uni.es')",
				"INSERT INTO Matricula VALUES ('M1','AL1','G1','2026-2027','2026-07-10','ACTIVA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M2','AL2','G1','2026-2027','2026-07-11','ACTIVA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M3','AL3','G1','2026-2027','2026-07-12','ACTIVA',NULL,NULL)",
				// MC1: baja demanda (1 pagada de 20 plazas = 5% < 10%)
				"INSERT INTO Microcredencial VALUES ('MC1','2026-2027','P001','Kubernetes','Ingenieria y Arquitectura',3,20,"
						+ "'2026-10-01','2026-10-15','2026-10-20','2026-11-20','ACEPTADA','2026-09-10','2026-09-15',NULL,NULL,NULL,0)",
				// MC2: alcanza exactamente el 10% (2 pagadas de 20 plazas = 10%)
				"INSERT INTO Microcredencial VALUES ('MC2','2026-2027','P001','Cloud Native','Ingenieria y Arquitectura',3,20,"
						+ "'2026-10-01','2026-10-15','2026-10-20','2026-11-20','ACEPTADA','2026-09-10','2026-09-15',NULL,NULL,NULL,0)",
				// MC3: aún en periodo de matriculación el 2026-10-14
				"INSERT INTO Microcredencial VALUES ('MC3','2026-2027','P001','Seguridad Web','Ingenieria y Arquitectura',3,20,"
						+ "'2026-10-11','2026-10-25','2026-11-01','2026-12-01','ACEPTADA','2026-09-10','2026-09-15',NULL,NULL,NULL,0)",
				// Inscripciones en MC1: AL1 pagada (INSCRITA), AL2 sin pagar (SOLICITADA)
				"INSERT INTO MatriculaMicrocredencial VALUES ('M1','2026-2027','MC1','2026-10-05','INSCRITA',NULL,NULL,60,'TARJETA','2026-10-05',1,NULL)",
				"INSERT INTO MatriculaMicrocredencial VALUES ('M2','2026-2027','MC1','2026-10-06','SOLICITADA',NULL,NULL,60,'TRANSFERENCIA','2026-10-06',0,NULL)",
				// Inscripciones en MC2: AL1 y AL2 pagadas (INSCRITA), AL3 sin pagar (SOLICITADA)
				"INSERT INTO MatriculaMicrocredencial VALUES ('M1','2026-2027','MC2','2026-10-05','INSCRITA',NULL,NULL,60,'TARJETA','2026-10-05',1,NULL)",
				"INSERT INTO MatriculaMicrocredencial VALUES ('M2','2026-2027','MC2','2026-10-06','INSCRITA',NULL,NULL,60,'TARJETA','2026-10-06',1,NULL)",
				"INSERT INTO MatriculaMicrocredencial VALUES ('M3','2026-2027','MC2','2026-10-07','SOLICITADA',NULL,NULL,60,NULL,NULL,0,NULL)"
		});
	}

	@Test
	@DisplayName("HU 36410: El día antes del cierre de matriculación se cancela la que tiene < 10% y se mantiene la que tiene >= 10%")
	public void testCancelacionPorBajaDemandaEnFechaEvaluacion() {
		// Evaluación el día antes de que cierre el periodo de matriculación ('2026-10-14' para fin '2026-10-15')
		microModel.cancelarPorBajaDemanda("2026-10-14");

		List<MicrocredencialDisplayDTO> delProfesor = microModel.getMicrocredenciales(PROFESOR);
		MicrocredencialDisplayDTO mc1 = delProfesor.stream().filter(m -> "MC1".equals(m.getMicrocredencialId())).findFirst().orElseThrow();
		MicrocredencialDisplayDTO mc2 = delProfesor.stream().filter(m -> "MC2".equals(m.getMicrocredencialId())).findFirst().orElseThrow();
		MicrocredencialDisplayDTO mc3 = delProfesor.stream().filter(m -> "MC3".equals(m.getMicrocredencialId())).findFirst().orElseThrow();

		// 1. MC1 (< 10%) pasa a CANCELADA_BAJA_DEMANDA y el profesor la ve como "Cancelada por baja demanda"
		assertAll(
				() -> assertEquals("CANCELADA_BAJA_DEMANDA", mc1.getEstado()),
				() -> assertEquals("Cancelada por baja demanda", mc1.getEstadoLegible()),
				() -> assertEquals("2026-10-14", mc1.getFechaCancelacion()));

		// 2. MC2 (10% exacto) se mantiene ACEPTADA
		assertAll(
				() -> assertEquals("ACEPTADA", mc2.getEstado()),
				() -> assertNull(mc2.getFechaCancelacion()));

		// 3. MC3 (periodo aún abierto) no se toca
		assertAll(
				() -> assertEquals("ACEPTADA", mc3.getEstado()),
				() -> assertNull(mc3.getFechaCancelacion()));
	}

	@Test
	@DisplayName("HU 36410: Las inscripciones de una microcredencial cancelada pasan a CANCELADA por BAJA_DEMANDA y las impagadas por IMPAGO")
	public void testInscripcionesNotificadasAlEstudiante() {
		microModel.cancelarPorBajaDemanda("2026-10-14");

		// AL1 tenía MC1 pagada (pasa a CANCELADA por BAJA_DEMANDA) y MC2 pagada (sigue INSCRITA)
		List<MatriculaMicrocredencialDisplayDTO> matAl1 = inscripcionModel.getMatriculasAlumno("AL1");
		MatriculaMicrocredencialDisplayDTO al1Mc1 = matAl1.stream().filter(m -> "MC1".equals(m.getMicrocredencialId())).findFirst().orElseThrow();
		MatriculaMicrocredencialDisplayDTO al1Mc2 = matAl1.stream().filter(m -> "MC2".equals(m.getMicrocredencialId())).findFirst().orElseThrow();

		assertAll(
				() -> assertEquals("CANCELADA", al1Mc1.getEstado()),
				() -> assertEquals("BAJA_DEMANDA", al1Mc1.getMotivoCancelacion()),
				() -> assertEquals("2026-10-14", al1Mc1.getFechaBaja()),
				() -> assertEquals("Cancelada por baja demanda", al1Mc1.getEstadoLegible()),
				() -> assertEquals("INSCRITA", al1Mc2.getEstado()),
				() -> assertNull(al1Mc2.getMotivoCancelacion()));

		// AL2 tenía MC1 en SOLICITADA sin pagar (pasa a CANCELADA por IMPAGO)
		List<MatriculaMicrocredencialDisplayDTO> matAl2 = inscripcionModel.getMatriculasAlumno("AL2");
		MatriculaMicrocredencialDisplayDTO al2Mc1 = matAl2.stream().filter(m -> "MC1".equals(m.getMicrocredencialId())).findFirst().orElseThrow();

		assertAll(
				() -> assertEquals("CANCELADA", al2Mc1.getEstado()),
				() -> assertEquals("IMPAGO", al2Mc1.getMotivoCancelacion()),
				() -> assertEquals("2026-10-14", al2Mc1.getFechaBaja()),
				() -> assertEquals("Cancelada por impago", al2Mc1.getEstadoLegible()));
	}

	@Test
	@DisplayName("HU 36410: Antes del día previo al cierre de matriculación no se cancela ninguna microcredencial")
	public void testNoCancelaAntesDeFechaEvaluacion() {
		microModel.cancelarPorBajaDemanda("2026-10-13");

		for (MicrocredencialDisplayDTO mc : microModel.getMicrocredenciales(PROFESOR)) {
			assertEquals("ACEPTADA", mc.getEstado());
			assertNull(mc.getFechaCancelacion());
		}
	}

	@Test
	@DisplayName("HU 36410: Validación de fecha en cancelarPorBajaDemanda")
	public void testFechaInvalidaLanzaValidationException() {
		assertThrows(ValidationException.class, () -> microModel.cancelarPorBajaDemanda(""));
		assertThrows(ValidationException.class, () -> microModel.cancelarPorBajaDemanda("14/10/2026"));
	}
}
