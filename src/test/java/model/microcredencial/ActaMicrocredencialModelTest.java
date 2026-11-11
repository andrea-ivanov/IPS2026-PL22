package model.microcredencial;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import util.Database;
import util.exception.ValidationException;

/**
 * Pruebas de la HU 36459: acta de una microcredencial (el profesor califica y la cierra, o la cierra el sistema
 * a los 15 días del fin de la impartición) y lo que ve el alumno.
 */
public class ActaMicrocredencialModelTest {
	private static final String SOLO_SU_PROFESOR = "Solo el profesor que imparte la microcredencial";

	private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private ActaMicrocredencialModel model;

	@BeforeEach
	public void setUp() {
		db.createDatabase(false);
		loadCleanDatabase(db);
		model = new ActaMicrocredencialModel(db);
	}

	/**
	 * Escenario (año 2026-2027, grado G1 de Ingenieria y Arquitectura):
	 * - MC1 (P001) ACEPTADA, impartición 2026-10-05..2026-10-23. M1 INSCRITA 8.5, M2 INSCRITA sin nota,
	 *   M3 ABANDONADA, M4 CANCELADA por IMPAGO, M5 INSCRITA 4, M6 SOLICITADA (transferencia sin validar).
	 * - MC2 (P001) ACEPTADA, impartición 2026-11-16..2026-12-18 (aún no ha empezado el 2026-11-11). M1 INSCRITA.
	 * - MC3 (P002) ACEPTADA, impartición 2026-10-05..2026-10-23. M2 INSCRITA 7.
	 * - MC4 (P001) FINALIZADA, acta cerrada por el profesor el 2026-10-30. M1 FINALIZADA sin nota (NP), M5 FINALIZADA 6.
	 * Orden alfabético en el acta: AL1 (Alonso), AL2 (Blanco), AL5 (Castro), AL6 (Diez).
	 */
	public static void loadCleanDatabase(Database db) {
		db.executeBatch(new String[] {
				"DELETE FROM MatriculaMicrocredencial",
				"DELETE FROM Microcredencial",
				"DELETE FROM Calificacion",
				"DELETE FROM MatriculaGrupo",
				"DELETE FROM MatriculaCurso",
				"DELETE FROM Matricula",
				"DELETE FROM RevisionGrado",
				"DELETE FROM GradoCurso",
				"DELETE FROM Grado",
				"DELETE FROM Alumno",
				"DELETE FROM PropuestaCurso",
				"DELETE FROM Imparte",
				"DELETE FROM Grupo",
				"DELETE FROM Acta",
				"DELETE FROM Convocatoria",
				"DELETE FROM Curso",
				"DELETE FROM Profesor",
				"DELETE FROM AnioAcademico",
				"INSERT INTO AnioAcademico VALUES ('2026-2027','2026-09-01','2027-08-31','2026-07-01','2026-09-30')",
				"INSERT INTO Grado VALUES ('G1','Ingenieria Informatica','Escuela','Ingenieria y Arquitectura','VIGENTE','2024-09-01')",
				"INSERT INTO Profesor VALUES ('P001','10000001A','Ana','Garcia','p001@uni.es')",
				"INSERT INTO Profesor VALUES ('P002','10000002B','Luis','Diaz','p002@uni.es')",
				"INSERT INTO Alumno VALUES ('AL1','20000001A','Juan','Alonso','al1@uni.es')",
				"INSERT INTO Alumno VALUES ('AL2','20000002B','Maria','Blanco','al2@uni.es')",
				"INSERT INTO Alumno VALUES ('AL3','20000003C','Pedro','Zapata','al3@uni.es')",
				"INSERT INTO Alumno VALUES ('AL4','20000004D','Lucia','Yuste','al4@uni.es')",
				"INSERT INTO Alumno VALUES ('AL5','20000005E','Carlos','Castro','al5@uni.es')",
				"INSERT INTO Alumno VALUES ('AL6','20000006F','Elena','Diez','al6@uni.es')",
				"INSERT INTO Matricula VALUES ('M1','AL1','G1','2026-2027','2026-07-10','ACTIVA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M2','AL2','G1','2026-2027','2026-07-10','ACTIVA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M3','AL3','G1','2026-2027','2026-07-10','ACTIVA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M4','AL4','G1','2026-2027','2026-07-10','ACTIVA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M5','AL5','G1','2026-2027','2026-07-10','ACTIVA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M6','AL6','G1','2026-2027','2026-07-10','ACTIVA',NULL,NULL)",
				"INSERT INTO Microcredencial VALUES ('MC1','2026-2027','P001','Taller de Git','Ingenieria y Arquitectura',3,30,"
						+ "'2026-09-14','2026-09-28','2026-10-05','2026-10-23','ACEPTADA','2026-08-01','2026-08-10',NULL,NULL,NULL,0)",
				"INSERT INTO Microcredencial VALUES ('MC2','2026-2027','P001','Taller de JUnit','Ingenieria y Arquitectura',3,30,"
						+ "'2026-10-26','2026-11-09','2026-11-16','2026-12-18','ACEPTADA','2026-08-01','2026-08-10',NULL,NULL,NULL,0)",
				"INSERT INTO Microcredencial VALUES ('MC3','2026-2027','P002','Taller de Docker','Ingenieria y Arquitectura',3,30,"
						+ "'2026-09-14','2026-09-28','2026-10-05','2026-10-23','ACEPTADA','2026-08-01','2026-08-10',NULL,NULL,NULL,0)",
				"INSERT INTO Microcredencial VALUES ('MC4','2026-2027','P001','Taller de Linux','Ingenieria y Arquitectura',3,30,"
						+ "'2026-09-14','2026-09-28','2026-10-05','2026-10-23','FINALIZADA','2026-08-01','2026-08-10',NULL,NULL,'2026-10-30',0)",
				"INSERT INTO MatriculaMicrocredencial VALUES ('M1','2026-2027','MC1','2026-09-15','INSCRITA',NULL,NULL,60,'TARJETA','2026-09-15',1,8.5)",
				"INSERT INTO MatriculaMicrocredencial VALUES ('M2','2026-2027','MC1','2026-09-15','INSCRITA',NULL,NULL,60,'TARJETA','2026-09-15',1,NULL)",
				"INSERT INTO MatriculaMicrocredencial VALUES ('M3','2026-2027','MC1','2026-09-15','ABANDONADA',NULL,'2026-10-10',60,'TARJETA','2026-09-15',1,NULL)",
				"INSERT INTO MatriculaMicrocredencial VALUES ('M4','2026-2027','MC1','2026-09-15','CANCELADA','IMPAGO','2026-09-27',60,NULL,NULL,0,NULL)",
				"INSERT INTO MatriculaMicrocredencial VALUES ('M5','2026-2027','MC1','2026-09-15','INSCRITA',NULL,NULL,60,'TARJETA','2026-09-15',1,4)",
				"INSERT INTO MatriculaMicrocredencial VALUES ('M6','2026-2027','MC1','2026-09-15','SOLICITADA',NULL,NULL,60,'TRANSFERENCIA','2026-09-16',0,NULL)",
				"INSERT INTO MatriculaMicrocredencial VALUES ('M1','2026-2027','MC2','2026-10-27','INSCRITA',NULL,NULL,60,'TARJETA','2026-10-27',1,NULL)",
				"INSERT INTO MatriculaMicrocredencial VALUES ('M2','2026-2027','MC3','2026-09-15','INSCRITA',NULL,NULL,60,'TARJETA','2026-09-15',1,7)",
				"INSERT INTO MatriculaMicrocredencial VALUES ('M1','2026-2027','MC4','2026-09-15','FINALIZADA',NULL,NULL,60,'TARJETA','2026-09-15',1,NULL)",
				"INSERT INTO MatriculaMicrocredencial VALUES ('M5','2026-2027','MC4','2026-09-15','FINALIZADA',NULL,NULL,60,'TARJETA','2026-09-15',1,6)"
		});
	}

