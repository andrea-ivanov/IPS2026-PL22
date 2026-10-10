package model.curso;

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
 * Pruebas unitarias para el caso de uso de búsqueda de Cursos / Asignaturas.
 * Ubicada en el paquete model.curso según la estructura modular.
 */
public class CursoModelTest extends BaseModelTest {
	private CursoModel cursoModel;

	@BeforeEach
	public void initModel() {
		this.cursoModel = new CursoModel(db);
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
		 * el método devuelva el objeto Curso correcto con todos sus campos poblados.
		 */
		@Test
		@DisplayName("Búsqueda por ID existente: devuelve el Curso con todos sus campos poblados")
		public void testBuscarPorIdExistente_ObjetoCorrectoConCamposPoblados() {
			Curso curso = cursoModel.buscarPorId("1");

			assertNotNull(curso, "El curso devuelto no debe ser nulo");
			assertAll("Verificación de campos del Curso existente",
					() -> assertEquals(1L, curso.getId(), "El ID del curso debe ser 1"),
					() -> assertEquals("Programación", curso.getNombre(), "El nombre del curso no coincide"),
					() -> assertEquals(1L, curso.getIdGrado(), "El ID del grado asociado debe ser 1"),
					() -> assertEquals(1L, curso.getIdCoordinador(), "El ID del coordinador debe ser 1")
			);
		}

		/**
		 * Búsqueda por ID numérico (Long):
		 * Asegura el correcto funcionamiento con tipo numérico.
		 */
		@Test
		@DisplayName("Búsqueda por ID numérico Long: funciona correctamente")
		public void testBuscarPorIdLong_ObjetoCorrecto() {
			Curso curso = cursoModel.buscarPorId(2L);

			assertNotNull(curso);
			assertEquals(2L, curso.getId());
			assertEquals("Bases de Datos", curso.getNombre());
			assertEquals(1L, curso.getIdGrado());
			assertEquals(2L, curso.getIdCoordinador());
		}

		/**
		 * Mapeo correcto de datos:
		 * Asegura que los tipos como booleanos y enteros se transformen correctamente.
		 */
		@Test
		@DisplayName("Mapeo correcto de datos: booleanos y enteros del Curso")
		public void testMapeoCorrectoDatos() {
			Curso curso = cursoModel.buscarPorId("1");

			assertAll("Verificación de tipos del Curso",
					() -> assertTrue(curso.isActivo(), "El campo booleano 'activo' debe ser true"),
					() -> assertEquals(Integer.valueOf(6), curso.getCreditos(), "Los créditos deben ser un Integer válido de 6")
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
		 * Comprueba que lance ResourceNotFoundException cuando no existe en la base de datos.
		 */
		@Test
		@DisplayName("ID inexistente: lanza ResourceNotFoundException")
		public void testBuscarPorIdInexistente_LanzaResourceNotFoundException() {
			assertThrows(ResourceNotFoundException.class, () -> {
				cursoModel.buscarPorId("7777");
			}, "Debe lanzar ResourceNotFoundException para ID String inexistente");

			assertThrows(ResourceNotFoundException.class, () -> {
				cursoModel.buscarPorId(7777L);
			}, "Debe lanzar ResourceNotFoundException para ID Long inexistente");
		}

		/**
		 * ID con formato inválido:
		 * Comprueba que lance ValidationException si el formato no es numérico entero.
		 */
		@ParameterizedTest(name = "Formato inválido: {0}")
		@ValueSource(strings = { "abc", "curso1", "1-a", "99.9", "-4", "!*#" })
		@DisplayName("ID con formato inválido: lanza ValidationException")
		public void testBuscarPorIdFormatoInvalido_LanzaValidationException(String idInvalido) {
			assertThrows(ValidationException.class, () -> {
				cursoModel.buscarPorId(idInvalido);
			}, "Debe lanzar ValidationException al recibir ID con formato: " + idInvalido);
		}

		/**
		 * ID nulo o vacío:
		 * Comprueba que el sistema maneje null o vacíos arrojando ValidationException controlada.
		 */
		@Test
		@DisplayName("ID nulo: lanza ValidationException")
		public void testBuscarPorIdNulo_LanzaValidationException() {
			assertThrows(ValidationException.class, () -> {
				cursoModel.buscarPorId((String) null);
			}, "Debe lanzar ValidationException cuando el ID String es null");

			assertThrows(ValidationException.class, () -> {
				cursoModel.buscarPorId((Long) null);
			}, "Debe lanzar ValidationException cuando el ID Long es null");
		}

		@ParameterizedTest(name = "Cadena vacía o en blanco: ''{0}''")
		@ValueSource(strings = { "", "   ", "\t", "\r\n" })
		@DisplayName("ID vacío o en blanco: lanza ValidationException")
		public void testBuscarPorIdVacio_LanzaValidationException(String idVacio) {
			assertThrows(ValidationException.class, () -> {
				cursoModel.buscarPorId(idVacio);
			}, "Debe lanzar ValidationException cuando el ID está vacío");
		}
	}
}

