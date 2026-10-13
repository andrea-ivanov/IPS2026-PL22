package model.anoacademico;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import model.fixtures.BaseModelTest;
import util.Util;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Pruebas unitarias para el modelo de negocio y caso de uso de Año Académico.
 */
public class AnoAcademicoModelTest extends BaseModelTest {
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
		@DisplayName("Buscar año académico actual: devuelve el objeto actual con todos sus campos poblados")
		public void testBuscarActual_DevuelveAnoAcademicoActual() {
			AnoAcademico actual = anoAcademicoModel.buscarActual();

			assertNotNull(actual, "El año académico actual no debe ser nulo");
			assertEquals(1L, actual.getId());
			assertEquals("2026/2027", actual.getNombre());
			assertTrue(actual.isActual(), "Debe estar marcado como actual");
			assertNotNull(actual.getInicioAnoAcademico(), "La fecha de inicio de año no debe ser nula");
			assertNotNull(actual.getFinAnoAcademico(), "La fecha de fin de año no debe ser nula");
			assertNotNull(actual.getInicioPeriodoMatriculacion(), "La fecha de inicio de matriculación no debe ser nula");
			assertNotNull(actual.getFinPeriodoMatriculacion(), "La fecha de fin de matriculación no debe ser nula");
		}

		@Test
		@DisplayName("Buscar por ID existente: devuelve el objeto correcto")
		public void testBuscarPorIdExistente_ObjetoCorrecto() {
			AnoAcademico ano = anoAcademicoModel.buscarPorId(1L);

			assertNotNull(ano);
			assertEquals(1L, ano.getId());
			assertEquals("2026/2027", ano.getNombre());
		}

		@Test
		@DisplayName("Actualizar fechas de año académico: persiste los cambios en base de datos")
		public void testActualizarFechasAnoAcademico_PersisteEnBD() {
			Date nuevoInicio = Util.isoStringToDate("2026-10-01");
			Date nuevoFin = Util.isoStringToDate("2027-07-15");

			anoAcademicoModel.actualizarFechasAnoAcademico(1L, nuevoInicio, nuevoFin);

			AnoAcademico actualizado = anoAcademicoModel.buscarPorId(1L);
			assertEquals(Util.dateToIsoString(nuevoInicio), Util.dateToIsoString(actualizado.getInicioAnoAcademico()));
			assertEquals(Util.dateToIsoString(nuevoFin), Util.dateToIsoString(actualizado.getFinAnoAcademico()));
		}

		@Test
		@DisplayName("Actualizar fechas de matriculación: persiste los cambios en base de datos")
		public void testActualizarFechasMatriculacion_PersisteEnBD() {
			Date nuevoInicio = Util.isoStringToDate("2026-07-10");
			Date nuevoFin = Util.isoStringToDate("2026-09-15");

			anoAcademicoModel.actualizarFechasMatriculacion(1L, nuevoInicio, nuevoFin);

			AnoAcademico actualizado = anoAcademicoModel.buscarPorId(1L);
			assertEquals(Util.dateToIsoString(nuevoInicio), Util.dateToIsoString(actualizado.getInicioPeriodoMatriculacion()));
			assertEquals(Util.dateToIsoString(nuevoFin), Util.dateToIsoString(actualizado.getFinPeriodoMatriculacion()));
		}

		@Test
		@DisplayName("Establecer como actual: desmarca el anterior y marca el nuevo")
		public void testEstablecerActual_CambiaAnoActual() {
			// Insertar un segundo año académico
			db.executeUpdate("INSERT INTO AnoAcademico (id_ano_academico, nombre, inicio_ano_academico, fin_ano_academico, " +
					"inicio_periodo_matriculacion, fin_periodo_matriculacion, actual) " +
					"VALUES (2, '2027/2028', '2027-09-01', '2028-06-30', '2027-07-01', '2027-08-31', 0)");

			anoAcademicoModel.establecerActual(2L);

			AnoAcademico actual = anoAcademicoModel.buscarActual();
			assertEquals(2L, actual.getId());
			assertTrue(actual.isActual());

			AnoAcademico anterior = anoAcademicoModel.buscarPorId(1L);
			assertFalse(anterior.isActual());
		}
	}

	// =========================================================================
	// 2. ESCENARIOS DE ERROR Y CASOS BORDE (EDGE CASES)
	// =========================================================================
	@Nested
	@DisplayName("2. Escenarios de Error y Casos Borde (Edge Cases)")
	public class EdgeCases {

		@Test
		@DisplayName("ID inexistente: lanza ResourceNotFoundException")
		public void testBuscarPorIdInexistente_LanzaResourceNotFoundException() {
			assertThrows(ResourceNotFoundException.class, () -> {
				anoAcademicoModel.buscarPorId(9999L);
			});
		}

		@Test
		@DisplayName("ID nulo: lanza ValidationException")
		public void testBuscarPorIdNulo_LanzaValidationException() {
			assertThrows(ValidationException.class, () -> {
				anoAcademicoModel.buscarPorId((Long) null);
			});
		}

		@ParameterizedTest
		@ValueSource(strings = {"", "   ", "\t", "\n"})
		@DisplayName("ID vacío o en blanco: lanza ValidationException")
		public void testBuscarPorIdVacio_LanzaValidationException(String idVacio) {
			assertThrows(ValidationException.class, () -> {
				anoAcademicoModel.buscarPorId(idVacio);
			});
		}

		@ParameterizedTest
		@ValueSource(strings = {"abc", "ano-1", "1.5", "-1", "!@#"})
		@DisplayName("ID con formato inválido: lanza ValidationException")
		public void testBuscarPorIdFormatoInvalido_LanzaValidationException(String idInvalido) {
			assertThrows(ValidationException.class, () -> {
				anoAcademicoModel.buscarPorId(idInvalido);
			});
		}

		@Test
		@DisplayName("Fecha fin anterior a fecha inicio en año académico: lanza ValidationException")
		public void testFechasInconsistentesAnoAcademico_LanzaValidationException() {
			Date inicio = Util.isoStringToDate("2027-06-30");
			Date fin = Util.isoStringToDate("2026-09-01"); // fin antes que inicio

			assertThrows(ValidationException.class, () -> {
				anoAcademicoModel.actualizarFechasAnoAcademico(1L, inicio, fin);
			});
		}

		@Test
		@DisplayName("Fecha fin anterior a fecha inicio en matriculación: lanza ValidationException")
		public void testFechasInconsistentesMatriculacion_LanzaValidationException() {
			Date inicio = Util.isoStringToDate("2026-09-01");
			Date fin = Util.isoStringToDate("2026-07-01"); // fin antes que inicio

			assertThrows(ValidationException.class, () -> {
				anoAcademicoModel.actualizarFechasMatriculacion(1L, inicio, fin);
			});
		}
	}
}

