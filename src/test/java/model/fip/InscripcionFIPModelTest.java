package model.fip;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import model.contrato.ContratoAlumnoModelTest;
import util.Database;
import util.exception.ValidationException;

/**
 * Pruebas unitarias de las reglas de matrícula presencial en FIP (HU 37046).
 * Valida todas las reglas de negocio sin requerir la base de datos de FIP:
 * - Plazo de matrícula
 * - Formadores externos no pueden matricularse (solo imparten)
 * - El profesor responsable no puede matricularse en su propia FIP
 * - Control de plazas máximas
 * - Prohibición de matrículas duplicadas
 * - Gratuidad de la matrícula (precio 0.0)
 * - Resultado inicial PENDIENTE
 */
public class InscripcionFIPModelTest {
	private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private InscripcionFIPModel model;

	@BeforeEach
	public void setUp() {
		model = new InscripcionFIPModel(db);
	}

	// //////////////////////////////////////////////
	// 1. Plazo de matrícula
	// //////////////////////////////////////////////

	@Test
	@DisplayName("Se puede matricular hasta el día anterior al inicio de la FIP")
	public void testDentroDelPlazo() {
		assertDoesNotThrow(() -> model.comprobarPlazo("2026-12-01", "2026-11-30"));
		assertDoesNotThrow(() -> model.comprobarPlazo("2026-12-01", "2026-10-15"));
	}

