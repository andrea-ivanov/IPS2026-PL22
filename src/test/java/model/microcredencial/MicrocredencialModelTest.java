package model.microcredencial;

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
 * Pruebas de la HU 35988: el profesor propone una microcredencial (MicrocredencialModel).
 * Cada prueba parte de sus propios datos (loadCleanDatabase), independientes de data.sql
 */
public class MicrocredencialModelTest {
	private static final String HOY = "2026-11-03";
	private static final String ANIO = "2026-2027";
	private static final String PROFESOR = "P001";
	private static final String RAMA = "Ingenieria y Arquitectura";

	private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private MicrocredencialModel model;

	@BeforeEach
	public void setUp() {
		db.createDatabase(false);
		loadCleanDatabase(db);
		model = new MicrocredencialModel(db);
	}

	/**
	 * Datos de prueba:
	 * - Año académico 2026-2027 y dos profesores
	 * - P002 tiene la microcredencial MC052 "Docker" RECHAZADA en 2026-2027, así que la siguiente es MC053
	 */
	public static void loadCleanDatabase(Database db) {
		db.executeBatch(new String[] {
				"DELETE FROM MatriculaMicrocredencial", "DELETE FROM Microcredencial",
				"DELETE FROM AnioAcademico", "DELETE FROM Profesor",
				"INSERT INTO AnioAcademico VALUES ('2026-2027','2026-09-01','2027-08-31','2026-07-01','2026-09-30')",
				"INSERT INTO Profesor VALUES ('P001','10000001A','Ana','Garcia','p001@uni.es')",
				"INSERT INTO Profesor VALUES ('P002','10000002B','Luis','Fernandez','p002@uni.es')",
				"INSERT INTO Microcredencial VALUES ('MC052','2026-2027','P002','Docker','Ingenieria y Arquitectura',3,25,"
						+ "'2026-10-01','2026-10-15','2026-10-20','2026-12-20','RECHAZADA','2026-09-10','2026-09-20',"
						+ "'Solapada',NULL,NULL,0)"
		});
	}

	/** Crea una propuesta válida cambiando solo los campos indicados */
	private String crear(String nombre, String rama, String plazas, String creditos, String inicioMat,
			String inicioImp, String finImp) {
		return model.crearMicrocredencial(PROFESOR, ANIO, nombre, rama, plazas, creditos, inicioMat, inicioImp, finImp,
				HOY);
	}

	private String crearValida() {
		return crear("  Spring Boot  ", RAMA, "30", "4,5", "2027-02-01", "2027-02-22", "2027-04-16");
	}

	private void assertError(String mensaje, Runnable accion) {
		ValidationException e = assertThrows(ValidationException.class, accion::run);
		assertTrue(e.getMessage().contains(mensaje), "Mensaje inesperado: " + e.getMessage());
	}

	// //////////////////////////////////////////////
	// Alta válida
	// //////////////////////////////////////////////

	/** Se guarda como PROPUESTA con el siguiente id, el fin de matriculación calculado y la fecha de hoy */
	@Test
	public void testCrearPropuestaValida() {
		assertEquals("MC053", crearValida());

		List<MicrocredencialDisplayDTO> mias = model.getMicrocredenciales(PROFESOR);
		assertEquals(1, mias.size());
		MicrocredencialDisplayDTO mc = mias.get(0);
		assertAll(
				() -> assertEquals("MC053", mc.getMicrocredencialId()),
				() -> assertEquals(ANIO, mc.getAnioAcademicoId()),
				() -> assertEquals("Spring Boot", mc.getNombre()),
				() -> assertEquals(RAMA, mc.getRamaConocimiento()),
				() -> assertEquals(4.5, mc.getCreditos()),
				() -> assertEquals(30, mc.getPlazas()),
				() -> assertEquals("2027-02-01", mc.getInicioMatriculacion()),
				() -> assertEquals("2027-02-15", mc.getFinMatriculacion()),
				() -> assertEquals("2027-02-22", mc.getInicioImparticion()),
				() -> assertEquals("2027-04-16", mc.getFinImparticion()),
				() -> assertEquals(HOY, mc.getFechaPropuesta()),
				() -> assertEquals("PROPUESTA", mc.getEstado()));
		List<Object[]> resto = db.executeQueryArray("SELECT fechaResolucion, motivoRechazo, cierreAutomatico "
				+ "FROM Microcredencial WHERE microcredencialId = 'MC053'");
		assertNull(resto.get(0)[0]);
		assertNull(resto.get(0)[1]);
		assertEquals(0, ((Number) resto.get(0)[2]).intValue());
	}

	/** Los límites admitidos: créditos justo en el máximo, matriculación desde hoy e impartición al día siguiente */
	@Test
	public void testLimitesValidos() {
		assertEquals("MC053", crear("Limites", RAMA, "1", "15", HOY, "2026-11-18", "2026-11-19"));
	}

	/** Solo ve las suyas: las de otros profesores no salen */
	@Test
	public void testSoloSusMicrocredenciales() {
		assertTrue(model.getMicrocredenciales(PROFESOR).isEmpty());
		assertEquals(1, model.getMicrocredenciales("P002").size());
	}

