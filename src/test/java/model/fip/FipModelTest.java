package model.fip;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import util.Database;
import util.exception.ValidationException;

/**
 * Pruebas de la HU 37165: el administrador crea una FIP y le asigna un profesor (FipModel).
 * Un caso por clase de equivalencia y sus valores límite. Hoy es 2026-11-21, así que hoy + 7 = 2026-11-28.
 * Cada prueba parte de sus propios datos (loadCleanDatabase), independientes de data.sql
 */
public class FipModelTest {
	private static final String HOY = "2026-11-21";
	private static final String PROFESOR = "P001";
	private static final String INICIO = "2026-12-10";
	private static final String FIN = "2026-12-20";

	private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private FipModel model;

	@BeforeEach
	public void setUp() {
		db.createDatabase(false);
		loadCleanDatabase(db);
		model = new FipModel(db);
	}

	/**
	 * Datos de prueba:
	 * - Dos profesores, P001 y P002, y un formador externo, FE001 (HU 37166)
	 * - P001 ya tiene la FIP005, así que la siguiente es FIP006
	 */
	public static void loadCleanDatabase(Database db) {
		db.executeBatch(new String[] {
				"DELETE FROM InscripcionFip", "DELETE FROM Fip", "DELETE FROM FormadorExterno", "DELETE FROM Profesor",
				"INSERT INTO Profesor VALUES ('P001','10000001A','Ana','Garcia','p001@uni.es')",
				"INSERT INTO Profesor VALUES ('P002','10000002B','Luis','Fernandez','p002@uni.es')",
				"INSERT INTO FormadorExterno VALUES ('FE001','Elena','Vega Morales','elena.vega@formacion.es')",
				"INSERT INTO Fip (fipId, nombre, profesorId, fechaCreacion, fechaInicio, fechaFin) "
						+ "VALUES ('FIP005','Excel basico','P001','2026-10-01','2026-10-20','2026-10-25')"
		});
	}

	private String crear(String nombre, String profesorId, String fechaInicio, String fechaFin) {
		return model.crearFip(nombre, profesorId, fechaInicio, fechaFin, HOY);
	}

	private void assertError(String mensaje, Runnable accion) {
		ValidationException e = assertThrows(ValidationException.class, accion::run);
		assertTrue(e.getMessage().contains(mensaje), "Mensaje inesperado: " + e.getMessage());
	}

	private void assertNoGuarda() {
		assertEquals(1, db.executeQueryArray("SELECT 1 FROM Fip").size());
	}

	// //////////////////////////////////////////////
	// Clases válidas
	// //////////////////////////////////////////////

	/** TF-01: nombre no vacío, profesor existente e inicio posterior a hoy + 7 */
	@Test
	public void testCrearFipValida() {
		assertEquals("FIP006", crear("  Excel avanzado  ", PROFESOR, INICIO, FIN));

		List<Object[]> filas = db.executeQueryArray("SELECT nombre, profesorId, formadorExternoId, fechaCreacion, "
				+ "fechaInicio, fechaFin, fechaCierreActa, cierreAutomatico FROM Fip WHERE fipId = 'FIP006'");
		assertEquals(1, filas.size());
		Object[] fila = filas.get(0);
		assertAll(
				() -> assertEquals("Excel avanzado", fila[0]),
				() -> assertEquals(PROFESOR, fila[1]),
				() -> assertNull(fila[2]),
				() -> assertEquals(HOY, fila[3]),
				() -> assertEquals(INICIO, fila[4]),
				() -> assertEquals(FIN, fila[5]),
				() -> assertNull(fila[6]),
				() -> assertEquals(0, ((Number) fila[7]).intValue()));
	}

	/** TF-02: límite válido de la fecha de inicio, hoy + 8 */
	@Test
	public void testInicioLimiteValido() {
		assertEquals("FIP006", crear("Excel avanzado", PROFESOR, "2026-11-29", "2026-11-30"));
	}

