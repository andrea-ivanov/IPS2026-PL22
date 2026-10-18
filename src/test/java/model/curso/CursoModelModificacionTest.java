package model.curso;

import static org.junit.jupiter.api.Assertions.*;

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
 * Pruebas unitarias para la modificación de Cursos dentro de un Grado (HU 35223).
 * Editables: año e itinerario (de GradoCurso) y créditos (de Curso, compartidos entre grados).
 */
public class CursoModelModificacionTest extends BaseModelTest {
	private CursoModel cursoModel;

	@BeforeEach
	public void initModel() {
		this.cursoModel = new CursoModel(db);
	}

	@Test
	@DisplayName("TM-C01 Modificación total: año, itinerario y créditos quedan con los valores nuevos")
	public void testModificarCurso_Total() {
		cursoModel.modificarCurso("1", "1", "3", "Software", "4.5");

		Object[] gradoCurso = filaGradoCurso("1", "1");
		assertAll("Campos modificados",
				() -> assertEquals(3, ((Number) gradoCurso[0]).intValue()),
				() -> assertEquals("Software", gradoCurso[1]),
				() -> assertEquals(4.5, creditos("1")));
	}

	@Test
	@DisplayName("TM-C02 Modificación parcial: solo cambia el itinerario")
	public void testModificarCurso_Parcial() {
		cursoModel.modificarCurso("1", "1", "1", "Software", "6");

		Object[] gradoCurso = filaGradoCurso("1", "1");
		assertAll("Solo cambia el itinerario",
				() -> assertEquals(1, ((Number) gradoCurso[0]).intValue()),
				() -> assertEquals("Software", gradoCurso[1]),
				() -> assertEquals(6.0, creditos("1")));
	}

	@Test
	@DisplayName("TM-C03 Sin cambios: no hay error y las filas quedan idénticas")
	public void testModificarCurso_SinCambios() {
		assertDoesNotThrow(() -> cursoModel.modificarCurso("1", "1", "1", "General", "6"));

		assertSinCambios();
	}

	@ParameterizedTest(name = "campo {0} = ''{1}''")
	@CsvSource(value = { "anio,NULL", "anio,''", "anio,'   '", "itinerario,NULL", "itinerario,''",
			"itinerario,'   '", "creditos,NULL", "creditos,''", "creditos,'   '" }, nullValues = "NULL")
	@DisplayName("TM-C04 Campo obligatorio vacío: lanza ValidationException y no modifica")
	public void testModificarCurso_CampoVacio(String campo, String valor) {
		String anio = "anio".equals(campo) ? valor : "3";
		String itinerario = "itinerario".equals(campo) ? valor : "Software";
		String creditos = "creditos".equals(campo) ? valor : "4.5";

		assertThrows(ValidationException.class, () -> cursoModel.modificarCurso("1", "1", anio, itinerario, creditos));

		assertSinCambios();
	}

	@ParameterizedTest(name = "créditos = ''{0}''")
	@ValueSource(strings = { "abc", "0", "-3" })
	@DisplayName("TM-C05 Créditos no válidos: lanza ValidationException y no modifica")
	public void testModificarCurso_CreditosInvalidos(String creditos) {
		assertThrows(ValidationException.class, () -> cursoModel.modificarCurso("1", "1", "3", "Software", creditos));

		assertSinCambios();
	}

	@ParameterizedTest(name = "año = ''{0}''")
	@ValueSource(strings = { "abc", "0", "7" })
	@DisplayName("TM-C06 Año fuera de 1-6: lanza ValidationException y no modifica")
	public void testModificarCurso_AnioInvalido(String anio) {
		assertThrows(ValidationException.class, () -> cursoModel.modificarCurso("1", "1", anio, "Software", "4.5"));

		assertSinCambios();
	}

	@Test
	@DisplayName("TM-C07 Los campos no editables no cambian")
	public void testModificarCurso_NoEditablesIntactos() {
		cursoModel.modificarCurso("1", "1", "3", "Software", "4.5");

		Object[] curso = db.executeQueryArray("SELECT cursoId, nombre, coordinadorId FROM Curso WHERE cursoId = '1'")
				.get(0);
		Object[] otro = filaGradoCurso("1", "2");
		assertAll("No editables",
				() -> assertEquals("1", curso[0]),
				() -> assertEquals("Programación", curso[1]),
				() -> assertEquals("1", curso[2]),
				() -> assertEquals("ACTIVO", filaGradoCurso("1", "1")[2]),
				() -> assertEquals(1, ((Number) otro[0]).intValue()),
				() -> assertEquals("General", otro[1]),
				() -> assertEquals("ACTIVO", otro[2]),
				() -> assertEquals(6.0, creditos("2")));
	}