	@Test
	public void testCalcularFinMatriculacion() {
		assertEquals("2027-02-15", MicrocredencialModel.calcularFinMatriculacion(" 2027-02-01 "));
		assertEquals("", MicrocredencialModel.calcularFinMatriculacion("01/02/2027"));
		assertEquals("", MicrocredencialModel.calcularFinMatriculacion(null));
	}

	// //////////////////////////////////////////////
	// Validaciones
	// //////////////////////////////////////////////

	@Test
	public void testCamposObligatorios() {
		assertError("nombre", () -> crear(" ", RAMA, "30", "3", "2027-02-01", "2027-02-22", "2027-04-16"));
		assertError("rama", () -> crear("X", null, "30", "3", "2027-02-01", "2027-02-22", "2027-04-16"));
		assertError("plazas", () -> crear("X", RAMA, "", "3", "2027-02-01", "2027-02-22", "2027-04-16"));
		assertError("créditos", () -> crear("X", RAMA, "30", "", "2027-02-01", "2027-02-22", "2027-04-16"));
		assertError("inicio de la matriculación", () -> crear("X", RAMA, "30", "3", "", "2027-02-22", "2027-04-16"));
		assertError("inicio de la impartición", () -> crear("X", RAMA, "30", "3", "2027-02-01", "", "2027-04-16"));
		assertError("fin de la impartición", () -> crear("X", RAMA, "30", "3", "2027-02-01", "2027-02-22", null));
		assertError("año académico", () -> model.crearMicrocredencial(PROFESOR, null, "X", RAMA, "30", "3",
				"2027-02-01", "2027-02-22", "2027-04-16", HOY));
	}

	/** El nombre admite hasta MAX_NOMBRE caracteres, sin contar los espacios de los extremos */
	@Test
	public void testLongitudNombre() {
		String maximo = "N".repeat(MicrocredencialModel.MAX_NOMBRE);
		assertError("no puede superar los 150 caracteres",
				() -> crear(maximo + "X", RAMA, "30", "3", "2027-02-01", "2027-02-22", "2027-04-16"));
		assertEquals("MC053", crear(" " + maximo + " ", RAMA, "30", "3", "2027-02-01", "2027-02-22", "2027-04-16"));
	}

	/** Solo las cinco ramas oficiales, escritas sin tildes */
	@Test
	public void testRamaNoValida() {
		assertError("rama de conocimiento no es válida",
				() -> crear("X", "Ingeniería y Arquitectura", "30", "3", "2027-02-01", "2027-02-22", "2027-04-16"));
	}

	@Test
	public void testPlazasNoValidas() {
		assertError("entero", () -> crear("X", RAMA, "2.5", "3", "2027-02-01", "2027-02-22", "2027-04-16"));
		assertError("mayores que 0", () -> crear("X", RAMA, "0", "3", "2027-02-01", "2027-02-22", "2027-04-16"));
	}

	@Test
	public void testCreditosNoValidos() {
		assertError("mayores que 0", () -> crear("X", RAMA, "30", "0", "2027-02-01", "2027-02-22", "2027-04-16"));
		assertError("máximo de 15", () -> crear("X", RAMA, "30", "15.5", "2027-02-01", "2027-02-22", "2027-04-16"));
		assertError("número", () -> crear("X", RAMA, "30", "tres", "2027-02-01", "2027-02-22", "2027-04-16"));
	}

	@Test
	public void testFormatoFecha() {
		assertError("AAAA-MM-DD", () -> crear("X", RAMA, "30", "3", "01/02/2027", "2027-02-22", "2027-04-16"));
		assertError("AAAA-MM-DD", () -> crear("X", RAMA, "30", "3", "2027-02-01", "2027-02-30", "2027-04-16"));
	}

	@Test
	public void testMatriculacionAnteriorAHoy() {
		assertError("anterior a hoy", () -> crear("X", RAMA, "30", "3", "2026-11-02", "2026-11-20", "2026-12-20"));
	}

	/** La impartición empieza después del fin de la matriculación (inicio + 14 días) y acaba después de empezar */
	@Test
	public void testFechasImparticion() {
		assertError("posterior al fin de la matriculación (2027-02-15)",
				() -> crear("X", RAMA, "30", "3", "2027-02-01", "2027-02-15", "2027-04-16"));
		assertError("posterior a su inicio", () -> crear("X", RAMA, "30", "3", "2027-02-01", "2027-02-22", "2027-02-22"));
	}

	/** El nombre no se repite en el año aunque cambien las mayúsculas, y cuentan también las rechazadas */
	@Test
	public void testNombreRepetidoEnElAnio() {
		assertError("Ya existe", () -> crear(" docker ", RAMA, "30", "3", "2027-02-01", "2027-02-22", "2027-04-16"));
		crearValida();
		assertError("Ya existe", () -> crear("SPRING BOOT", RAMA, "30", "3", "2027-02-01", "2027-02-22", "2027-04-16"));
	}

	/** Si la validación falla no se guarda nada */
	@Test
	public void testNoGuardaSiFalla() {
		assertThrows(ValidationException.class,
				() -> crear("X", RAMA, "30", "20", "2027-02-01", "2027-02-22", "2027-04-16"));
		assertEquals(1, db.executeQueryArray("SELECT 1 FROM Microcredencial").size());
	}
}
