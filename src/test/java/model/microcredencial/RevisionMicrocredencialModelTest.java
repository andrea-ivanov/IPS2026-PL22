package model.microcredencial;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import util.Database;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Pruebas de la HU 35994 (primera mitad): el secretario ve las microcredenciales PROPUESTAS y las acepta.
 * Cada prueba parte de sus propios datos (loadCleanDatabase), independientes de data.sql
 */
public class RevisionMicrocredencialModelTest {
	private static final String HOY = "2026-11-04";

	private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private RevisionMicrocredencialModel model;

	@BeforeEach
	public void setUp() {
		db.createDatabase(false);
		loadCleanDatabase(db);
		model = new RevisionMicrocredencialModel(db);
	}

	/**
	 * Datos de prueba:
	 * - MC001 (Ana Garcia) y MC002 (Luis Fernandez) PROPUESTAS; MC002 se propuso antes
	 * - MC003 ACEPTADA y MC004 RECHAZADA: ya están resueltas y no salen como pendientes
	 */
	public static void loadCleanDatabase(Database db) {
		db.executeBatch(new String[] {
				"DELETE FROM RevisionGrado", "DELETE FROM MatriculaMicrocredencial", "DELETE FROM Microcredencial",
				"DELETE FROM Calificacion", "DELETE FROM MatriculaGrupo", "DELETE FROM MatriculaCurso",
				"DELETE FROM Matricula", "DELETE FROM Acta", "DELETE FROM Convocatoria", "DELETE FROM Imparte",
				"DELETE FROM Grupo", "DELETE FROM PropuestaCurso", "DELETE FROM GradoCurso", "DELETE FROM Curso",
				"DELETE FROM Grado", "DELETE FROM AnioAcademico", "DELETE FROM Profesor", "DELETE FROM Alumno",
				"INSERT INTO AnioAcademico VALUES ('2026-2027','2026-09-08','2027-07-31','2026-07-01','2026-09-30')",
				"INSERT INTO Profesor VALUES ('P001','10000001A','Ana','Garcia','p001@uni.es')",
				"INSERT INTO Profesor VALUES ('P002','10000002B','Luis','Fernandez','p002@uni.es')",
				"INSERT INTO Microcredencial VALUES ('MC001','2026-2027','P001','Docker Basico','Ingenieria y Arquitectura',"
						+ "3,25,'2027-02-01','2027-02-15','2027-02-22','2027-04-16','PROPUESTA','2026-10-29',NULL,NULL,NULL,NULL,0)",
				"INSERT INTO Microcredencial VALUES ('MC002','2026-2027','P002','Estadistica con R','Ciencias',"
						+ "1.5,40,'2027-01-10','2027-01-24','2027-02-01','2027-03-15','PROPUESTA','2026-10-20',NULL,NULL,NULL,NULL,0)",
				"INSERT INTO Microcredencial VALUES ('MC003','2026-2027','P001','Git Avanzado','Ingenieria y Arquitectura',"
						+ "2,30,'2027-01-10','2027-01-24','2027-02-01','2027-03-15','ACEPTADA','2026-10-01','2026-10-05',NULL,NULL,NULL,0)",
				"INSERT INTO Microcredencial VALUES ('MC004','2026-2027','P002','Negociacion','Ciencias Sociales y Juridicas',"
						+ "3,40,'2027-01-10','2027-01-24','2027-02-01','2027-03-15','RECHAZADA','2026-10-01','2026-10-06',"
						+ "'Contenido solapado',NULL,NULL,0)"
		});
	}

	// //////////////////////////////////////////////
	// Listado de pendientes
	// //////////////////////////////////////////////

	/** Solo salen las PROPUESTAS, de la más antigua a la más reciente */
	@Test
	public void testSoloPropuestasOrdenadas() {
		assertEquals(List.of("MC002", "MC001"), ids(model.getPropuestasPendientes()));
	}

