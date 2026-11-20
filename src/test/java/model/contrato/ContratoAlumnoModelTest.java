package model.contrato;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import util.Database;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Pruebas de la HU 37045: el secretario registra y consulta los contratos de los alumnos.
 * Cada prueba parte de sus propios datos (loadCleanDatabase), independientes de data.sql
 */
@Disabled("Pendiente de la tabla ContratoAlumno en schema.sql (la añade otro miembro del equipo)")
public class ContratoAlumnoModelTest {
	private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private ContratoAlumnoModel model;

	@BeforeEach
	public void setUp() {
		db.createDatabase(false);
		loadCleanDatabase(db);
		model = new ContratoAlumnoModel(db);
	}

	/** Los contratos impedirían a otras pruebas borrar los alumnos (clave foránea) */
	@AfterEach
	public void tearDown() {
		db.executeUpdate("DELETE FROM ContratoAlumno");
	}

	/**
	 * Datos de prueba:
	 * - AL0001 tiene dos contratos que se solapan (CT0001 y CT0003) y uno ya terminado (CT0002)
	 * - AL0002 no tiene contratos ni matrícula en ningún grado
	 */
	public static void loadCleanDatabase(Database db) {
		db.executeBatch(new String[] {
				"DELETE FROM ContratoAlumno", "DELETE FROM RevisionGrado", "DELETE FROM MatriculaMicrocredencial",
				"DELETE FROM Microcredencial", "DELETE FROM Calificacion", "DELETE FROM MatriculaGrupo",
				"DELETE FROM MatriculaCurso", "DELETE FROM Matricula", "DELETE FROM Alumno",
				"INSERT INTO Alumno VALUES ('AL0001','10000001A','Lucia','Pascual Santos','lpascual@alumno.es')",
				"INSERT INTO Alumno VALUES ('AL0002','10000002B','Pablo','Garcia Ruiz','pgarcia@alumno.es')",
				"INSERT INTO ContratoAlumno VALUES ('CT0001','AL0001','Becaria del departamento','2026-09-01','2027-06-30')",
				"INSERT INTO ContratoAlumno VALUES ('CT0002','AL0001','Colaboracion en biblioteca','2025-10-01','2026-06-30')",
				"INSERT INTO ContratoAlumno VALUES ('CT0003','AL0001','Apoyo a la docencia','2027-01-15','2027-07-15')"
		});
	}

	// //////////////////////////////////////////////
	// Historial
	// //////////////////////////////////////////////

	/** El historial tiene todos los contratos del alumno, del que empieza más tarde al que empieza antes */
	@Test
	public void testHistorialOrdenado() {
		assertEquals(List.of("CT0003", "CT0001", "CT0002"), ids(model.getContratos("AL0001")));
		ContratoAlumnoDTO c = model.getContratos("AL0001").get(1);
		assertAll(() -> assertEquals("AL0001", c.getAlumnoId()),
				() -> assertEquals("Becaria del departamento", c.getDescripcion()),
				() -> assertEquals("2026-09-01", c.getFechaInicio()),
				() -> assertEquals("2027-06-30", c.getFechaFin()));
	}

	/** Un alumno sin contratos tiene el historial vacío */
	@Test
	public void testHistorialVacio() {
		assertTrue(model.getContratos("AL0002").isEmpty());
	}

	/** El historial de un alumno que no existe da error de recurso no encontrado */
	@Test
	public void testHistorialAlumnoInexistente() {
		assertThrows(ResourceNotFoundException.class, () -> model.getContratos("AL9999"));
		assertThrows(ValidationException.class, () -> model.getContratos(" "));
	}

	// //////////////////////////////////////////////
	// Alta de contratos
	// //////////////////////////////////////////////

	/** Un contrato válido se guarda con el siguiente código y aparece en el historial; los textos se guardan sin espacios */
	@Test
	public void testRegistrarContrato() {
		String id = model.registrarContrato(" AL0002 ", "  Becario de investigacion ", "2026-11-01", "2027-10-31");
		assertEquals("CT0004", id);
		List<ContratoAlumnoDTO> contratos = model.getContratos("AL0002");
		assertEquals(1, contratos.size());
		ContratoAlumnoDTO c = contratos.get(0);
		assertAll(() -> assertEquals("CT0004", c.getContratoId()),
				() -> assertEquals("Becario de investigacion", c.getDescripcion()),
				() -> assertEquals("2026-11-01", c.getFechaInicio()),
				() -> assertEquals("2027-10-31", c.getFechaFin()));
	}

