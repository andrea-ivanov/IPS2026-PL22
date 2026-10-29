package model.propuesta;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import model.grado.GradoDisplayDTO;
import util.Database;
import util.exception.ValidationException;

/**
 * Pruebas de la HU 35243: propuestas de asignaturas del profesor (PropuestaModel).
 * Cada prueba parte de sus propios datos (loadCleanDatabase), independientes de data.sql
 */
public class PropuestaModelTest {
	private static final String HOY = "2026-10-19";
	private static final String PROFESOR = "P001";

	private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private PropuestaModel model;

	@BeforeEach
	public void setUp() {
		db.createDatabase(false);
		loadCleanDatabase(db);
		model = new PropuestaModel(db);
	}

	/**
	 * Datos de prueba:
	 * - P001 imparte Calculo (en G001 vigente y G003 en extincion) y Redes (en G002 vigente); no imparte en G004
	 * - P002 imparte en G004 y tiene propuestas en G001: Robotica PENDIENTE y Big Data RECHAZADA
	 * - La última propuesta es PR005, así que la siguiente es PR006
	 */
	public static void loadCleanDatabase(Database db) {
		db.executeBatch(new String[] {
				"DELETE FROM Calificacion", "DELETE FROM MatriculaGrupo", "DELETE FROM MatriculaCurso",
				"DELETE FROM Matricula", "DELETE FROM Acta", "DELETE FROM Convocatoria", "DELETE FROM Imparte",
				"DELETE FROM Grupo", "DELETE FROM PropuestaCurso", "DELETE FROM GradoCurso", "DELETE FROM Curso",
				"DELETE FROM Grado", "DELETE FROM AnioAcademico", "DELETE FROM Profesor", "DELETE FROM Alumno",
				"INSERT INTO Grado VALUES ('G001','Software','EII','Ingenieria y Arquitectura','VIGENTE','2024-09-01')",
				"INSERT INTO Grado VALUES ('G002','Tecnologias','EPI','Ingenieria y Arquitectura','VIGENTE','2025-09-01')",
				"INSERT INTO Grado VALUES ('G003','Matematicas','Ciencias','Ciencias','EN_EXTINCION','2021-09-01')",
				"INSERT INTO Grado VALUES ('G004','Datos','EII','Ingenieria y Arquitectura','VIGENTE','2022-09-01')",
				"INSERT INTO Profesor VALUES ('P001','10000001A','Ana','Garcia','p001@uni.es')",
				"INSERT INTO Profesor VALUES ('P002','10000002B','Luis','Fernandez','p002@uni.es')",
				"INSERT INTO Curso VALUES ('C001','P001','Calculo',6)",
				"INSERT INTO Curso VALUES ('C002','P001','Redes',6)",
				"INSERT INTO Curso VALUES ('C004','P002','Aprendizaje Automatico',6)",
				"INSERT INTO GradoCurso VALUES ('G001','C001',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G003','C001',1,NULL,'INACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G002','C002',2,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G004','C004',3,NULL,'ACTIVO')",
				"INSERT INTO Grupo VALUES ('C001','TE1','TE',80)", "INSERT INTO Grupo VALUES ('C002','TE1','TE',80)",
				"INSERT INTO Grupo VALUES ('C004','TE1','TE',80)",
				"INSERT INTO Imparte VALUES ('P001','C001','TE1')", "INSERT INTO Imparte VALUES ('P001','C002','TE1')",
				"INSERT INTO Imparte VALUES ('P002','C004','TE1')",
				"INSERT INTO PropuestaCurso VALUES ('PR004','P002','G001','Robotica',3,6,NULL,'2026-05-10','PENDIENTE')",
				"INSERT INTO PropuestaCurso VALUES ('PR005','P002','G001','Big Data',4,6,NULL,'2026-03-15','RECHAZADA')"
		});
	}

	// //////////////////////////////////////////////
	// Grados del profesor
	// //////////////////////////////////////////////

