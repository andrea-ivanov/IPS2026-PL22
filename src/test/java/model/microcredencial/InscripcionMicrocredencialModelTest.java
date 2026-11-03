package model.microcredencial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import model.matriculacion.MatriculacionModel;
import util.Database;
import util.exception.ValidationException;

/**
 * Pruebas de la HU 35987: ramas habilitadas y oferta de microcredenciales del alumno
 * (InscripcionMicrocredencialModel). Cada prueba parte de sus propios datos, independientes de data.sql
 */
public class InscripcionMicrocredencialModelTest {
	private static final String HOY = "2026-11-03";
	private static final String RAMA_G1 = "Ingenieria y Arquitectura";

	private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private InscripcionMicrocredencialModel model;

	@BeforeEach
	public void setUp() {
		db.createDatabase(false);
		loadCleanDatabase(db);
		model = new InscripcionMicrocredencialModel(db);
	}

	/**
	 * Datos de prueba:
	 * - Años 2025-2026 (anterior) y 2026-2027 (actual); grados G1 Ingenieria y Arquitectura, G2 Ciencias y
	 *   G3 Ciencias de la Salud
	 * - AL1: matrícula 2026-2027 en G1 (activa), 2026-2027 en G3 (CANCELADA) y 2025-2026 en G2
	 * - AL2: matrícula 2026-2027 en G1, con una inscripción pagada en MC5 y otra solicitada sin pagar en MC6
	 * - AL3: solo matrícula 2025-2026 en G1
	 * - Microcredenciales de 2026-2027 (los nombres siguen el orden de los ids):
	 *   MC1 ACEPTADA G1 abierta hoy, 2 plazas; MC2 PROPUESTA G1; MC3 ACEPTADA G1 con el periodo cerrado;
	 *   MC4 ACEPTADA G3; MC5 ACEPTADA G1 abierta, 1 plaza (ocupada y pagada); MC6 ACEPTADA G1 abierta, 1 plaza
	 *   (solicitada sin pagar)
	 */
	public static void loadCleanDatabase(Database db) {
		db.executeBatch(new String[] {
				"INSERT INTO AnioAcademico VALUES ('2025-2026','2025-09-01','2026-08-31','2025-07-01','2025-09-30')",
				"INSERT INTO AnioAcademico VALUES ('2026-2027','2026-09-01','2027-08-31','2026-07-01','2026-09-30')",
				"INSERT INTO Grado VALUES ('G1','Ingenieria Informatica','Escuela de Ingenieria','Ingenieria y Arquitectura',"
						+ "'VIGENTE','2024-09-01')",
				"INSERT INTO Grado VALUES ('G2','Matematicas','Facultad de Ciencias','Ciencias','VIGENTE','2024-09-01')",
				"INSERT INTO Grado VALUES ('G3','Enfermeria','Facultad de Medicina','Ciencias de la Salud','VIGENTE',"
						+ "'2024-09-01')",
				"INSERT INTO Alumno VALUES ('AL1','10000001A','Juan','Perez','al1@uni.es')",
				"INSERT INTO Alumno VALUES ('AL2','10000002B','Maria','Gomez','al2@uni.es')",
				"INSERT INTO Alumno VALUES ('AL3','10000003C','Pedro','Lopez','al3@uni.es')",
				"INSERT INTO Profesor VALUES ('P001','20000001A','Ana','Garcia','p001@uni.es')",
				"INSERT INTO Matricula VALUES ('M1','AL1','G1','2026-2027','2026-07-10','ACTIVA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M2','AL1','G3','2026-2027','2026-07-10','CANCELADA','IMPAGO','2026-09-15')",
				"INSERT INTO Matricula VALUES ('M3','AL1','G2','2025-2026','2025-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M4','AL2','G1','2026-2027','2026-07-11','ACTIVA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M5','AL3','G1','2025-2026','2025-07-12','FINALIZADA',NULL,NULL)",
				microcredencial("MC1", "Microcredencial 1", RAMA_G1, 2, "2026-10-27", "2026-11-10", "ACEPTADA"),
				microcredencial("MC2", "Microcredencial 2", RAMA_G1, 10, "2026-10-27", "2026-11-10", "PROPUESTA"),
				microcredencial("MC3", "Microcredencial 3", RAMA_G1, 10, "2026-10-01", "2026-10-15", "ACEPTADA"),
				microcredencial("MC4", "Microcredencial 4", "Ciencias de la Salud", 10, "2026-10-27", "2026-11-10",
						"ACEPTADA"),
				microcredencial("MC5", "Microcredencial 5", RAMA_G1, 1, "2026-10-27", "2026-11-10", "ACEPTADA"),
				microcredencial("MC6", "Microcredencial 6", RAMA_G1, 1, "2026-10-27", "2026-11-10", "ACEPTADA"),
				"INSERT INTO MatriculaMicrocredencial VALUES ('M4','2026-2027','MC5','2026-10-28','INSCRITA',NULL,NULL,"
						+ "60,'TARJETA','2026-10-28',1,NULL)",
				"INSERT INTO MatriculaMicrocredencial VALUES ('M4','2026-2027','MC6','2026-10-28','SOLICITADA',NULL,NULL,"
						+ "60,'TRANSFERENCIA','2026-10-28',0,NULL)"
		});
	}