	@Test
	@DisplayName("Desde el día de inicio ya no se admiten altas de matrícula")
	public void testFueraDelPlazo() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.comprobarPlazo("2026-12-01", "2026-12-01"));
		assertTrue(e.getMessage().contains("2026-11-30"), e.getMessage());
		assertThrows(ValidationException.class, () -> model.comprobarPlazo("2026-12-01", "2026-12-15"));
	}

	@Test
	@DisplayName("Sin fechas válidas no se puede comprobar el plazo de matrícula")
	public void testPlazoFechasNoValidas() {
		assertThrows(ValidationException.class, () -> model.comprobarPlazo(null, "2026-11-30"));
		assertThrows(ValidationException.class, () -> model.comprobarPlazo("2026-12-01", null));
		assertThrows(ValidationException.class, () -> model.comprobarPlazo("2026-12-01", "30/11/2026"));
	}

	// //////////////////////////////////////////////
	// 2. Formadores externos (no pueden matricularse)
	// //////////////////////////////////////////////

	@Test
	@DisplayName("Los formadores externos no pueden matricularse en FIP, solo impartir")
	public void testFormadorExternoNoPuedeMatricularse() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.comprobarNoEsFormadorExterno(TipoUsuarioFIP.FORMADOR_EXTERNO));
		assertTrue(e.getMessage().contains("solo imparten FIP"), e.getMessage());
		assertFalse(TipoUsuarioFIP.FORMADOR_EXTERNO.esMatriculable());
	}

	@Test
	@DisplayName("Alumnos y profesores de plantilla sí son tipos de usuario matriculables")
	public void testTiposUsuarioMatriculables() {
		assertDoesNotThrow(() -> model.comprobarNoEsFormadorExterno(TipoUsuarioFIP.ALUMNO));
		assertDoesNotThrow(() -> model.comprobarNoEsFormadorExterno(TipoUsuarioFIP.PROFESOR));
		assertTrue(TipoUsuarioFIP.ALUMNO.esMatriculable());
		assertTrue(TipoUsuarioFIP.PROFESOR.esMatriculable());
	}

	@Test
	@DisplayName("Tipo de usuario obligatorio")
	public void testTipoUsuarioObligatorio() {
		assertThrows(ValidationException.class, () -> model.comprobarNoEsFormadorExterno(null));
	}

	// //////////////////////////////////////////////
	// 3. Profesor responsable no puede matricularse en su FIP
	// //////////////////////////////////////////////

	@Test
	@DisplayName("El profesor responsable de la FIP no puede ser asistente en ella")
	public void testProfesorResponsableNoPuedeMatricularse() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.comprobarProfesorNoEsResponsable("PR0001", "PR0001"));
		assertTrue(e.getMessage().contains("profesor responsable"), e.getMessage());
	}

	@Test
	@DisplayName("Un profesor que no es responsable de la FIP sí puede matricularse")
	public void testProfesorNoResponsablePuedeMatricularse() {
		assertDoesNotThrow(() -> model.comprobarProfesorNoEsResponsable("PR0002", "PR0001"));
		assertDoesNotThrow(() -> model.comprobarProfesorNoEsResponsable("PR0002", null));
	}

	// //////////////////////////////////////////////
	// 4. Plazas de la FIP
	// //////////////////////////////////////////////

	@Test
	@DisplayName("Se permite matricular si quedan plazas disponibles")
	public void testPlazasDisponibles() {
		assertDoesNotThrow(() -> model.comprobarPlazasDisponibles(5, 10));
		assertDoesNotThrow(() -> model.comprobarPlazasDisponibles(9, 10));
		assertDoesNotThrow(() -> model.comprobarPlazasDisponibles(0, 5));
	}

	@Test
	@DisplayName("Se bloquea la matrícula al completarse el número máximo de plazas")
	public void testPlazasLlenasBloqueaMatricula() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.comprobarPlazasDisponibles(10, 10));
		assertTrue(e.getMessage().contains("No quedan plazas disponibles"), e.getMessage());

		assertThrows(ValidationException.class, () -> model.comprobarPlazasDisponibles(11, 10));
	}

	// //////////////////////////////////////////////
	// 5. Matrículas duplicadas
	// //////////////////////////////////////////////

	@Test
	@DisplayName("No se puede matricular dos veces al mismo usuario en la misma FIP")
	public void testNoDuplicarMatricula() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.comprobarNoDuplicada("AL0001", Arrays.asList("AL0002", "AL0001", "PR0001")));
		assertTrue(e.getMessage().contains("No se puede matricular dos veces"), e.getMessage());
		assertTrue(e.getMessage().contains("AL0001"), e.getMessage());
	}

	@Test
	@DisplayName("Se permite matricular si el usuario aún no está inscrito")
	public void testMatriculaNuevaNoDuplicada() {
		assertDoesNotThrow(() -> model.comprobarNoDuplicada("AL0003", Arrays.asList("AL0001", "AL0002")));
		assertDoesNotThrow(() -> model.comprobarNoDuplicada("AL0001", Collections.emptyList()));
		assertDoesNotThrow(() -> model.comprobarNoDuplicada("AL0001", null));
	}

	// //////////////////////////////////////////////
	// 6. Gratuidad y resultado inicial PENDIENTE
	// //////////////////////////////////////////////

	@Test
	@DisplayName("La matrícula en una FIP es siempre gratuita (precio 0.0)")
	public void testMatriculaGratuita() {
		assertEquals(0.0, InscripcionFIPModel.PRECIO_MATRICULA);
		InscripcionFIPDTO dto = model.crearInscripcion("INS0001", "FIP001", "AL0001", TipoUsuarioFIP.ALUMNO, "2026-11-20");
		assertEquals(0.0, dto.getPrecio());
	}

	@Test
	@DisplayName("Al matricular, la inscripción queda con resultado PENDIENTE")
	public void testResultadoInicialPendiente() {
		InscripcionFIPDTO dto = model.crearInscripcion("INS0001", "FIP001", "PR0002", TipoUsuarioFIP.PROFESOR, "2026-11-20");
		assertEquals(ResultadoInscripcionFIP.PENDIENTE, dto.getResultado());
		assertEquals("PR0002", dto.getUsuarioId());
		assertEquals("FIP001", dto.getFipId());
	}

	// //////////////////////////////////////////////
	// 7. Validación completa orquestada
	// //////////////////////////////////////////////

	@Test
	@DisplayName("Validación completa para profesor de plantilla sin conflictos")
	public void testValidacionCompletaProfesorCorrecta() {
		assertDoesNotThrow(() -> model.validarMatricula("FIP001", "2026-12-01", "2026-11-20",
				"PR0002", TipoUsuarioFIP.PROFESOR, "PR0001", 5, 10, false));
	}

	@Test
	@DisplayName("Validación completa rechaza formadores externos")
	public void testValidacionCompletaRechazaFormadorExterno() {
		assertThrows(ValidationException.class, () -> model.validarMatricula("FIP001", "2026-12-01", "2026-11-20",
				"EXT001", TipoUsuarioFIP.FORMADOR_EXTERNO, "PR0001", 5, 10, false));
	}

	@Test
	@DisplayName("Validación completa rechaza profesor responsable")
	public void testValidacionCompletaRechazaProfesorResponsable() {
		assertThrows(ValidationException.class, () -> model.validarMatricula("FIP001", "2026-12-01", "2026-11-20",
				"PR0001", TipoUsuarioFIP.PROFESOR, "PR0001", 5, 10, false));
	}

	@Test
	@DisplayName("Validación completa rechaza si plazas completas o duplicado")
	public void testValidacionCompletaRechazaPlazasLlenasODuplicado() {
		assertThrows(ValidationException.class, () -> model.validarMatricula("FIP001", "2026-12-01", "2026-11-20",
				"PR0002", TipoUsuarioFIP.PROFESOR, "PR0001", 10, 10, false));
		assertThrows(ValidationException.class, () -> model.validarMatricula("FIP001", "2026-12-01", "2026-11-20",
				"PR0002", TipoUsuarioFIP.PROFESOR, "PR0001", 5, 10, true));
	}

	// //////////////////////////////////////////////
	// 8. DTO de FIP
	// //////////////////////////////////////////////

	@Test
	@DisplayName("FIPDTO calcula plazas disponibles correctamente")
	public void testFIPDTOCalculoPlazas() {
		FIPDTO fip = new FIPDTO("FIP001", "Taller de IA", "2026-12-01", "2026-12-10", "PR0001", 20, 15);
		assertEquals("FIP001", fip.getFipId());
		assertEquals("Taller de IA", fip.getNombre());
		assertEquals(5, fip.getPlazasDisponibles());
		assertTrue(fip.hayPlazasDisponibles());

		fip.setPlazasOcupadas(20);
		assertEquals(0, fip.getPlazasDisponibles());
		assertFalse(fip.hayPlazasDisponibles());
	}

	// //////////////////////////////////////////////
	// Contrato del alumno (usa la tabla ContratoAlumno)
	// //////////////////////////////////////////////

	/**
	 * Datos de ContratoAlumnoModelTest: AL0001 tiene contratos del 2025-10-01 al 2026-06-30 y del 2026-09-01 al
	 * 2027-07-15 (este con dos contratos solapados); AL0002 no tiene ninguno
	 */
	@Nested
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