	/** Solo aparecen los grados en los que imparte el profesor (también los no vigentes, con su estado) */
	@Test
	public void testSoloGradosDondeImparte() {
		List<GradoDisplayDTO> grados = model.getGradosProfesor(PROFESOR);
		assertEquals(List.of("G003", "G001", "G002"), ids(grados)); // ordenados por nombre
		assertEquals("EN_EXTINCION", grados.get(0).getEstado());
		assertEquals(List.of("G004"), ids(model.getGradosProfesor("P002")));
	}

	// //////////////////////////////////////////////
	// Propuestas válidas
	// //////////////////////////////////////////////

	/** Una propuesta válida se guarda PENDIENTE con la fecha actual y aparece en "Mis propuestas" */
	@Test
	public void testPropuestaValida() {
		List<String> creadas = model.crearPropuestas(PROFESOR, List.of("G001"), " Robotica Movil ", "3", "6",
				"Robots que se mueven", HOY);
		assertEquals(List.of("PR006"), creadas);

		List<PropuestaDisplayDTO> mias = model.getPropuestas(PROFESOR);
		assertEquals(1, mias.size());
		PropuestaDisplayDTO p = mias.get(0);
		assertAll(() -> assertEquals("PR006", p.getPropuestaId()),
				() -> assertEquals("G001", p.getGradoId()),
				() -> assertEquals("Software", p.getGrado()),
				() -> assertEquals("Robotica Movil", p.getNombre()),
				() -> assertEquals(3, p.getAnio()),
				() -> assertEquals(6.0, p.getCreditos()),
				() -> assertEquals("Robots que se mueven", p.getDescripcion()),
				() -> assertEquals(HOY, p.getFechaPropuesta()),
				() -> assertEquals("PENDIENTE", p.getEstado()));
	}

	/** Si se eligen varios grados se crea una propuesta por grado; un grado repetido cuenta una vez */
	@Test
	public void testUnaPropuestaPorGrado() {
		List<String> creadas = model.crearPropuestas(PROFESOR, List.of("G001", "G002", "G001"), "Big Data Avanzado",
				"4", "4,5", "", HOY);
		assertEquals(List.of("PR006", "PR007"), creadas);
		List<PropuestaDisplayDTO> mias = model.getPropuestas(PROFESOR);
		assertEquals(2, mias.size());
		assertTrue(mias.stream().allMatch(p -> p.getCreditos() == 4.5 && "PENDIENTE".equals(p.getEstado())));
		// la descripción vacía se guarda como nula
		assertNull(mias.get(0).getDescripcion());
	}

	/** El nombre de una propuesta RECHAZADA se puede volver a proponer */
	@Test
	public void testNombreDePropuestaRechazadaSePuedeReutilizar() {
		assertEquals(1, model.crearPropuestas(PROFESOR, List.of("G001"), "big data", "4", "6", null, HOY).size());
	}

	/** validarPropuestas (la que usa la vista antes de pedir confirmación) no guarda nada */
	@Test
	public void testValidarNoGuarda() {
		model.validarPropuestas(PROFESOR, List.of("G001", "G002"), "Algo Nuevo", "2", "6", null);
		assertEquals(0, model.getPropuestas(PROFESOR).size());
	}

	// //////////////////////////////////////////////
	// Propuestas no válidas
	// //////////////////////////////////////////////