	/** No hace falta estar matriculado: AL0002 no tiene ninguna matrícula y se le registra el contrato */
	@Test
	public void testSinMatriculaSePuedeRegistrar() {
		assertTrue(db.executeQueryArray("SELECT 1 FROM Matricula WHERE alumnoId = 'AL0002'").isEmpty());
		model.registrarContrato("AL0002", "Becario", "2026-11-01", "2027-01-31");
		assertEquals(1, model.getContratos("AL0002").size());
	}

	/** Un alumno puede tener varios contratos a la vez, aunque se solapen */
	@Test
	public void testContratosSolapados() {
		model.registrarContrato("AL0001", "Segunda beca", "2026-10-01", "2027-02-28");
		assertEquals(4, model.getContratos("AL0001").size());
	}

	/** Se admiten contratos pasados (retroactivos) y futuros */
	@Test
	public void testContratosPasadosYFuturos() {
		model.registrarContrato("AL0002", "Contrato antiguo", "2020-01-01", "2020-12-31");
		model.registrarContrato("AL0002", "Contrato futuro", "2030-01-01", "2030-06-30");
		assertEquals(List.of("CT0005", "CT0004"), ids(model.getContratos("AL0002")));
	}

	/** El contrato puede durar un solo día (inicio = fin), pero el fin no puede ser anterior al inicio */
	@Test
	public void testFechaFinNoAnterior() {
		model.registrarContrato("AL0002", "Un dia", "2026-12-01", "2026-12-01");
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.registrarContrato("AL0002", "Al reves", "2026-12-02", "2026-12-01"));
		assertEquals("La fecha de fin no puede ser anterior a la de inicio", e.getMessage());
		assertEquals(1, model.getContratos("AL0002").size());
	}

	/** Las fechas y la descripción son obligatorias; nada se guarda si falta alguna */
	@Test
	public void testCamposObligatorios() {
		assertThrows(ValidationException.class, () -> model.registrarContrato("AL0002", null, "2026-11-01", "2027-01-31"));
		assertThrows(ValidationException.class, () -> model.registrarContrato("AL0002", "   ", "2026-11-01", "2027-01-31"));
		assertThrows(ValidationException.class, () -> model.registrarContrato("AL0002", "Becario", null, "2027-01-31"));
		assertThrows(ValidationException.class, () -> model.registrarContrato("AL0002", "Becario", "2026-11-01", " "));
		assertTrue(model.getContratos("AL0002").isEmpty());
	}

	/** Las fechas tienen que ser fechas reales en formato AAAA-MM-DD */
	@Test
	public void testFechasNoValidas() {
		assertThrows(ValidationException.class,
				() -> model.registrarContrato("AL0002", "Becario", "01/11/2026", "2027-01-31"));
		assertThrows(ValidationException.class,
				() -> model.registrarContrato("AL0002", "Becario", "2026-11-01", "2027-02-30"));
		assertTrue(model.getContratos("AL0002").isEmpty());
	}

	/** La descripción cabe hasta 500 caracteres */
	@Test
	public void testLimiteDescripcion() {
		String limite = "x".repeat(ContratoAlumnoModel.MAX_DESCRIPCION);
		assertThrows(ValidationException.class,
				() -> model.registrarContrato("AL0002", limite + "x", "2026-11-01", "2027-01-31"));
		model.registrarContrato("AL0002", limite, "2026-11-01", "2027-01-31");
		assertEquals(limite, model.getContratos("AL0002").get(0).getDescripcion());
	}

	/** No se registran contratos de alumnos que no existen */
	@Test
	public void testAlumnoInexistente() {
		assertThrows(ResourceNotFoundException.class,
				() -> model.registrarContrato("AL9999", "Becario", "2026-11-01", "2027-01-31"));
	}

	// //////////////////////////////////////////////
	// Contrato vigente (requisito de la HU 37046)
	// //////////////////////////////////////////////

	/** El contrato está vigente desde su inicio hasta su fin, ambos incluidos */
	@Test
	public void testContratoVigente() {
		assertTrue(model.tieneContratoVigente("AL0001", "2026-09-01"));
		assertTrue(model.tieneContratoVigente("AL0001", "2027-07-15"));
		assertTrue(model.tieneContratoVigente("AL0001", "2026-03-15"));
		assertFalse(model.tieneContratoVigente("AL0001", "2026-08-15"));
		assertFalse(model.tieneContratoVigente("AL0001", "2027-07-16"));
		assertFalse(model.tieneContratoVigente("AL0002", "2026-11-01"));
	}

	private static List<String> ids(List<ContratoAlumnoDTO> contratos) {
		return contratos.stream().map(ContratoAlumnoDTO::getContratoId).collect(Collectors.toList());
	}
}
