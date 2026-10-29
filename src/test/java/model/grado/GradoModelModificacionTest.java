package model.grado;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import model.fixtures.BaseModelTest;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Pruebas unitarias para la modificación de Grados (HU 35223).
 * Editables: nombre, facultad y rama de conocimiento.
 */
public class GradoModelModificacionTest extends BaseModelTest {
	private static final String NOMBRE = "Ingeniería Informática";
	private static final String FACULTAD = "Escuela de Ingeniería Informática";
	private static final String RAMA = "Ingenieria y Arquitectura";

	private GradoModel gradoModel;

	@BeforeEach
	public void initModel() {
		this.gradoModel = new GradoModel(db);
	}

	@Test
	@DisplayName("TM-G01 Modificación total: los 3 campos quedan con los valores nuevos")
	public void testModificarGrado_Total() {
		gradoModel.modificarGrado("1", "Informática Nueva", "Facultad Nueva", "Ciencias");

		Object[] grado = filaGrado("1");
		assertAll("Campos modificados",
				() -> assertEquals("Informática Nueva", grado[1]),
				() -> assertEquals("Facultad Nueva", grado[2]),
				() -> assertEquals("Ciencias", grado[3]));
	}

	@Test
	@DisplayName("TM-G02 Modificación parcial: solo cambia el nombre")
	public void testModificarGrado_Parcial() {
		gradoModel.modificarGrado("1", "Informática Nueva", FACULTAD, RAMA);

		Object[] grado = filaGrado("1");
		assertAll("Solo cambia el nombre",
				() -> assertEquals("Informática Nueva", grado[1]),
				() -> assertEquals(FACULTAD, grado[2]),
				() -> assertEquals(RAMA, grado[3]));
	}

	@Test
	@DisplayName("TM-G03 Sin cambios: no hay error y la fila queda idéntica")
	public void testModificarGrado_SinCambios() {
		Object[] antes = filaGrado("1");

		assertDoesNotThrow(() -> gradoModel.modificarGrado("1", NOMBRE, FACULTAD, RAMA));

		assertArrayEquals(antes, filaGrado("1"));
	}

	@ParameterizedTest(name = "campo {0} = ''{1}''")
	@CsvSource(value = { "nombre,NULL", "nombre,''", "nombre,'   '", "facultad,NULL", "facultad,''",
			"facultad,'   '", "rama,NULL", "rama,''", "rama,'   '" }, nullValues = "NULL")
	@DisplayName("TM-G04 Campo obligatorio vacío: lanza ValidationException y no modifica")
	public void testModificarGrado_CampoVacio(String campo, String valor) {
		Object[] antes = filaGrado("1");
		String nombre = "nombre".equals(campo) ? valor : "Nuevo";
		String facultad = "facultad".equals(campo) ? valor : "Nueva";
		String rama = "rama".equals(campo) ? valor : "Nueva";

		assertThrows(ValidationException.class, () -> gradoModel.modificarGrado("1", nombre, facultad, rama));

		assertArrayEquals(antes, filaGrado("1"));
	}

	@Test
	@DisplayName("TM-G05 El nombre no es UNIQUE: se permite el nombre de otro grado")
	public void testModificarGrado_NombreDeOtroGrado() {
		gradoModel.modificarGrado("1", "Ingeniería de Telecomunicaciones", FACULTAD, RAMA);

		assertEquals("Ingeniería de Telecomunicaciones", filaGrado("1")[1]);
	}

	@Test
	@DisplayName("TM-G06 Los campos no editables no cambian")
	public void testModificarGrado_NoEditablesIntactos() {
		List<Object[]> cursosAntes = db.executeQueryArray(
				"SELECT cursoId, anio, itinerario, estado FROM GradoCurso WHERE gradoId = '1' ORDER BY cursoId");

		gradoModel.modificarGrado("1", "Informática Nueva", "Facultad Nueva", "Ciencias");

		Object[] grado = filaGrado("1");
		List<Object[]> cursosDespues = db.executeQueryArray(
				"SELECT cursoId, anio, itinerario, estado FROM GradoCurso WHERE gradoId = '1' ORDER BY cursoId");
		assertAll("No editables",
				() -> assertEquals("1", grado[0]),
				() -> assertEquals("VIGENTE", grado[4]),
				() -> assertEquals("2024-09-01", grado[5]),
				() -> assertEquals(cursosAntes.size(), cursosDespues.size()));
		for (int i = 0; i < cursosAntes.size(); i++) {
			assertArrayEquals(cursosAntes.get(i), cursosDespues.get(i));
		}
	}

	@Test
	@DisplayName("TM-G07 Grado inexistente: lanza ResourceNotFoundException")
	public void testModificarGrado_Inexistente() {
		assertThrows(ResourceNotFoundException.class,
				() -> gradoModel.modificarGrado("9999", "Informática Nueva", "Facultad Nueva", "Ciencias"));
	}

	@ParameterizedTest(name = "estado = {0}")
	@ValueSource(strings = { "PENDIENTE_RENOVACION", "EN_EXTINCION", "EXTINTO" })
	@DisplayName("TM-G08 Sin restricción por estado: se modifica y el estado no cambia")
	public void testModificarGrado_CualquierEstado(String estado) {
		db.executeUpdate("UPDATE Grado SET estado = ? WHERE gradoId = '1'", estado);

		gradoModel.modificarGrado("1", "Informática Nueva", "Facultad Nueva", "Ciencias");

		Object[] grado = filaGrado("1");
		assertEquals("Informática Nueva", grado[1]);
		assertEquals(estado, grado[4]);
	}

	private Object[] filaGrado(String gradoId) {
		return db.executeQueryArray(
				"SELECT gradoId, nombre, facultad, ramaConocimiento, estado, fechaInicioVigencia FROM Grado WHERE gradoId = ?",
				gradoId).get(0);
	}
}