	/** Microcredencial de 3 créditos de 2026-2027, impartida del 2026-11-20 al 2027-01-30 */
	private static String microcredencial(String id, String nombre, String rama, int plazas, String inicioMat,
			String finMat, String estado) {
		String resolucion = "PROPUESTA".equals(estado) ? "NULL" : "'2026-10-20'";
		return "INSERT INTO Microcredencial VALUES ('" + id + "','2026-2027','P001','" + nombre + "','" + rama + "',3,"
				+ plazas + ",'" + inicioMat + "','" + finMat + "','2026-11-20','2027-01-30','" + estado
				+ "','2026-10-01'," + resolucion + ",NULL,NULL,NULL,0)";
	}

	private static List<String> ids(List<OfertaMicrocredencialDTO> oferta) {
		return oferta.stream().map(OfertaMicrocredencialDTO::getMicrocredencialId).toList();
	}

	private OfertaMicrocredencialDTO buscar(List<OfertaMicrocredencialDTO> oferta, String id) {
		return oferta.stream().filter(o -> id.equals(o.getMicrocredencialId())).findFirst().orElseThrow();
	}

	// //////////////////////////////////////////////
	// Ramas habilitadas
	// //////////////////////////////////////////////

	/** Solo cuentan las matrículas no canceladas del año actual (ni G3 cancelada ni G2 del año anterior) */
	@Test
	public void testRamasSoloMatriculasNoCanceladasDelAnioActual() {
		assertEquals(List.of(RAMA_G1), model.getRamasHabilitadas("AL1"));
	}

	/** Alumno nulo, inexistente o sin matrícula este año: sin ramas */
	@Test
	public void testRamasAlumnoNuloInexistenteOSinMatricula() {
		List<String> ramasNulo = model.getRamasHabilitadas(null);
		assertNotNull(ramasNulo);
		assertTrue(ramasNulo.isEmpty());
		assertTrue(model.getRamasHabilitadas("AL_INEXISTENTE_99999").isEmpty());
		assertTrue(model.getRamasHabilitadas("AL3").isEmpty());
	}

	// //////////////////////////////////////////////
	// Oferta
	// //////////////////////////////////////////////