	/** TF-08: límite válido de la fecha de fin, el mismo día que el inicio */
	@Test
	public void testFinIgualAInicio() {
		assertEquals("FIP006", crear("Excel avanzado", PROFESOR, INICIO, INICIO));
	}

	/** TF-11: el mismo profesor puede tener varias FIP, aunque coincidan en fechas */
	@Test
	public void testVariasFipDelMismoProfesor() {
		assertEquals("FIP006", crear("Excel avanzado", PROFESOR, INICIO, FIN));
		assertEquals("FIP007", crear("Word avanzado", PROFESOR, INICIO, FIN));
	}

	// //////////////////////////////////////////////
	// Clases no válidas
	// //////////////////////////////////////////////

	/** TF-03: límite no válido de la fecha de inicio, hoy + 7 */
	@Test
	public void testInicioLimiteNoValido() {
		assertError("La fecha de inicio debe ser posterior al 28/11/26",
				() -> crear("Excel avanzado", PROFESOR, "2026-11-28", FIN));
		assertNoGuarda();
	}

	/** TF-04: fecha de inicio hoy, anterior a hoy o sin indicar */
	@Test
	public void testInicioHoyOAnterior() {
		assertError("La fecha de inicio debe ser posterior al 28/11/26",
				() -> crear("Excel avanzado", PROFESOR, HOY, FIN));
		assertError("La fecha de inicio debe ser posterior al 28/11/26",
				() -> crear("Excel avanzado", PROFESOR, "2026-11-20", FIN));
		assertError("La fecha de inicio es obligatoria", () -> crear("Excel avanzado", PROFESOR, null, FIN));
		assertNoGuarda();
	}

	/** TF-05: nombre vacío, solo espacios o nulo */
	@Test
	public void testNombreVacio() {
		assertError("El nombre de la FIP es obligatorio", () -> crear("", PROFESOR, INICIO, FIN));
		assertError("El nombre de la FIP es obligatorio", () -> crear("   ", PROFESOR, INICIO, FIN));
		assertError("El nombre de la FIP es obligatorio", () -> crear(null, PROFESOR, INICIO, FIN));
		assertNoGuarda();
	}

	/** TF-06: profesor vacío o nulo */
	@Test
	public void testProfesorVacio() {
		assertError("El profesor de la FIP es obligatorio", () -> crear("Excel avanzado", "", INICIO, FIN));
		assertError("El profesor de la FIP es obligatorio", () -> crear("Excel avanzado", null, INICIO, FIN));
		assertNoGuarda();
	}

	/** TF-07: profesor que no existe */
	@Test
	public void testProfesorNoExiste() {
		assertError("El profesor P999 no existe", () -> crear("Excel avanzado", "P999", INICIO, FIN));
		assertNoGuarda();
	}

	/** TF-09: fecha de fin vacía o nula */
	@Test
	public void testFinVacia() {
		assertError("La fecha de fin es obligatoria", () -> crear("Excel avanzado", PROFESOR, INICIO, ""));
		assertError("La fecha de fin es obligatoria", () -> crear("Excel avanzado", PROFESOR, INICIO, null));
		assertNoGuarda();
	}

	/** TF-10: límite no válido de la fecha de fin, el día antes del inicio */
	@Test
	public void testFinAnteriorAInicio() {
		assertError("La fecha de fin no puede ser anterior a la de inicio",
				() -> crear("Excel avanzado", PROFESOR, INICIO, "2026-12-09"));
		assertNoGuarda();
	}

	// //////////////////////////////////////////////
	// Listado
	// //////////////////////////////////////////////

