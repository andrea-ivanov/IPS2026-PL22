package model.grado;

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
 * Pruebas unitarias para el caso de uso de búsqueda de Grados.
 * Ubicada en el paquete model.grado según la estructura por dominios.
 */
public class GradoModelTest extends BaseModelTest {
	private GradoModel gradoModel;

	@BeforeEach
	public void initModel() {
		this.gradoModel = new GradoModel(db);
	}

	// =========================================================================
	// 1. ESCENARIOS DE ÉXITO (HAPPY PATH)
	// =========================================================================
	@Nested
	@DisplayName("1. Escenarios de Éxito (Happy Path)")
	public class HappyPath {

		/**
		 * Búsqueda por ID existente:
		 * Verificar que al pasar un ID válido y existente en la base de datos,
		 * el método devuelva el objeto correcto con todos sus campos poblados,
		 * incluyendo la lista de cursos asociados con su id y nombre.
		 */
		@Test
		@DisplayName("Búsqueda por ID existente: devuelve el Grado con todos sus campos y cursos")
		public void testBuscarPorIdExistente_ObjetoCorrectoConCamposPoblados() {
			Grado grado = gradoModel.buscarPorId("1");

			assertNotNull(grado, "El grado devuelto no debe ser nulo");
			assertAll("Verificación de campos del Grado existente",
					() -> assertEquals(1L, grado.getId(), "El ID del grado debe ser 1"),
					() -> assertEquals("Ingeniería Informática", grado.getNombre(), "El nombre del grado no coincide"),
					() -> assertNotNull(grado.getCursos(), "La lista de cursos no debe ser nula"),
					() -> assertEquals(2, grado.getCursos().size(), "El grado debe contener exactamente 2 cursos"),
					() -> assertEquals(1L, grado.getCursos().get(0).getId(), "El ID del primer curso debe ser 1"),
					() -> assertEquals("Programación", grado.getCursos().get(0).getNombre(), "El primer curso debe ser Programación"),
					() -> assertEquals(2L, grado.getCursos().get(1).getId(), "El ID del segundo curso debe ser 2"),
					() -> assertEquals("Bases de Datos", grado.getCursos().get(1).getNombre(), "El segundo curso debe ser Bases de Datos")
			);
		}

		/**
		 * Búsqueda por ID numérico (Long):
		 * Asegura la sobrecarga tipada del método.
		 */
		@Test
		@DisplayName("Búsqueda por ID numérico Long: funciona idénticamente a la versión String")
		public void testBuscarPorIdLong_ObjetoCorrecto() {
			Grado grado = gradoModel.buscarPorId(1L);

			assertNotNull(grado);
			assertEquals(1L, grado.getId());
			assertEquals("Ingeniería Informática", grado.getNombre());
			assertFalse(grado.getCursos().isEmpty());
		}

		/**
		 * Mapeo correcto de datos:
		 * Asegurar que los tipos de datos del modelo (como enums y booleanos)
		 * se transformen correctamente al objeto de retorno.
		 */
		@Test
		@DisplayName("Mapeo correcto de datos: enums y booleanos del Grado")
		public void testMapeoCorrectoDatos() {
			Grado grado = gradoModel.buscarPorId("1");

			assertAll("Verificación de tipos mapeados",
					() -> assertEquals(EstadoGrado.VIGENTE, grado.getEstado(), "El estado debe mapearse al enum EstadoGrado.VIGENTE"),
					() -> assertTrue(grado.isActivo(), "El campo booleano activo debe ser true"),
					() -> assertEquals(Long.valueOf(1L), grado.getCursos().get(0).getIdGrado(), "El idGrado del curso hijo debe coincidir")
			);
		}
	}

	// =========================================================================
	// 2. ESCENARIOS DE ERROR Y CASOS BORDE (EDGE CASES)
	// =========================================================================
	@Nested
	@DisplayName("2. Escenarios de Error y Casos Borde (Edge Cases)")
	public class EdgeCases {

		/**
		 * ID inexistente:
		 * Verificar qué ocurre cuando el ID no existe en la base de datos.
		 * Debe lanzar ResourceNotFoundException.
		 */
		@Test
		@DisplayName("ID inexistente: lanza ResourceNotFoundException")
		public void testBuscarPorIdInexistente_LanzaResourceNotFoundException() {
			ResourceNotFoundException exString = assertThrows(ResourceNotFoundException.class, () -> {
				gradoModel.buscarPorId("9999");
			}, "Debe lanzar ResourceNotFoundException al buscar ID String inexistente");

			assertTrue(exString.getMessage().contains("9999"), "El mensaje debe contener el ID no encontrado");

			ResourceNotFoundException exLong = assertThrows(ResourceNotFoundException.class, () -> {
				gradoModel.buscarPorId(9999L);
			}, "Debe lanzar ResourceNotFoundException al buscar ID Long inexistente");

			assertTrue(exLong.getMessage().contains("9999"), "El mensaje debe contener el ID no encontrado");
		}

		/**
		 * ID con formato inválido:
		 * Probar el comportamiento cuando el ID no cumple con el formato esperado
		 * (por ejemplo, enviar un texto 'abc' cuando el sistema espera un ID numérico).
		 * Debería lanzar ValidationException (Bad Request).
		 */
		@ParameterizedTest(name = "Formato inválido: {0}")
		@ValueSource(strings = { "abc", "12a", "xyz", "1.5", "-1", "@!#", "id_1" })
		@DisplayName("ID con formato inválido: lanza ValidationException")
		public void testBuscarPorIdFormatoInvalido_LanzaValidationException(String idInvalido) {
			assertThrows(ValidationException.class, () -> {
				gradoModel.buscarPorId(idInvalido);
			}, "Debe lanzar ValidationException para el ID con formato inválido: " + idInvalido);
		}

		/**
		 * ID nulo o vacío:
		 * Verificar que el sistema maneje correctamente valores como null o string vacío
		 * sin romperse inesperadamente, arrojando ValidationException controlada.
		 */
		@Test
		@DisplayName("ID nulo: lanza ValidationException")
		public void testBuscarPorIdNulo_LanzaValidationException() {
			assertThrows(ValidationException.class, () -> {
				gradoModel.buscarPorId((String) null);
			}, "Debe lanzar ValidationException cuando el ID String es null");

			assertThrows(ValidationException.class, () -> {
				gradoModel.buscarPorId((Long) null);
			}, "Debe lanzar ValidationException cuando el ID Long es null");
		}

		@ParameterizedTest(name = "Cadena vacía o en blanco: ''{0}''")
		@ValueSource(strings = { "", "   ", "\t", "\n" })
		@DisplayName("ID vacío o en blanco: lanza ValidationException")
		public void testBuscarPorIdVacio_LanzaValidationException(String idVacio) {
			assertThrows(ValidationException.class, () -> {
				gradoModel.buscarPorId(idVacio);
			}, "Debe lanzar ValidationException cuando el ID es una cadena vacía o en blanco");
		}
	}
}

