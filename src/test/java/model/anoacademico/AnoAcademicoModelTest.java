package model.anoacademico;

import static org.junit.jupiter.api.Assertions.*;

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
		@DisplayName("Buscar año académico actual: devuelve el último configurado (mayor fecha de inicio) con todos sus campos")
		public void testBuscarActual_DevuelveUltimoConfigurado() {
			AnoAcademico actual = anoAcademicoModel.buscarActual();

			assertNotNull(actual, "El año académico actual no debe ser nulo");
			assertEquals("2", actual.getAnioAcademicoId());
			assertEquals("2", actual.getNombre());
			assertTrue(actual.isActual(), "Debe estar marcado como actual");
			assertEquals("2027-09-01", Util.dateToIsoString(actual.getInicioAnoAcademico()));
			assertEquals("2028-06-30", Util.dateToIsoString(actual.getFinAnoAcademico()));
			assertEquals("2027-07-01", Util.dateToIsoString(actual.getInicioPeriodoMatriculacion()));
			assertEquals("2027-08-31", Util.dateToIsoString(actual.getFinPeriodoMatriculacion()));
		}

		@Test
		@DisplayName("Buscar por ID existente: devuelve el objeto correcto")
		public void testBuscarPorIdExistente_ObjetoCorrecto() {
			AnoAcademico ano = anoAcademicoModel.buscarPorId("2");

			assertNotNull(ano);
			assertEquals("2", ano.getAnioAcademicoId());
			assertEquals("2027-09-01", Util.dateToIsoString(ano.getInicioAnoAcademico()));
			assertEquals("2028-06-30", Util.dateToIsoString(ano.getFinAnoAcademico()));
		}
	}

	// =========================================================================
	// 2. ESCENARIOS DE ERROR Y CASOS BORDE (EDGE CASES)
	// =========================================================================
	@Nested
	@DisplayName("2. Escenarios de Error y Casos Borde (Edge Cases)")
	public class EdgeCases {

		@Test
		@DisplayName("Sin años académicos: buscarActual lanza ResourceNotFoundException")
		public void testBuscarActualSinAnos_LanzaResourceNotFoundException() {
			db.executeBatch(new String[] {
					"DELETE FROM MatriculaCurso",
					"DELETE FROM Matricula",
					"DELETE FROM AnioAcademico"
			});

			assertThrows(ResourceNotFoundException.class, () -> {
				anoAcademicoModel.buscarActual();
			});
		}

		@Test
		@DisplayName("ID inexistente: lanza ResourceNotFoundException")
		public void testBuscarPorIdInexistente_LanzaResourceNotFoundException() {
			assertThrows(ResourceNotFoundException.class, () -> {
				anoAcademicoModel.buscarPorId("9999");
			});
		}

		@Test
		@DisplayName("ID nulo: lanza ValidationException")
		public void testBuscarPorIdNulo_LanzaValidationException() {
			assertThrows(ValidationException.class, () -> {
				anoAcademicoModel.buscarPorId(null);
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
	}
}
