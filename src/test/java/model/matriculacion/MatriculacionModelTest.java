package model.matriculacion;

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
 * Pruebas unitarias para el caso de uso de búsqueda de Matriculaciones.
 * Ubicada en el paquete model.matriculacion según la modularización del modelo.
 */
public class MatriculacionModelTest extends BaseModelTest {
	private MatriculacionModel matriculacionModel;

	@BeforeEach
	public void initModel() {
		this.matriculacionModel = new MatriculacionModel(db);
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
		 * el método devuelva la Matriculación correcta con todos sus campos poblados,
		 * incluyendo el objeto Alumno y la lista de cursos en los que está matriculado.
		 */
		@Test
		@DisplayName("Búsqueda por ID existente: objeto devuelto con alumno y cursos asociados")
		public void testBuscarPorIdExistente_ObjetoCorrectoConCamposPoblados() {
			Matriculacion matriculacion = matriculacionModel.buscarPorId("1");

			assertNotNull(matriculacion, "La matrícula devuelta no debe ser nula");
			assertAll("Verificación de campos principales de la matrícula",
					() -> assertEquals(1L, matriculacion.getId(), "El ID de la matrícula debe ser 1"),
					() -> assertEquals(1L, matriculacion.getIdAnoAcademico(), "El ID del año académico debe ser 1"),
					() -> assertNotNull(matriculacion.getAlumno(), "El objeto alumno no debe ser nulo"),
					() -> assertEquals(1L, matriculacion.getAlumno().getId(), "El ID del alumno debe ser 1"),
					() -> assertEquals("Juan Perez", matriculacion.getAlumno().getNombre(), "El nombre del alumno debe coincidir"),
					() -> assertEquals("juan@uni.es", matriculacion.getAlumno().getEmail(), "El email del alumno debe coincidir"),
					() -> assertNotNull(matriculacion.getCursos(), "La lista de cursos no debe ser nula"),
					() -> assertEquals(2, matriculacion.getCursos().size(), "Debe contener los 2 cursos del grado asociado"),
					() -> assertEquals("Programación", matriculacion.getCursos().get(0).getNombre()),
					() -> assertEquals("Bases de Datos", matriculacion.getCursos().get(1).getNombre())
			);
		}

		/**
		 * Búsqueda por ID numérico (Long):
		 * Asegura la sobrecarga tipada.
		 */
		@Test
		@DisplayName("Búsqueda por ID numérico Long: funciona correctamente")
		public void testBuscarPorIdLong_ObjetoCorrecto() {
			Matriculacion matriculacion = matriculacionModel.buscarPorId(1L);

			assertNotNull(matriculacion);
			assertEquals(1L, matriculacion.getId());
			assertNotNull(matriculacion.getAlumno());
		}

		/**
		 * Mapeo correcto de datos:
		 * Asegurar que los tipos de datos del modelo (como fechas, enums o booleanos)
		 * se transformen correctamente desde la base de datos al objeto de retorno.
		 */
		@Test
		@DisplayName("Mapeo correcto de datos: transformación de fechas, enums y booleanos")
		public void testMapeoCorrectoDatos_FechasEnumsYBooleanos() {
			// Matrícula 1: estado "Activa", fecha "2026-09-01"
			Matriculacion mat1 = matriculacionModel.buscarPorId("1");

			assertAll("Mapeo de datos para Matrícula Activa",
					// 1. Mapeo de fecha: debe ser un java.util.Date correspondiente a 2026-09-01
					() -> assertNotNull(mat1.getFecha(), "La fecha no debe ser nula"),
					() -> assertEquals(Util.isoStringToDate("2026-09-01"), mat1.getFecha(), "La fecha debe mapearse a Date con el día exacto"),
					() -> assertEquals("2026-09-01", Util.dateToIsoString(mat1.getFecha()), "El formato ISO debe ser 2026-09-01"),

					// 2. Mapeo de enum: debe transformarse de texto "Activa" a EstadoMatricula.ACTIVA
					() -> assertEquals(EstadoMatricula.ACTIVA, mat1.getEstado(), "El estado 'Activa' debe mapearse a EstadoMatricula.ACTIVA"),

					// 3. Mapeo de booleano: true para estado ACTIVA
					() -> assertTrue(mat1.isPagada(), "El campo booleano 'pagada' debe ser true para matrícula activa")
			);

			// Matrícula 2: estado "Cancelada", fecha "2026-09-02"
			Matriculacion mat2 = matriculacionModel.buscarPorId("2");

			assertAll("Mapeo de datos para Matrícula Cancelada",
					() -> assertEquals(Util.isoStringToDate("2026-09-02"), mat2.getFecha(), "La fecha debe coincidir con 2026-09-02"),
					() -> assertEquals(EstadoMatricula.CANCELADA, mat2.getEstado(), "El estado 'Cancelada' debe mapearse a EstadoMatricula.CANCELADA"),
					() -> assertFalse(mat2.isPagada(), "El campo booleano 'pagada' debe ser false para matrícula cancelada")
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
			ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> {
				matriculacionModel.buscarPorId("8888");
			}, "Debe lanzar ResourceNotFoundException cuando el ID no existe");

			assertTrue(ex.getMessage().contains("8888"), "El mensaje debe contener el ID no encontrado");

			assertThrows(ResourceNotFoundException.class, () -> {
				matriculacionModel.buscarPorId(8888L);
			}, "Debe lanzar ResourceNotFoundException para ID Long inexistente");
		}

		/**
		 * ID con formato inválido:
		 * Probar el comportamiento cuando el ID no cumple con el formato esperado.
		 * Debería lanzar ValidationException (Bad Request).
		 */
		@ParameterizedTest(name = "Formato inválido: {0}")
		@ValueSource(strings = { "abc", "matr-1", "matricula_123", "9.99", "-10", "!@#$" })
		@DisplayName("ID con formato inválido: lanza ValidationException")
		public void testBuscarPorIdFormatoInvalido_LanzaValidationException(String idInvalido) {
			assertThrows(ValidationException.class, () -> {
				matriculacionModel.buscarPorId(idInvalido);
			}, "Debe lanzar ValidationException al recibir ID con formato: " + idInvalido);
		}

		/**
		 * ID nulo o vacío:
		 * Verificar que el sistema maneje la entrada de null o string vacío sin romperse
		 * inesperadamente, arrojando ValidationException.
		 */
		@Test
		@DisplayName("ID nulo: lanza ValidationException")
		public void testBuscarPorIdNulo_LanzaValidationException() {
			assertThrows(ValidationException.class, () -> {
				matriculacionModel.buscarPorId((String) null);
			}, "Debe lanzar ValidationException cuando el ID String es null");

			assertThrows(ValidationException.class, () -> {
				matriculacionModel.buscarPorId((Long) null);
			}, "Debe lanzar ValidationException cuando el ID Long es null");
		}

		@ParameterizedTest(name = "Cadena vacía o en blanco: ''{0}''")
		@ValueSource(strings = { "", "   ", "\t\t", "\n" })
		@DisplayName("ID vacío o en blanco: lanza ValidationException")
		public void testBuscarPorIdVacio_LanzaValidationException(String idVacio) {
			assertThrows(ValidationException.class, () -> {
				matriculacionModel.buscarPorId(idVacio);
			}, "Debe lanzar ValidationException cuando el ID está vacío");
		}
	}
}