	// =========================================================================
	// Restricciones de la BD confirmadas por el PO
	// =========================================================================

	@Test
	@DisplayName("La BD no admite cerrar el acta antes del fin de la microcredencial")
	public void testBdNoAdmiteCierreAntesDelFin() {
		assertThrows(RuntimeException.class, () -> db.executeUpdate(
				"UPDATE Microcredencial SET fechaCierreActa = '2026-10-22' WHERE microcredencialId = 'MC4'"));
		assertDoesNotThrow(() -> db.executeUpdate(
				"UPDATE Microcredencial SET fechaCierreActa = '2026-10-23' WHERE microcredencialId = 'MC4'"));
	}

	@Test
	@DisplayName("La BD no admite un cierre manual pasados los 15 días del fin")
	public void testBdNoAdmiteCierreManualFueraDePlazo() {
		assertThrows(RuntimeException.class, () -> db.executeUpdate(
				"UPDATE Microcredencial SET fechaCierreActa = '2026-11-08' WHERE microcredencialId = 'MC4'"));
		assertDoesNotThrow(() -> db.executeUpdate(
				"UPDATE Microcredencial SET fechaCierreActa = '2026-11-07' WHERE microcredencialId = 'MC4'"));
	}

	@Test
	@DisplayName("El cierre automático lleva siempre la fecha de fin + 15 días")
	public void testBdCierreAutomaticoSoloAlVencerPlazo() {
		assertThrows(RuntimeException.class, () -> db.executeUpdate(
				"UPDATE Microcredencial SET cierreAutomatico = 1 WHERE microcredencialId = 'MC4'"));
		assertDoesNotThrow(() -> db.executeUpdate("UPDATE Microcredencial SET cierreAutomatico = 1, "
				+ "fechaCierreActa = '2026-11-07' WHERE microcredencialId = 'MC4'"));
	}