	/**
	 * TF-12: salen todas las FIP, también las de un formador externo, de la que empieza más tarde a la que empieza
	 * antes, con el nombre y los apellidos y el correo de quien la imparte
	 */
	@Test
	public void testListadoFips() {
		crear("Excel avanzado", PROFESOR, INICIO, FIN);
		db.executeBatch(new String[] {
				"INSERT INTO Fip (fipId, nombre, formadorExternoId, fechaCreacion, fechaInicio, fechaFin) "
						+ "VALUES ('FIP004','Prevencion de Riesgos','FE001','2026-10-20','2026-11-05','2026-11-10')"
		});

		List<FipDisplayDTO> fips = model.getFips();
		assertEquals(3, fips.size());
		assertAll(
				() -> assertEquals("FIP006", fips.get(0).getFipId()),
				() -> assertEquals("Excel avanzado", fips.get(0).getNombre()),
				() -> assertEquals("Ana Garcia", fips.get(0).getResponsable()),
				() -> assertEquals("p001@uni.es", fips.get(0).getEmail()),
				() -> assertEquals(INICIO, fips.get(0).getFechaInicio()),
				() -> assertEquals(FIN, fips.get(0).getFechaFin()),
				() -> assertEquals("FIP004", fips.get(1).getFipId()),
				() -> assertEquals("Elena Vega Morales", fips.get(1).getResponsable()),
				() -> assertEquals("elena.vega@formacion.es", fips.get(1).getEmail()),
				() -> assertEquals("FIP005", fips.get(2).getFipId()));
	}

	// //////////////////////////////////////////////
	// FIP con un formador externo como responsable (HU 37166)
	// //////////////////////////////////////////////

	private String crearConExterno(String nombre, String formadorExternoId, String fechaInicio, String fechaFin) {
		return model.crearFipFormadorExterno(nombre, formadorExternoId, fechaInicio, fechaFin, HOY);
	}

	/** TF-13: el responsable es el formador externo; profesorId queda nulo */
	@Test
	public void testCrearFipConFormadorExterno() {
		assertEquals("FIP006", crearConExterno("  Excel avanzado  ", "FE001", INICIO, FIN));

		List<Object[]> filas = db.executeQueryArray("SELECT nombre, profesorId, formadorExternoId, fechaCreacion, "
				+ "fechaInicio, fechaFin FROM Fip WHERE fipId = 'FIP006'");
		assertEquals(1, filas.size());
		Object[] fila = filas.get(0);
		assertAll(
				() -> assertEquals("Excel avanzado", fila[0]),
				() -> assertNull(fila[1]),
				() -> assertEquals("FE001", fila[2]),
				() -> assertEquals(HOY, fila[3]),
				() -> assertEquals(INICIO, fila[4]),
				() -> assertEquals(FIN, fila[5]));
	}

	/** TF-14: formador externo vacío, nulo o que no existe */
	@Test
	public void testFormadorExternoVacioONoExiste() {
		assertError("El formador externo de la FIP es obligatorio",
				() -> crearConExterno("Excel avanzado", "", INICIO, FIN));
		assertError("El formador externo de la FIP es obligatorio",
				() -> crearConExterno("Excel avanzado", null, INICIO, FIN));
		assertError("El formador externo FE999 no existe", () -> crearConExterno("Excel avanzado", "FE999", INICIO, FIN));
		assertNoGuarda();
	}

	/** TF-15: las mismas reglas de nombre y fechas que con un profesor de plantilla */
	@Test
	public void testFormadorExternoMismasReglas() {
		assertError("El nombre de la FIP es obligatorio", () -> crearConExterno("  ", "FE001", INICIO, FIN));
		assertError("La fecha de inicio debe ser posterior al 28/11/26",
				() -> crearConExterno("Excel avanzado", "FE001", "2026-11-28", FIN));
		assertError("La fecha de fin es obligatoria", () -> crearConExterno("Excel avanzado", "FE001", INICIO, null));
		assertError("La fecha de fin no puede ser anterior a la de inicio",
				() -> crearConExterno("Excel avanzado", "FE001", INICIO, "2026-12-09"));
		assertNoGuarda();
	}
}