	/** Solo ACEPTADAS, con el periodo abierto hoy, de sus ramas y con plazas pagadas libres */
	@Test
	public void testOfertaSoloAceptadasEnPeriodoYDeSusRamas() {
		List<OfertaMicrocredencialDTO> oferta = model.getOferta("AL1", HOY);
		assertEquals(List.of("MC1", "MC6"), ids(oferta));
		for (OfertaMicrocredencialDTO mc : oferta) {
			assertEquals(RAMA_G1, mc.getRamaConocimiento(),
					"Todas las microcredenciales ofertadas deben pertenecer a una rama habilitada");
			assertEquals("2026-2027", mc.getAnioAcademicoId());
		}
	}

	/** Las plazas libres solo descuentan las inscripciones con el pago validado */
	@Test
	public void testOfertaPlazasSoloCuentanPagadas() {
		List<OfertaMicrocredencialDTO> oferta = model.getOferta("AL1", HOY);
		assertEquals(1, buscar(oferta, "MC6").getPlazasLibres());
		assertEquals(2, buscar(oferta, "MC1").getPlazasLibres());
		assertEquals(2, buscar(oferta, "MC1").getPlazas());
	}

	/** Precio = créditos * PRECIO_CREDITO, con los datos de la microcredencial */
	@Test
	public void testOfertaPrecio() {
		OfertaMicrocredencialDTO mc1 = buscar(model.getOferta("AL1", HOY), "MC1");
		assertTrue(mc1.getCreditos() > 0, "Los créditos deben ser positivos");
		assertEquals(3.0, mc1.getCreditos(), 0.001);
		assertEquals(mc1.getCreditos() * MatriculacionModel.PRECIO_CREDITO, mc1.getPrecio(), 0.001);
		assertEquals("Microcredencial 1", mc1.getNombre());
		assertEquals("2026-10-27", mc1.getInicioMatriculacion());
		assertEquals("2026-11-10", mc1.getFinMatriculacion());
		assertEquals("2026-11-20", mc1.getInicioImparticion());
		assertEquals("2027-01-30", mc1.getFinImparticion());
	}

	/** Sin matrícula este año (solo la de 2025-2026) no hay oferta */
	@Test
	public void testOfertaSinMatriculaEsteAnioVacia() {
		List<OfertaMicrocredencialDTO> oferta = model.getOferta("AL3", HOY);
		assertNotNull(oferta);
		assertTrue(oferta.isEmpty());
	}

	/** Una microcredencial en la que ya está inscrito no vuelve a ofertarse */
	@Test
	public void testOfertaExcluyeYaInscrito() {
		db.executeUpdate("INSERT INTO MatriculaMicrocredencial VALUES ('M1','2026-2027','MC1','2026-11-01',"
				+ "'SOLICITADA',NULL,NULL,60,NULL,NULL,0,NULL)");
		assertEquals(List.of("MC6"), ids(model.getOferta("AL1", HOY)));
	}