	// =========================================================================
	// Consulta del acta
	// =========================================================================

	@Test
	@DisplayName("En el acta salen solo las inscripciones pagadas, por apellidos, y sin nota si no la tienen")
	public void testActaSoloInscripcionesPagadas() {
		List<CalificacionMicrocredencialDTO> acta = model.getCalificaciones("P001", "MC1");
		assertEquals(List.of("M1", "M2", "M5"), matriculas(acta));
		assertEquals(List.of("8.5", "", "4"), notas(acta));
		assertEquals("AL1", acta.get(0).getAlumnoId());
		assertEquals("Juan Alonso", acta.get(0).getAlumno());
		assertEquals("20000001A", acta.get(0).getDni());
	}

	@Test
	@DisplayName("Solo el profesor que imparte la microcredencial ve su acta")
	public void testOtroProfesorNoVeElActa() {
		ValidationException e = assertThrows(ValidationException.class, () -> model.getCalificaciones("P002", "MC1"));
		assertTrue(e.getMessage().contains(SOLO_SU_PROFESOR), e.getMessage());
		e = assertThrows(ValidationException.class, () -> model.getCalificaciones("P001", "MC999"));
		assertTrue(e.getMessage().contains(SOLO_SU_PROFESOR), e.getMessage());
	}

	@Test
	@DisplayName("En un acta cerrada el alumno sin nota sale como NP")
	public void testActaCerradaMuestraNP() {
		List<CalificacionMicrocredencialDTO> acta = model.getCalificaciones("P001", "MC4");
		assertEquals(List.of("M1", "M5"), matriculas(acta));
		assertEquals(List.of("NP", "6"), notas(acta));
	}

	@Test
	@DisplayName("Las notas se ponen desde el inicio de la impartición y solo con el acta abierta")
	public void testPuedeEditarNotasSegunFechas() {
		MicrocredencialDisplayDTO mc1 = model.getMicrocredencialDelProfesor("P001", "MC1");
		assertFalse(ActaMicrocredencialModel.puedeEditarNotas(mc1, "2026-10-04"));
		assertTrue(ActaMicrocredencialModel.puedeEditarNotas(mc1, "2026-10-05"));
		assertTrue(ActaMicrocredencialModel.puedeEditarNotas(mc1, "2026-11-07"));
		assertFalse(ActaMicrocredencialModel.puedeEditarNotas(mc1, "2026-11-08"), "Pasado el plazo de cierre, no");
		MicrocredencialDisplayDTO mc4 = model.getMicrocredencialDelProfesor("P001", "MC4");
		assertFalse(ActaMicrocredencialModel.actaAbierta(mc4));
		assertFalse(ActaMicrocredencialModel.puedeEditarNotas(mc4, "2026-10-20"));
	}

