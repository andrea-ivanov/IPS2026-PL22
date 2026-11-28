package model.fip;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import model.profesor.ProfesorDTO;
import util.Database;
import util.exception.ValidationException;

/**
 * Pruebas de la HU 37166: el administrador da de alta un formador externo al configurar una FIP
 * (FormadorExternoModel). Un caso por clase de equivalencia.
 * Cada prueba parte de sus propios datos (loadCleanDatabase), independientes de data.sql
 */
public class FormadorExternoModelTest {
	private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private FormadorExternoModel model;

	@BeforeEach
	public void setUp() {
		db.createDatabase(false);
		loadCleanDatabase(db);
		model = new FormadorExternoModel(db);
	}

	/**
	 * Datos de prueba:
	 * - P001, profesor de plantilla con el correo ana@uni.es
	 * - FE001, formador externo con el correo elena@externa.es, así que el siguiente es FE002
	 */
	public static void loadCleanDatabase(Database db) {
		db.executeBatch(new String[] {
				"DELETE FROM InscripcionFip", "DELETE FROM Fip", "DELETE FROM FormadorExterno", "DELETE FROM Profesor",
				"INSERT INTO Profesor VALUES ('P001','10000001A','Ana','Garcia','ana@uni.es')",
				"INSERT INTO FormadorExterno VALUES ('FE001','Elena','Vega','elena@externa.es')"
		});
	}

	private void assertError(String mensaje, Runnable accion) {
		ValidationException e = assertThrows(ValidationException.class, accion::run);
		assertTrue(e.getMessage().contains(mensaje), "Mensaje inesperado: " + e.getMessage());
	}

	private void assertNoGuarda() {
		assertEquals(1, db.executeQueryArray("SELECT 1 FROM FormadorExterno").size());
	}

	// //////////////////////////////////////////////
	// Clases válidas
	// //////////////////////////////////////////////

	/** TE-01: los tres campos con datos y un correo con formato válido y no registrado */
	@Test
	public void testCrearFormadorExternoValido() {
		assertEquals("FE002", model.crearFormadorExterno(" Marcos ", " Herrero Blanco ", " marcos@cyber.es "));

		List<Object[]> filas = db.executeQueryArray(
				"SELECT nombre, apellidos, email FROM FormadorExterno WHERE formadorExternoId = 'FE002'");
		assertEquals(1, filas.size());
		assertAll(
				() -> assertEquals("Marcos", filas.get(0)[0]),
				() -> assertEquals("Herrero Blanco", filas.get(0)[1]),
				() -> assertEquals("marcos@cyber.es", filas.get(0)[2]));
	}

	/** TE-02: los identificadores siguen la numeración */
	@Test
	public void testNumeracion() {
		assertEquals("FE002", model.crearFormadorExterno("Marcos", "Herrero Blanco", "marcos@cyber.es"));
		assertEquals("FE003", model.crearFormadorExterno("Sofia", "Navarro Gil", "sofia@docencia.es"));
	}

	/** TE-10: los combos muestran primero los profesores de plantilla y después los externos, marcados */
	@Test
	public void testDocentesFip() {
		List<ProfesorDTO> docentes = model.getDocentesFip();
		assertEquals(2, docentes.size());
		assertAll(
				() -> assertEquals("P001", docentes.get(0).getProfesorId()),
				() -> assertFalse(docentes.get(0).isEsExterno()),
				() -> assertEquals("P001 - Ana Garcia", docentes.get(0).toString()),
				() -> assertEquals("FE001", docentes.get(1).getProfesorId()),
				() -> assertTrue(docentes.get(1).isEsExterno()),
				() -> assertEquals("FE001 - Elena Vega (externo)", docentes.get(1).toString()));
	}

	// //////////////////////////////////////////////
	// Clases no válidas
	// //////////////////////////////////////////////

	/** TE-03: nombre vacío, solo espacios o nulo */
	@Test
	public void testNombreVacio() {
		assertError("El nombre es obligatorio", () -> model.crearFormadorExterno("", "Herrero", "marcos@cyber.es"));
		assertError("El nombre es obligatorio", () -> model.crearFormadorExterno("  ", "Herrero", "marcos@cyber.es"));
		assertError("El nombre es obligatorio", () -> model.crearFormadorExterno(null, "Herrero", "marcos@cyber.es"));
		assertNoGuarda();
	}

	/** TE-04: apellidos vacíos, solo espacios o nulos */
	@Test
	public void testApellidosVacios() {
		assertError("Los apellidos son obligatorios", () -> model.crearFormadorExterno("Marcos", "", "marcos@cyber.es"));
		assertError("Los apellidos son obligatorios", () -> model.crearFormadorExterno("Marcos", "  ", "marcos@cyber.es"));
		assertError("Los apellidos son obligatorios", () -> model.crearFormadorExterno("Marcos", null, "marcos@cyber.es"));
		assertNoGuarda();
	}

	/** TE-05: correo vacío, solo espacios o nulo */
	@Test
	public void testEmailVacio() {
		assertError("El correo electrónico es obligatorio", () -> model.crearFormadorExterno("Marcos", "Herrero", ""));
		assertError("El correo electrónico es obligatorio", () -> model.crearFormadorExterno("Marcos", "Herrero", "  "));
		assertError("El correo electrónico es obligatorio", () -> model.crearFormadorExterno("Marcos", "Herrero", null));
		assertNoGuarda();
	}

	/** TE-06: correo sin @, sin usuario, sin dominio, sin punto en el dominio o con espacios */
	@Test
	public void testEmailFormatoNoValido() {
		for (String email : new String[] { "marcos", "marcos@", "@cyber.es", "marcos@cyber", "mar cos@cyber.es" }) {
			assertError("El correo electrónico no tiene un formato válido",
					() -> model.crearFormadorExterno("Marcos", "Herrero", email));
		}
		assertNoGuarda();
	}

	/** TE-07: correo de otro formador externo */
	@Test
	public void testEmailDeOtroExterno() {
		assertError("Ya hay un usuario registrado con el correo elena@externa.es",
				() -> model.crearFormadorExterno("Marcos", "Herrero", "elena@externa.es"));
		assertNoGuarda();
	}

	/** TE-08: correo de un profesor de plantilla */
	@Test
	public void testEmailDeUnProfesor() {
		assertError("Ya hay un usuario registrado con el correo ana@uni.es",
				() -> model.crearFormadorExterno("Marcos", "Herrero", "ana@uni.es"));
		assertNoGuarda();
	}

	/** TE-09: el correo repetido se detecta aunque cambien las mayúsculas o lleve espacios en los extremos */
	@Test
	public void testEmailRepetidoConMayusculas() {
		assertError("Ya hay un usuario registrado con el correo ANA@UNI.ES",
				() -> model.crearFormadorExterno("Marcos", "Herrero", " ANA@UNI.ES "));
		assertError("Ya hay un usuario registrado con el correo Elena@Externa.es",
				() -> model.crearFormadorExterno("Marcos", "Herrero", "Elena@Externa.es"));
		assertNoGuarda();
	}
}