	/** Si alguna de las propuestas no es válida (grado no vigente) no se guarda ninguna */
	@Test
	public void testSiUnaNoEsValidaNoSeGuardaNinguna() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.crearPropuestas(PROFESOR, List.of("G001", "G003"), "Algebra", "1", "6", null, HOY));
		assertTrue(e.getMessage().contains("no está vigente"), e.getMessage());
		assertEquals(0, model.getPropuestas(PROFESOR).size());
	}

	/** No se puede proponer en un grado en el que no se imparte docencia */
	@Test
	public void testGradoDondeNoImparte() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.crearPropuestas(PROFESOR, List.of("G004"), "Algebra", "1", "6", null, HOY));
		assertEquals("No impartes docencia en el grado G004", e.getMessage());
	}

	/** Campos obligatorios vacíos y sin grados */
	@Test
	public void testCamposVacios() {
		assertAll(
				() -> assertMensaje("El nombre de la asignatura es obligatorio", List.of("G001"), "  ", "3", "6"),
				() -> assertMensaje("El año es obligatorio", List.of("G001"), "Algebra", "", "6"),
				() -> assertMensaje("Los créditos son obligatorios", List.of("G001"), "Algebra", "3", null),
				() -> assertMensaje("Selecciona al menos un grado", List.of(), "Algebra", "3", "6"));
	}

	/** El año debe ser un entero entre 1 y 6 */
	@Test
	public void testAnioNoValido() {
		for (String anio : new String[] { "0", "7", "-1", "2.5", "abc" }) {
			assertThrows(ValidationException.class,
					() -> model.crearPropuestas(PROFESOR, List.of("G001"), "Algebra", anio, "6", null, HOY), "Año " + anio);
		}
		// los límites son válidos
		model.crearPropuestas(PROFESOR, List.of("G001"), "Algebra I", "1", "6", null, HOY);
		model.crearPropuestas(PROFESOR, List.of("G001"), "Algebra II", "6", "6", null, HOY);
		assertEquals(2, model.getPropuestas(PROFESOR).size());
	}

	/** Los créditos deben ser un número mayor que 0, menor que 1000 y con como mucho un decimal */
	@Test
	public void testCreditosNoValidos() {
		for (String creditos : new String[] { "0", "-1", "abc", "4.55", "1000" }) {
			assertThrows(ValidationException.class,
					() -> model.crearPropuestas(PROFESOR, List.of("G001"), "Algebra", "1", creditos, null, HOY),
					"Créditos " + creditos);
		}
		assertEquals(0, model.getPropuestas(PROFESOR).size());
	}

	/** El nombre no puede repetirse en el grado: ni como asignatura ni como propuesta no rechazada (sin mayúsculas) */
	@Test
	public void testNombreDuplicadoEnElGrado() {
		ValidationException asignatura = assertThrows(ValidationException.class,
				() -> model.crearPropuestas(PROFESOR, List.of("G001"), "CALCULO", "1", "6", null, HOY));
		assertTrue(asignatura.getMessage().startsWith("Ya existe una asignatura o propuesta"), asignatura.getMessage());
		assertThrows(ValidationException.class,
				() -> model.crearPropuestas(PROFESOR, List.of("G001"), " robotica ", "3", "6", null, HOY));
		// en otro grado el mismo nombre sí se admite
		assertEquals(1, model.crearPropuestas(PROFESOR, List.of("G002"), "Calculo", "1", "6", null, HOY).size());
	}

	/** Nombre y descripción no pueden superar el tamaño de las columnas */
	@Test
	public void testLongitudMaxima() {
		String largo = "x".repeat(PropuestaModel.MAX_NOMBRE + 1);
		assertThrows(ValidationException.class,
				() -> model.crearPropuestas(PROFESOR, List.of("G001"), largo, "1", "6", null, HOY));
		String descripcionLarga = "x".repeat(PropuestaModel.MAX_DESCRIPCION + 1);
		assertThrows(ValidationException.class,
				() -> model.crearPropuestas(PROFESOR, List.of("G001"), "Algebra", "1", "6", descripcionLarga, HOY));
	}

	private void assertMensaje(String mensaje, List<String> grados, String nombre, String anio, String creditos) {
		ValidationException e = assertThrows(ValidationException.class,
				() -> model.crearPropuestas(PROFESOR, grados, nombre, anio, creditos, null, HOY));
		assertEquals(mensaje, e.getMessage());
	}

	private static List<String> ids(List<GradoDisplayDTO> grados) {
		return grados.stream().map(GradoDisplayDTO::getId).collect(Collectors.toList());
	}
}
