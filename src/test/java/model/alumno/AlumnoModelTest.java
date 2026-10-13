package model.alumno;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import model.fixtures.BaseModelTest;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Pruebas unitarias para el caso de uso de búsqueda de Alumnos.
 * Ubicada en el paquete model.alumno.
 */
public class AlumnoModelTest extends BaseModelTest {
	private AlumnoModel alumnoModel;

	@BeforeEach
	public void initModel() {
		this.alumnoModel = new AlumnoModel(db);
	}

	// =========================================================================
	// 1. ESCENARIOS DE ÉXITO (HAPPY PATH)
	// =========================================================================
	@Nested
	@DisplayName("1. Escenarios de Éxito (Happy Path)")
	public class HappyPath {

		/**
		 * Búsqueda por ID existente:
		 * Devuelve el objeto Alumno con todos sus campos poblados.
		 */
		@Test
		@DisplayName("Búsqueda por ID existente: devuelve el Alumno con todos sus campos poblados")
		public void testBuscarPorIdExistente_ObjetoCorrectoConCamposPoblados() {
			Alumno alumno = alumnoModel.buscarPorId("1");

			assertNotNull(alumno, "El alumno devuelto no debe ser nulo");
			assertAll("Verificación de campos del Alumno",
					() -> assertEquals(1L, alumno.getId(), "El ID debe ser 1"),
					() -> assertEquals("Juan Perez", alumno.getNombre(), "El nombre no coincide"),
					() -> assertEquals("juan@uni.es", alumno.getEmail(), "El email no coincide")
			);
		}

		/**
		 * Búsqueda por ID numérico Long.
		 */
		@Test
		@DisplayName("Búsqueda por ID numérico Long: funciona correctamente")
		public void testBuscarPorIdLong_ObjetoCorrecto() {
			Alumno alumno = alumnoModel.buscarPorId(2L);

			assertNotNull(alumno);
			assertEquals(2L, alumno.getId());
			assertEquals("Maria Gomez", alumno.getNombre());
			assertEquals("maria@uni.es", alumno.getEmail());
		}

		/**
		 * Mapeo correcto de datos.
		 */
		@Test
		@DisplayName("Mapeo correcto de datos del Alumno")
		public void testMapeoCorrectoDatos() {
			Alumno alumno = alumnoModel.buscarPorId("1");

			assertNotNull(alumno.getEmail());
			assertTrue(alumno.getEmail().contains("@"));
		}
	}

	// =========================================================================
	// 2. ESCENARIOS DE ERROR Y CASOS BORDE (EDGE CASES)
	// =========================================================================
	@Nested
	@DisplayName("2. Escenarios de Error y Casos Borde (Edge Cases)")
	public class EdgeCases {

		/**
		 * ID inexistente.
		 */
		@Test
		@DisplayName("ID inexistente: lanza ResourceNotFoundException")
		public void testBuscarPorIdInexistente_LanzaResourceNotFoundException() {
			assertThrows(ResourceNotFoundException.class, () -> {
				alumnoModel.buscarPorId("9999");
			}, "Debe lanzar ResourceNotFoundException para ID String inexistente");

			assertThrows(ResourceNotFoundException.class, () -> {
				alumnoModel.buscarPorId(9999L);
			}, "Debe lanzar ResourceNotFoundException para ID Long inexistente");
		}

		/**
		 * ID con formato inválido.
		 */
		@ParameterizedTest(name = "Formato inválido: {0}")
		@ValueSource(strings = { "abc", "alumno-1", "9.9", "-1", "!@#" })
		@DisplayName("ID con formato inválido: lanza ValidationException")
		public void testBuscarPorIdFormatoInvalido_LanzaValidationException(String idInvalido) {
			assertThrows(ValidationException.class, () -> {
				alumnoModel.buscarPorId(idInvalido);
			}, "Debe lanzar ValidationException al recibir ID con formato: " + idInvalido);
		}

		/**
		 * ID nulo o vacío.
		 */
		@Test
		@DisplayName("ID nulo: lanza ValidationException")
		public void testBuscarPorIdNulo_LanzaValidationException() {
			assertThrows(ValidationException.class, () -> {
				alumnoModel.buscarPorId((String) null);
			}, "Debe lanzar ValidationException cuando el ID String es null");

			assertThrows(ValidationException.class, () -> {
				alumnoModel.buscarPorId((Long) null);
			}, "Debe lanzar ValidationException cuando el ID Long es null");
		}

		@ParameterizedTest(name = "Cadena vacía o en blanco: ''{0}''")
		@ValueSource(strings = { "", "   ", "\t", "\n" })
		@DisplayName("ID vacío o en blanco: lanza ValidationException")
		public void testBuscarPorIdVacio_LanzaValidationException(String idVacio) {
			assertThrows(ValidationException.class, () -> {
				alumnoModel.buscarPorId(idVacio);
			}, "Debe lanzar ValidationException cuando el ID está vacío");
		}
	}
}