	/** Con otra matrícula de la misma rama, estar inscrito desde cualquiera de ellas excluye la microcredencial */
	@Test
	public void testOfertaExcluyeInscritoDesdeOtraMatricula() {
		db.executeBatch(new String[] {
				"INSERT INTO Grado VALUES ('G4','Ingenieria Civil','Escuela de Ingenieria','Ingenieria y Arquitectura',"
						+ "'VIGENTE','2024-09-01')",
				"INSERT INTO Matricula VALUES ('M6','AL1','G4','2026-2027','2026-07-10','ACTIVA',NULL,NULL)",
				"INSERT INTO MatriculaMicrocredencial VALUES ('M6','2026-2027','MC1','2026-11-01',"
						+ "'SOLICITADA',NULL,NULL,60,NULL,NULL,0,NULL)"
		});
		assertEquals(List.of("MC6"), ids(model.getOferta("AL1", HOY)));
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.inscribir("AL1", List.of("MC1"), "TARJETA", HOY));
		assertTrue(e.getMessage().contains("Ya estás inscrito"));
	}

	// //////////////////////////////////////////////
	// Inscripción
	// //////////////////////////////////////////////

	private List<Object[]> filas(String alumnoId) {
		return db.executeQueryArray("SELECT mm.microcredencialId, mm.matriculaId, mm.estado, mm.pagoValidado, "
				+ "mm.metodoPago, mm.fechaPago, mm.fechaSolicitud, mm.importe, mm.nota, mm.motivoCancelacion, "
				+ "mm.fechaBaja FROM MatriculaMicrocredencial mm JOIN Matricula m ON m.matriculaId = mm.matriculaId "
				+ "WHERE m.alumnoId = ? ORDER BY mm.microcredencialId", alumnoId);
	}

	private int totalFilas() {
		return ((Number) db.executeQueryArray("SELECT COUNT(*) FROM MatriculaMicrocredencial").get(0)[0]).intValue();
	}

	@Test
	public void testInscribirConTarjetaQuedaInscrita() {
		model.inscribir("AL1", List.of("MC1"), "TARJETA", HOY);
		List<Object[]> filas = filas("AL1");
		assertEquals(1, filas.size());
		Object[] f = filas.get(0);
		assertEquals("MC1", f[0]);
		assertEquals("M1", f[1]);
		assertEquals("INSCRITA", f[2]);
		assertEquals(1, ((Number) f[3]).intValue());
		assertEquals("TARJETA", f[4]);
		assertEquals(HOY, f[5]);
		assertEquals(HOY, f[6]);
		assertEquals(3 * MatriculacionModel.PRECIO_CREDITO, ((Number) f[7]).doubleValue(), 0.001);
		assertEquals(null, f[8]);
		assertEquals(null, f[9]);
		assertEquals(null, f[10]);
	}

	@Test
	public void testInscribirConTransferenciaQuedaSolicitada() {
		model.inscribir("AL1", List.of("MC1"), "TRANSFERENCIA", HOY);
		Object[] f = filas("AL1").get(0);
		assertEquals("SOLICITADA", f[2]);
		assertEquals(0, ((Number) f[3]).intValue());
		assertEquals("TRANSFERENCIA", f[4]);
		assertEquals(HOY, f[5]);
	}

	@Test
	public void testInscribirVarias() {
		model.inscribir("AL1", List.of("MC1", "MC6"), "TARJETA", HOY);
		assertEquals(2, filas("AL1").size());
	}

	@Test
	public void testInscribirSinPlazasNoGuardaNinguna() {
		int antes = totalFilas();
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.inscribir("AL1", List.of("MC1", "MC5"), "TARJETA", HOY));
		assertTrue(e.getMessage().contains("Microcredencial 5"));
		assertTrue(e.getMessage().contains("plazas"));
		assertEquals(antes, totalFilas());
	}

	@Test
	public void testInscribirYaInscritoFalla() {
		model.inscribir("AL1", List.of("MC1"), "TARJETA", HOY);
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.inscribir("AL1", List.of("MC1"), "TARJETA", HOY));
		assertTrue(e.getMessage().contains("Ya estás inscrito"));
	}

	@Test
	public void testInscribirFueraDePeriodoORamaFalla() {
		assertThrows(ValidationException.class, () -> model.inscribir("AL1", List.of("MC3"), "TARJETA", HOY));
		assertThrows(ValidationException.class, () -> model.inscribir("AL1", List.of("MC4"), "TARJETA", HOY));
		assertTrue(filas("AL1").isEmpty());
	}

	@Test
	public void testInscribirListaVaciaFalla() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.inscribir("AL1", List.of(), "TARJETA", HOY));
		assertTrue(e.getMessage().contains("Selecciona al menos una microcredencial"));
	}

	@Test
	public void testInscribirMetodoPagoNoValido() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.inscribir("AL1", List.of("MC1"), "BIZUM", HOY));
		assertTrue(e.getMessage().contains("Debe seleccionar un método de pago"));
		assertFalse(db.executeQueryArray("SELECT 1 FROM MatriculaMicrocredencial WHERE matriculaId = 'M1'")
				.iterator().hasNext());
	}
}