	@Test
	@DisplayName("El acta se cierra desde el fin de la impartición hasta 15 días después")
	public void testPuedeCerrarActaSegunFechas() {
		MicrocredencialDisplayDTO mc1 = model.getMicrocredencialDelProfesor("P001", "MC1");
		assertTrue(ActaMicrocredencialModel.actaAbierta(mc1));
		assertFalse(ActaMicrocredencialModel.puedeCerrarActa(mc1, "2026-10-22"));
		assertTrue(ActaMicrocredencialModel.puedeCerrarActa(mc1, "2026-10-23"));
		assertTrue(ActaMicrocredencialModel.puedeCerrarActa(mc1, "2026-11-07"));
		assertFalse(ActaMicrocredencialModel.puedeCerrarActa(mc1, "2026-11-08"));
		MicrocredencialDisplayDTO mc4 = model.getMicrocredencialDelProfesor("P001", "MC4");
		assertFalse(ActaMicrocredencialModel.puedeCerrarActa(mc4, "2026-10-25"));
	}

	@Test
	@DisplayName("En 'Mis microcredenciales' una FINALIZADA dice cuándo y quién cerró el acta")
	public void testDetalleEstadoActaCerrada() {
		assertEquals("Acta cerrada el 2026-10-30", detalle("MC4"));
		db.executeUpdate("UPDATE Microcredencial SET cierreAutomatico = 1, fechaCierreActa = '2026-11-07' "
				+ "WHERE microcredencialId = 'MC4'");
		assertEquals("Acta cerrada por el sistema el 2026-11-07", detalle("MC4"));
	}

	// =========================================================================
	// Poner y cambiar notas
	// =========================================================================

	@Test
	@DisplayName("El profesor pone y cambia notas; vacío o NP dejan al alumno sin nota")
	public void testGuardarYCambiarNotas() {
		model.guardarNotas("P001", "MC1", notas("M1", "9,25", "M2", "5", "M5", ""), "2026-10-20");
		assertEquals(9.25, nota("M1", "MC1"));
		assertEquals(5.0, nota("M2", "MC1"));
		assertNull(nota("M5", "MC1"));

		model.guardarNotas("P001", "MC1", notas("M2", "NP"), "2026-10-20");
		assertNull(nota("M2", "MC1"));
		assertEquals(9.25, nota("M1", "MC1"), "Las notas que no se envían no cambian");
	}

	@Test
	@DisplayName("Las notas van de 0 a 10")
	public void testNotasFueraDeRango() {
		for (String mala : List.of("10.5", "-1", "abc")) {
			ValidationException e = assertThrows(ValidationException.class,
					() -> model.guardarNotas("P001", "MC1", notas("M1", mala), "2026-10-20"));
			assertTrue(e.getMessage().contains("entre 0 y 10"), e.getMessage());
		}
		model.guardarNotas("P001", "MC1", notas("M1", "0", "M2", "10"), "2026-10-20");
		assertEquals(0.0, nota("M1", "MC1"));
		assertEquals(10.0, nota("M2", "MC1"));
	}

	@Test
	@DisplayName("Si una nota no es válida no se guarda ninguna")
	public void testNotaInvalidaNoGuardaNinguna() {
		assertThrows(ValidationException.class,
				() -> model.guardarNotas("P001", "MC1", notas("M1", "7", "M2", "11"), "2026-10-20"));
		assertEquals(8.5, nota("M1", "MC1"));
		assertNull(nota("M2", "MC1"));
	}

