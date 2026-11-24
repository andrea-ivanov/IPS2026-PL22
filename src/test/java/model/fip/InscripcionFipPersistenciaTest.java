package model.fip;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import util.Database;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Pruebas de la HU 37046 que guardan en la base de datos: matricular en una FIP.
 * Cada prueba parte de sus propios datos (loadCleanDatabase), independientes de data.sql
 */
public class InscripcionFipPersistenciaTest {
	private static final String HOY = "2026-11-23";

	private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private InscripcionFIPModel model;

	@BeforeEach
	public void setUp() {
		db.createDatabase(false);
		loadCleanDatabase(db);
		model = new InscripcionFIPModel(db);
	}

	/** Las inscripciones y contratos impedirían a otras pruebas borrar alumnos y profesores (claves foráneas) */
	@AfterEach
	public void tearDown() {
		db.executeBatch(new String[] { "DELETE FROM InscripcionFip", "DELETE FROM Fip", "DELETE FROM FormadorExterno",
				"DELETE FROM ContratoAlumno" });
		db.createDatabase(false); // deja la tabla Fip como en schema.sql
	}

	/**
	 * Datos de prueba (hoy = 2026-11-23):
	 * - FIP001: responsable P001, empieza el 2026-12-01 (matrícula abierta), sin inscritos
	 * - FIP002: responsable el formador externo FE001, empezó el 2026-11-10; AL0001 inscrito (IF00001)
	 * - AL0001 tiene contrato del 2026-09-01 al 2027-06-30; AL0002 no tiene contratos
	 */
	public static void loadCleanDatabase(Database db) {
		db.executeBatch(new String[] {
				"INSERT INTO Alumno VALUES ('AL0001','10000001A','Lucia','Pascual Santos','lpascual@alumno.es')",
				"INSERT INTO Alumno VALUES ('AL0002','10000002B','Pablo','Garcia Ruiz','pgarcia@alumno.es')",
				"INSERT INTO Profesor VALUES ('P001','20000001A','Marina','Serrano Suarez','mserrano@uni.es')",
				"INSERT INTO Profesor VALUES ('P002','20000002B','Jorge','Santana Perez','jsantana@uni.es')",
				"INSERT INTO FormadorExterno VALUES ('FE001','Elena','Vega Morales','elena.vega@externa.es')",
				"INSERT INTO ContratoAlumno (contratoId, alumnoId, descripcion, fechaInicio, fechaFin) "
						+ "VALUES ('CT0001','AL0001','Becaria del departamento','2026-09-01','2027-06-30')",
				insertFip(db, "'FIP001','Metodologias Docentes','P001',NULL,'2026-11-01','2026-12-01','2026-12-10'"),
				insertFip(db, "'FIP002','Ciberseguridad',NULL,'FE001','2026-10-01','2026-11-10','2026-11-30'"),
				"INSERT INTO InscripcionFip (inscripcionId, fipId, alumnoId, profesorId, fechaInscripcion, resultado) "
						+ "VALUES ('IF00001','FIP002','AL0001',NULL,'2026-10-05','PENDIENTE')"
		});
	}

	/** Si el esquema ya tiene Fip.plazas se inserta con 30 plazas; si no, sin esa columna */
	private static String insertFip(Database db, String valores) {
		boolean plazas = tienePlazas(db);
		return "INSERT INTO Fip (fipId, nombre, profesorId, formadorExternoId, fechaCreacion, fechaInicio, fechaFin"
				+ (plazas ? ", plazas" : "") + ") VALUES (" + valores + (plazas ? ",30" : "") + ")";
	}

	private static boolean tienePlazas(Database db) {
		return !db.executeQueryArray("SELECT 1 FROM pragma_table_info('Fip') WHERE name = 'plazas'").isEmpty();
	}

	/**
	 * Fija el máximo de plazas de la FIP. Mientras el esquema no tenga Fip.plazas, la añade solo en la base de datos
	 * de pruebas (createDatabase la vuelve a crear según schema.sql).
	 */
	public static void fijarPlazas(Database db, String fipId, int plazas) {
		if (!tienePlazas(db)) {
			db.executeBatch(new String[] { "ALTER TABLE Fip ADD COLUMN plazas INT" });
		}
		db.executeUpdate("UPDATE Fip SET plazas = ? WHERE fipId = ?", plazas, fipId);
	}

	// //////////////////////////////////////////////
	// Consulta
	// //////////////////////////////////////////////

	/** Las FIP salen de la que empieza más tarde a la que empezó antes, con su responsable y sus plazas ocupadas */
	@Test
	public void testFips() {
		List<FIPDTO> fips = model.getFips();
		assertEquals(List.of("FIP001", "FIP002"), fips.stream().map(FIPDTO::getFipId).collect(Collectors.toList()));
		assertAll(() -> assertEquals("P001", fips.get(0).getProfesorResponsableId()),
				() -> assertEquals(0, fips.get(0).getPlazasOcupadas()),
				() -> assertEquals("2026-12-01", fips.get(0).getFechaInicio()),
				() -> assertNull(fips.get(1).getProfesorResponsableId(), "El responsable de FIP002 es un formador externo"),
				() -> assertEquals(1, fips.get(1).getPlazasOcupadas()));
	}

	// //////////////////////////////////////////////
	// Matricular
	// //////////////////////////////////////////////