	/** Cada pendiente trae los datos clave: nombre, profesor proponente, créditos y periodo de impartición */
	@Test
	public void testDatosClaveDeLaPropuesta() {
		PropuestaMicrocredencialDTO p = model.getPropuestasPendientes().get(1);
		assertAll(() -> assertEquals("MC001", p.getMicrocredencialId()),
				() -> assertEquals("Docker Basico", p.getNombre()),
				() -> assertEquals("P001", p.getProfesorId()),
				() -> assertEquals("Ana Garcia", p.getProfesor()),
				() -> assertEquals(3.0, p.getCreditos()),
				() -> assertEquals("2027-02-22", p.getInicioImparticion()),
				() -> assertEquals("2027-04-16", p.getFinImparticion()),
				() -> assertEquals("2027-02-22 - 2027-04-16", p.getPeriodoImparticion()),
				() -> assertEquals("Ingenieria y Arquitectura", p.getRamaConocimiento()),
				() -> assertEquals(25, p.getPlazas()),
				() -> assertEquals("2026-10-29", p.getFechaPropuesta()),
				() -> assertEquals("2026-2027", p.getAnioAcademicoId()));
		// los créditos con decimal se leen bien (DECIMAL(4,1))
		assertEquals(1.5, model.getPropuestasPendientes().get(0).getCreditos());
	}

	/** Sin propuestas pendientes la lista está vacía */
	@Test
	public void testSinPendientes() {
		db.executeUpdate("UPDATE Microcredencial SET estado = 'ACEPTADA', fechaResolucion = ? WHERE estado = 'PROPUESTA'", HOY);
		assertTrue(model.getPropuestasPendientes().isEmpty());
	}

	// //////////////////////////////////////////////
	// Aceptar
	// //////////////////////////////////////////////

	/** Al aceptarla pasa a ACEPTADA con la fecha de hoy como resolución y deja de estar pendiente */
	@Test
	public void testAceptarPropuesta() {
		model.aceptarPropuesta("MC001", HOY);

		assertEquals("ACEPTADA", estado("MC001"));
		assertEquals(HOY, fechaResolucion("MC001"));
		assertEquals(List.of("MC002"), ids(model.getPropuestasPendientes()));
		// la otra propuesta no cambia
		assertEquals("PROPUESTA", estado("MC002"));
		assertNull(fechaResolucion("MC002"));
	}

	/** El identificador se admite con espacios alrededor */
	@Test
	public void testAceptarConEspacios() {
		model.aceptarPropuesta("  MC002 ", HOY);
		assertEquals("ACEPTADA", estado("MC002"));
	}

	/** Una propuesta ya aceptada no se puede volver a aceptar ni cambia su fecha de resolución */
	@Test
	public void testAceptarDosVeces() {
		model.aceptarPropuesta("MC001", HOY);
		ValidationException e = assertThrows(ValidationException.class, () -> model.aceptarPropuesta("MC001", "2026-11-05"));
		assertTrue(e.getMessage().contains("ACEPTADA"), e.getMessage());
		assertEquals(HOY, fechaResolucion("MC001"));
	}

	/** Una propuesta RECHAZADA queda invalidada: no se puede aceptar */
	@Test
	public void testAceptarRechazada() {
		assertThrows(ValidationException.class, () -> model.aceptarPropuesta("MC004", HOY));
		assertEquals("RECHAZADA", estado("MC004"));
		assertEquals("2026-10-06", fechaResolucion("MC004"));
	}

	/** Una microcredencial que no existe da error de recurso no encontrado */
	@Test
	public void testAceptarInexistente() {
		assertThrows(ResourceNotFoundException.class, () -> model.aceptarPropuesta("MC999", HOY));
	}

	/** Sin identificador o sin fecha no se acepta nada */
	@Test
	public void testAceptarSinDatos() {
		assertThrows(ValidationException.class, () -> model.aceptarPropuesta(null, HOY));
		assertThrows(ValidationException.class, () -> model.aceptarPropuesta("  ", HOY));
		assertThrows(ValidationException.class, () -> model.aceptarPropuesta("MC001", null));
		assertEquals("PROPUESTA", estado("MC001"));
	}

	// //////////////////////////////////////////////
	// Auxiliares
	// //////////////////////////////////////////////

	private static List<String> ids(List<PropuestaMicrocredencialDTO> propuestas) {
		return propuestas.stream().map(PropuestaMicrocredencialDTO::getMicrocredencialId).collect(Collectors.toList());
	}

	private String estado(String id) {
		return (String) db.executeQueryArray("SELECT estado FROM Microcredencial WHERE microcredencialId = ?", id).get(0)[0];
	}

	private String fechaResolucion(String id) {
		Object valor = db.executeQueryArray("SELECT fechaResolucion FROM Microcredencial WHERE microcredencialId = ?", id)
				.get(0)[0];
		return valor == null ? null : valor.toString();
	}
}