	@Test
	@DisplayName("No se ponen notas antes de que empiece la microcredencial")
	public void testNotasAntesDeEmpezarImparticion() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.guardarNotas("P001", "MC2", notas("M1", "7"), "2026-11-11"));
		assertEquals("La microcredencial todavía no ha empezado (inicio: 2026-11-16)", e.getMessage());
		assertNull(nota("M1", "MC2"));
	}

	@Test
	@DisplayName("Pasado el plazo de cierre ya no se cambian notas: el sistema cerrará el acta con fecha fin + 15")
	public void testNotasFueraDePlazoDeCierre() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.guardarNotas("P001", "MC1", notas("M1", "3"), "2026-11-08"));
		assertEquals("El plazo para cerrar el acta terminó el 2026-11-07: ya no se pueden cambiar las notas",
				e.getMessage());
		assertEquals(8.5, nota("M1", "MC1"));
	}

	@Test
	@DisplayName("Con el acta cerrada las notas no se pueden cambiar")
	public void testNotasConActaCerrada() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.guardarNotas("P001", "MC4", notas("M5", "9"), "2026-11-01"));
		assertEquals("El acta está cerrada: las notas ya no se pueden cambiar", e.getMessage());
		assertEquals(6.0, nota("M5", "MC4"));
	}

	@Test
	@DisplayName("Solo el profesor que imparte la microcredencial pone notas")
	public void testOtroProfesorNoPuedeCalificar() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.guardarNotas("P002", "MC1", notas("M1", "2"), "2026-10-20"));
		assertTrue(e.getMessage().contains(SOLO_SU_PROFESOR), e.getMessage());
		assertEquals(8.5, nota("M1", "MC1"));
	}

	@Test
	@DisplayName("No se califica a quien no está en el acta (abandonó, sin pagar u otra matrícula)")
	public void testNotaDeInscripcionFueraDelActaNoGuardaNada() {
		for (String fuera : List.of("M3", "M4", "M6", "M999")) {
			ValidationException e = assertThrows(ValidationException.class,
					() -> model.guardarNotas("P001", "MC1", notas("M1", "7", fuera, "6"), "2026-10-20"));
			assertEquals("La matrícula " + fuera + " no está en el acta", e.getMessage());
			assertEquals(8.5, nota("M1", "MC1"), "No se guarda ninguna nota del lote");
		}
		assertNull(nota("M3", "MC1"));
	}

	// =========================================================================
	// Cierre manual
	// =========================================================================

	@Test
	@DisplayName("Al cerrar el acta la microcredencial y sus inscritos pasan a FINALIZADA con la fecha de cierre")
	public void testCerrarActa() {
		model.cerrarActa("P001", "MC1", "2026-10-29");

		assertEquals(List.of("FINALIZADA", "2026-10-29", "0"), microcredencial("MC1"));
		assertEquals("FINALIZADA", estado("M1", "MC1"));
		assertEquals("FINALIZADA", estado("M2", "MC1"));
		assertEquals("FINALIZADA", estado("M5", "MC1"));
		assertEquals(8.5, nota("M1", "MC1"));
		assertNull(nota("M2", "MC1"));
		assertEquals("ABANDONADA", estado("M3", "MC1"));
		assertEquals("CANCELADA", estado("M4", "MC1"));
		assertEquals("SOLICITADA", estado("M6", "MC1"));
		assertEquals(List.of("8.5", "NP", "4"), notas(model.getCalificaciones("P001", "MC1")));
		assertEquals(Arrays.asList("ACEPTADA", null, "0"), microcredencial("MC2"), "Las demás no cambian");
	}

	@Test
	@DisplayName("El acta no se puede cerrar antes del fin de la microcredencial")
	public void testNoSeCierraAntesDelFin() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.cerrarActa("P001", "MC1", "2026-10-22"));
		assertEquals("No se puede cerrar el acta hasta el fin de la microcredencial (2026-10-23)", e.getMessage());
		assertEquals(Arrays.asList("ACEPTADA", null, "0"), microcredencial("MC1"));
		assertEquals("INSCRITA", estado("M1", "MC1"));
	}

	@Test
	@DisplayName("Pasado el plazo de 15 días el profesor ya no la cierra: la cierra el sistema")
	public void testNoSeCierraFueraDePlazo() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.cerrarActa("P001", "MC1", "2026-11-08"));
		assertEquals("El plazo para cerrar el acta terminó el 2026-11-07: la cerrará el sistema", e.getMessage());
		assertEquals(Arrays.asList("ACEPTADA", null, "0"), microcredencial("MC1"));
		model.cerrarActa("P001", "MC1", "2026-11-07");
		assertEquals(List.of("FINALIZADA", "2026-11-07", "0"), microcredencial("MC1"));
	}

	@Test
	@DisplayName("Un acta cerrada no se vuelve a cerrar")
	public void testNoSeCierraDosVeces() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.cerrarActa("P001", "MC4", "2026-11-01"));
		assertEquals("El acta está cerrada: las notas ya no se pueden cambiar", e.getMessage());
		assertEquals(List.of("FINALIZADA", "2026-10-30", "0"), microcredencial("MC4"));
	}

	@Test
	@DisplayName("Solo el profesor que imparte la microcredencial cierra su acta")
	public void testOtroProfesorNoPuedeCerrar() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.cerrarActa("P002", "MC1", "2026-10-29"));
		assertTrue(e.getMessage().contains(SOLO_SU_PROFESOR), e.getMessage());
		assertEquals(Arrays.asList("ACEPTADA", null, "0"), microcredencial("MC1"));
	}

	@Test
	@DisplayName("Con el acta cerrada las notas ya no cambian")
	public void testNotasNoCambianTrasCerrar() {
		model.cerrarActa("P001", "MC1", "2026-10-29");
		assertThrows(ValidationException.class,
				() -> model.guardarNotas("P001", "MC1", notas("M1", "3"), "2026-10-30"));
		assertEquals(8.5, nota("M1", "MC1"));
	}

	@Test
	@DisplayName("El alumno solo ve su nota en 'Mis matriculaciones' con el acta cerrada")
	public void testAlumnoVeNotaSoloConActaCerrada() {
		InscripcionMicrocredencialModel alumno = new InscripcionMicrocredencialModel(db);
		assertEquals("Pendiente", notaAlumno(alumno, "AL1", "MC1"));
		assertEquals("Pendiente", notaAlumno(alumno, "AL2", "MC1"));

		model.cerrarActa("P001", "MC1", "2026-10-29");

		assertEquals("8.5", notaAlumno(alumno, "AL1", "MC1"));
		assertEquals("NP", notaAlumno(alumno, "AL2", "MC1"));
		assertEquals("Pendiente", notaAlumno(alumno, "AL1", "MC2"), "Las demás actas siguen abiertas");
	}

	// =========================================================================
	// Cierre automático
	// =========================================================================

	@Test
	@DisplayName("El sistema cierra el acta cuando pasan 15 días del fin, no antes, con fecha fin + 15")
	public void testCierreAutomaticoRespetaElPlazo() {
		assertEquals(0, model.cerrarActasVencidas("2026-11-07"));
		assertEquals(Arrays.asList("ACEPTADA", null, "0"), microcredencial("MC1"));

		assertEquals(2, model.cerrarActasVencidas("2026-11-08"));
		assertEquals(List.of("FINALIZADA", "2026-11-07", "1"), microcredencial("MC1"));
		assertEquals(List.of("FINALIZADA", "2026-11-07", "1"), microcredencial("MC3"));
		assertEquals(Arrays.asList("ACEPTADA", null, "0"), microcredencial("MC2"));
		assertEquals(List.of("FINALIZADA", "2026-10-30", "0"), microcredencial("MC4"));
	}

	@Test
	@DisplayName("Aunque el sistema la cierre más tarde, la fecha de cierre es fin + 15")
	public void testCierreAutomaticoTardioGuardaFechaDelPlazo() {
		assertEquals(2, model.cerrarActasVencidas("2026-12-01"));
		assertEquals(List.of("FINALIZADA", "2026-11-07", "1"), microcredencial("MC1"));
	}

	@Test
	@DisplayName("El cierre automático conserva las notas puestas; quien no tiene nota queda como NP")
	public void testCierreAutomaticoConservaNotas() {
		model.cerrarActasVencidas("2026-11-08");
		assertEquals(List.of("8.5", "NP", "4"), notas(model.getCalificaciones("P001", "MC1")));
		assertEquals("FINALIZADA", estado("M1", "MC1"));
		assertEquals("FINALIZADA", estado("M2", "MC3"));
		assertEquals(7.0, nota("M2", "MC3"));
		assertEquals("INSCRITA", estado("M1", "MC2"), "Las inscripciones de actas abiertas no cambian");
	}

	@Test
	@DisplayName("El cierre automático no toca abandonadas, canceladas ni solicitadas")
	public void testCierreAutomaticoNoTocaSolicitadasNiAbandonadas() {
		model.cerrarActasVencidas("2026-11-08");
		assertEquals("ABANDONADA", estado("M3", "MC1"));
		assertEquals("CANCELADA", estado("M4", "MC1"));
		assertEquals("SOLICITADA", estado("M6", "MC1"));
	}

	@Test
	@DisplayName("Volver a ejecutar el cierre automático no cambia nada")
	public void testCierreAutomaticoIdempotente() {
		assertEquals(2, model.cerrarActasVencidas("2026-11-08"));
		assertEquals(0, model.cerrarActasVencidas("2026-11-09"));
		assertEquals(List.of("FINALIZADA", "2026-11-07", "1"), microcredencial("MC1"));
	}

	// =========================================================================
	// Auxiliares
	// =========================================================================

	/** estado, fechaCierreActa y cierreAutomatico de la microcredencial */
	private static List<String> microcredencial(String microcredencialId) {
		Object[] fila = db.executeQueryArray("SELECT estado, fechaCierreActa, cierreAutomatico FROM Microcredencial "
				+ "WHERE microcredencialId = ?", microcredencialId).get(0);
		List<String> valores = new ArrayList<>();
		for (Object v : fila) {
			valores.add(v == null ? null : v.toString());
		}
		return valores;
	}

	private static String estado(String matriculaId, String microcredencialId) {
		return (String) db.executeQueryArray("SELECT estado FROM MatriculaMicrocredencial "
				+ "WHERE matriculaId = ? AND microcredencialId = ?", matriculaId, microcredencialId).get(0)[0];
	}

	private static String notaAlumno(InscripcionMicrocredencialModel alumno, String alumnoId, String microcredencialId) {
		for (MatriculaMicrocredencialDisplayDTO m : alumno.getMatriculasAlumno(alumnoId)) {
			if (m.getMicrocredencialId().equals(microcredencialId)) {
				return m.getNotaTexto();
			}
		}
		return null;
	}

	/** Notas como las envía la tabla: matrícula, texto, matrícula, texto... */
	private static Map<String, String> notas(String... pares) {
		Map<String, String> notas = new LinkedHashMap<>();
		for (int i = 0; i < pares.length; i += 2) {
			notas.put(pares[i], pares[i + 1]);
		}
		return notas;
	}

	private static Double nota(String matriculaId, String microcredencialId) {
		Object valor = db.executeQueryArray("SELECT CAST(nota AS REAL) FROM MatriculaMicrocredencial "
				+ "WHERE matriculaId = ? AND microcredencialId = ?", matriculaId, microcredencialId).get(0)[0];
		return valor == null ? null : ((Number) valor).doubleValue();
	}

	private static List<String> matriculas(List<CalificacionMicrocredencialDTO> acta) {
		List<String> ids = new ArrayList<>();
		for (CalificacionMicrocredencialDTO c : acta) {
			ids.add(c.getMatriculaId());
		}
		return ids;
	}

	private static List<String> notas(List<CalificacionMicrocredencialDTO> acta) {
		List<String> notas = new ArrayList<>();
		for (CalificacionMicrocredencialDTO c : acta) {
			notas.add(c.getNotaTexto());
		}
		return notas;
	}

	private static String detalle(String microcredencialId) {
		for (MicrocredencialDisplayDTO mc : new MicrocredencialModel(db).getMicrocredenciales("P001")) {
			if (mc.getMicrocredencialId().equals(microcredencialId)) {
				return mc.getDetalleEstado();
			}
		}
		return null;
	}
}