	/** Un alumno con contrato vigente al inicio de la FIP queda inscrito como PENDIENTE con la fecha de hoy */
	@Test
	public void testMatricularAlumno() {
		assertEquals("IF00002", model.matricular("FIP001", " AL0001 ", TipoUsuarioFIP.ALUMNO, HOY));
		List<InscritoFipDTO> inscritos = model.getInscritos("FIP001");
		assertEquals(1, inscritos.size());
		InscritoFipDTO i = inscritos.get(0);
		assertAll(() -> assertEquals("IF00002", i.getInscripcionId()),
				() -> assertEquals("AL0001", i.getUsuarioId()),
				() -> assertEquals("Lucia Pascual Santos", i.getUsuario()),
				() -> assertEquals("ALUMNO", i.getTipo()),
				() -> assertEquals(HOY, i.getFechaInscripcion()),
				() -> assertEquals("PENDIENTE", i.getResultado()));
	}

	/** Una FIP creada por el administrador (HU 37165) admite matrículas desde ese mismo día, salvo la de su responsable */
	@Test
	public void testMatricularEnFipCreadaPorAdministrador() {
		String fipId = new FipModel(db).crearFip("Excel avanzado", "P002", "2026-12-05", "2026-12-20", HOY);
		assertEquals(fipId, model.getFips().get(0).getFipId());
		model.matricular(fipId, "AL0001", TipoUsuarioFIP.ALUMNO, HOY);
		assertThrows(ValidationException.class, () -> model.matricular(fipId, "P002", TipoUsuarioFIP.PROFESOR, HOY));
		assertEquals(1, model.getInscritos(fipId).size());
	}

	/** Un profesor que no es el responsable se matricula directamente */
	@Test
	public void testMatricularProfesor() {
		model.matricular("FIP001", "P002", TipoUsuarioFIP.PROFESOR, HOY);
		InscritoFipDTO i = model.getInscritos("FIP001").get(0);
		assertEquals("P002", i.getUsuarioId());
		assertEquals("PROFESOR", i.getTipo());
		assertEquals("Jorge Santana Perez", i.getUsuario());
	}

	/** No se puede matricular dos veces al mismo usuario en la misma FIP */
	@Test
	public void testMatriculaDuplicada() {
		model.matricular("FIP001", "AL0001", TipoUsuarioFIP.ALUMNO, HOY);
		assertThrows(ValidationException.class, () -> model.matricular("FIP001", "AL0001", TipoUsuarioFIP.ALUMNO, HOY));
		assertEquals(1, model.getInscritos("FIP001").size());
	}

	/** Un alumno sin contrato vigente al inicio de la FIP no se matricula */
	@Test
	public void testAlumnoSinContrato() {
		assertThrows(ValidationException.class, () -> model.matricular("FIP001", "AL0002", TipoUsuarioFIP.ALUMNO, HOY));
		assertTrue(model.getInscritos("FIP001").isEmpty());
	}

	/** El profesor responsable no se matricula en su propia FIP */
	@Test
	public void testResponsableNoSeMatricula() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.matricular("FIP001", "P001", TipoUsuarioFIP.PROFESOR, HOY));
		assertTrue(e.getMessage().contains("responsable"), e.getMessage());
		assertTrue(model.getInscritos("FIP001").isEmpty());
	}

	/** Desde el día de inicio de la FIP ya no se admiten altas */
	@Test
	public void testFipEmpezada() {
		assertThrows(ValidationException.class, () -> model.matricular("FIP002", "P002", TipoUsuarioFIP.PROFESOR, HOY));
		assertThrows(ValidationException.class,
				() -> model.matricular("FIP001", "P002", TipoUsuarioFIP.PROFESOR, "2026-12-01"));
		assertTrue(model.getInscritos("FIP001").isEmpty());
	}

	/** La matrícula se abre el día en que se crea la FIP: antes (solo posible con una fecha simulada) no se admite */
	@Test
	public void testFipSinCrear() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.matricular("FIP001", "AL0001", TipoUsuarioFIP.ALUMNO, "2026-10-31"));
		assertTrue(e.getMessage().contains("2026-11-01"), e.getMessage());
		assertTrue(model.getInscritos("FIP001").isEmpty());
		model.matricular("FIP001", "AL0001", TipoUsuarioFIP.ALUMNO, "2026-11-01");
		assertEquals(1, model.getInscritos("FIP001").size());
	}

	/** Al llenarse las plazas de la FIP se bloquea la matrícula */
	@Test
	public void testPlazasLlenas() {
		fijarPlazas(db, "FIP001", 1);
		model.matricular("FIP001", "AL0001", TipoUsuarioFIP.ALUMNO, HOY);
		assertEquals(1, model.getFips().get(0).getPlazasMaximas());
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.matricular("FIP001", "P002", TipoUsuarioFIP.PROFESOR, HOY));
		assertTrue(e.getMessage().contains("No quedan plazas"), e.getMessage());
		assertEquals(1, model.getInscritos("FIP001").size());
	}

	/** Los formadores externos no se matriculan */
	@Test
	public void testFormadorExterno() {
		assertThrows(ValidationException.class,
				() -> model.matricular("FIP001", "FE001", TipoUsuarioFIP.FORMADOR_EXTERNO, HOY));
	}

	/** La FIP y el usuario tienen que existir */
	@Test
	public void testNoExisten() {
		assertThrows(ResourceNotFoundException.class, () -> model.matricular("FIP999", "AL0001", TipoUsuarioFIP.ALUMNO, HOY));
		assertThrows(ResourceNotFoundException.class, () -> model.matricular("FIP001", "AL9999", TipoUsuarioFIP.ALUMNO, HOY));
		assertThrows(ResourceNotFoundException.class, () -> model.matricular("FIP001", "P999", TipoUsuarioFIP.PROFESOR, HOY));
	}
}