	@Test
	@DisplayName("TM-C08 Curso compartido: año e itinerario solo cambian en ese grado; los créditos en todos")
	public void testModificarCurso_CursoCompartido() {
		db.executeUpdate("INSERT INTO GradoCurso (gradoId, cursoId, anio, itinerario, estado) VALUES ('2', '1', 2, 'Otro', 'ACTIVO')");

		cursoModel.modificarCurso("1", "1", "3", "Software", "4.5");

		Object[] otroGrado = filaGradoCurso("2", "1");
		assertAll("Curso compartido",
				() -> assertEquals(2, ((Number) otroGrado[0]).intValue()),
				() -> assertEquals("Otro", otroGrado[1]),
				() -> assertEquals(4.5, creditos("1")));
	}

	@Test
	@DisplayName("TM-C09 Curso inexistente en ese grado: lanza ResourceNotFoundException")
	public void testModificarCurso_Inexistente() {
		assertThrows(ResourceNotFoundException.class,
				() -> cursoModel.modificarCurso("2", "1", "3", "Software", "4.5"));

		assertEquals(6.0, creditos("1"));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "UPDATE GradoCurso SET estado = 'INACTIVO' WHERE gradoId = '1' AND cursoId = '1'",
			"UPDATE Grado SET estado = 'EN_EXTINCION' WHERE gradoId = '1'" })
	@DisplayName("TM-C10 Sin restricción por estado: se modifica y el estado no cambia")
	public void testModificarCurso_CualquierEstado(String preparacion) {
		db.executeUpdate(preparacion);
		Object estadoGradoCurso = filaGradoCurso("1", "1")[2];
		Object estadoGrado = db.executeQueryArray("SELECT estado FROM Grado WHERE gradoId = '1'").get(0)[0];

		cursoModel.modificarCurso("1", "1", "3", "Software", "4.5");

		assertAll("Se modifica sin cambiar estados",
				() -> assertEquals("Software", filaGradoCurso("1", "1")[1]),
				() -> assertEquals(estadoGradoCurso, filaGradoCurso("1", "1")[2]),
				() -> assertEquals(estadoGrado,
						db.executeQueryArray("SELECT estado FROM Grado WHERE gradoId = '1'").get(0)[0]));
	}

	@Test
	@DisplayName("TM-C11 Precarga: devuelve los valores del curso en ese grado sin transformar")
	public void testBuscarDatosEdicion() {
		CursoEdicionDTO datos = cursoModel.buscarDatosEdicion("1", "1");

		assertAll("Datos de edición",
				() -> assertEquals("1", datos.getAnio()),
				() -> assertEquals("General", datos.getItinerario()),
				() -> assertEquals("6", datos.getCreditos()),
				() -> assertEquals("Programación", datos.getNombre()),
				() -> assertEquals("1", datos.getCoordinadorId()));
	}

	@Test
	@DisplayName("TM-C12 Número de grados del curso: cuenta todas sus asociaciones")
	public void testContarGradosDelCurso() {
		assertEquals(1, cursoModel.contarGradosDelCurso("1"));

		db.executeUpdate("INSERT INTO GradoCurso (gradoId, cursoId, anio, itinerario, estado) VALUES ('2', '1', 2, 'Otro', 'ACTIVO')");

		assertEquals(2, cursoModel.contarGradosDelCurso("1"));
	}

	private Object[] filaGradoCurso(String gradoId, String cursoId) {
		return db.executeQueryArray("SELECT anio, itinerario, estado FROM GradoCurso WHERE gradoId = ? AND cursoId = ?",
				gradoId, cursoId).get(0);
	}

	private double creditos(String cursoId) {
		return ((Number) db.executeQueryArray("SELECT creditos FROM Curso WHERE cursoId = ?", cursoId).get(0)[0])
				.doubleValue();
	}

	private void assertSinCambios() {
		Object[] gradoCurso = filaGradoCurso("1", "1");
		assertAll("Sin cambios",
				() -> assertEquals(1, ((Number) gradoCurso[0]).intValue()),
				() -> assertEquals("General", gradoCurso[1]),
				() -> assertEquals("ACTIVO", gradoCurso[2]),
				() -> assertEquals(6.0, creditos("1")));
	}
}
