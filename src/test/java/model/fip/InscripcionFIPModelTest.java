package model.fip;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import model.contrato.ContratoAlumnoModelTest;
import util.Database;
import util.exception.ValidationException;

/**
 * Pruebas de la HU 37046 que no dependen de la tabla FIP: plazo de matrícula y contrato vigente del alumno.
 */
public class InscripcionFIPModelTest {
	private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private InscripcionFIPModel model;

	@BeforeEach
	public void setUp() {
		model = new InscripcionFIPModel(db);
	}

	// //////////////////////////////////////////////
	// Plazo (no usa la base de datos)
	// //////////////////////////////////////////////

	/** Se puede matricular hasta el día anterior al inicio de la FIP */
	@Test
	public void testDentroDelPlazo() {
		assertDoesNotThrow(() -> model.comprobarPlazo("2026-12-01", "2026-11-30"));
		assertDoesNotThrow(() -> model.comprobarPlazo("2026-12-01", "2026-10-15"));
	}

	/** Desde el día de inicio ya no se admiten altas */
	@Test
	public void testFueraDelPlazo() {
		ValidationException e = assertThrows(ValidationException.class, () -> model.comprobarPlazo("2026-12-01", "2026-12-01"));
		assertTrue(e.getMessage().contains("2026-11-30"), e.getMessage());
		assertThrows(ValidationException.class, () -> model.comprobarPlazo("2026-12-01", "2026-12-15"));
	}

	/** Sin fechas válidas no se puede comprobar el plazo */
	@Test
	public void testPlazoFechasNoValidas() {
		assertThrows(ValidationException.class, () -> model.comprobarPlazo(null, "2026-11-30"));
		assertThrows(ValidationException.class, () -> model.comprobarPlazo("2026-12-01", "30/11/2026"));
	}

	// //////////////////////////////////////////////
	// Contrato del alumno (usa la tabla ContratoAlumno)
	// //////////////////////////////////////////////

	/**
	 * Datos de ContratoAlumnoModelTest: AL0001 tiene contratos del 2025-10-01 al 2026-06-30 y del 2026-09-01 al
	 * 2027-07-15 (este con dos contratos solapados); AL0002 no tiene ninguno
	 */
	@Nested
	@Disabled("Pendiente de la tabla ContratoAlumno en schema.sql (la añade otro miembro del equipo)")
	class ContratoDelAlumno {
		@BeforeEach
		public void cargarContratos() {
			db.createDatabase(false);
			ContratoAlumnoModelTest.loadCleanDatabase(db);
		}

		/** El alumno con un contrato vigente en la fecha de inicio de la FIP puede matricularse (también el primer y el último día) */
		@Test
		public void testAlumnoConContratoVigente() {
			assertDoesNotThrow(() -> model.comprobarContratoAlumno("AL0001", "2026-12-01"));
			assertDoesNotThrow(() -> model.comprobarContratoAlumno("AL0001", "2026-09-01"));
			assertDoesNotThrow(() -> model.comprobarContratoAlumno("AL0001", "2027-07-15"));
		}

		/** Si ningún contrato cubre la fecha de inicio de la FIP, se bloquea la matrícula */
		@Test
		public void testAlumnoSinContratoVigente() {
			// entre el fin de un contrato y el inicio del siguiente
			ValidationException e = assertThrows(ValidationException.class,
					() -> model.comprobarContratoAlumno("AL0001", "2026-08-01"));
			assertTrue(e.getMessage().contains("AL0001"), e.getMessage());
			// después del último contrato
			assertThrows(ValidationException.class, () -> model.comprobarContratoAlumno("AL0001", "2027-07-16"));
			// alumno sin contratos
			assertThrows(ValidationException.class, () -> model.comprobarContratoAlumno("AL0002", "2026-12-01"));
		}
	}
}
