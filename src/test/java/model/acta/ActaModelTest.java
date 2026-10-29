package model.acta;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import model.asignaciondocente.AsignacionDocenteDisplayDTO;
import model.asignaciondocente.RolDocente;
import repository.ProfesorRepository;
import util.Database;
import util.exception.ValidationException;

/**
 * Pruebas de la HU 35239: asignaciones del profesor, notas y cierre de actas (ActaModel).
 * Cada prueba parte de sus propios datos (loadCleanDatabase), independientes de data.sql,
 * con fecha de referencia octubre de 2026
 */
public class ActaModelTest {
	private static final String HOY = "2026-10-12";
	// Acta abierta de Fundamentos de Informatica (coordinador P004, docente P006)
	private static final String CURSO = "C005";
	private static final String ANIO = "2026-2027";
	private static final String CONV = "ORDINARIA_ENERO";

	private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private ActaModel actaModel;
	private ProfesorRepository profesorRepository;

	@BeforeEach
	public void setUp() {
		db.createDatabase(false);
		loadCleanDatabase(db);
		actaModel = new ActaModel(db);
		profesorRepository = new ProfesorRepository(db);
	}

	/**
	 * Datos de prueba:
	 * - P003 imparte 5 grupos: DOCENTE en C001 (coordina P001) y COORDINADOR en C003 y C013
	 * - C005 (coordina P004, imparte tambien P006) esta en 3 grados
	 * - Acta abierta de C005 en enero 2026-2027 con 4 alumnos calificables (uno mas SOLICITADA, que no sale)
	 * - Acta cerrada de C005 en enero 2025-2026 con 3 notas
	 * - Acta abierta de C001 en julio 2025-2026, con la convocatoria ya terminada: la cierra el sistema
	 * - Otra acta abierta de enero 2026-2027 (C003) y una de junio 2026-2027 (C006)
	 */
	public static void loadCleanDatabase(Database db) {
		db.executeBatch(new String[] {
				"DELETE FROM Calificacion", "DELETE FROM MatriculaGrupo", "DELETE FROM MatriculaCurso",
				"DELETE FROM Matricula", "DELETE FROM Acta", "DELETE FROM Convocatoria", "DELETE FROM Imparte",
				"DELETE FROM Grupo", "DELETE FROM PropuestaCurso", "DELETE FROM GradoCurso", "DELETE FROM Curso",
				"DELETE FROM Grado", "DELETE FROM AnioAcademico", "DELETE FROM Profesor", "DELETE FROM Alumno",
				"INSERT INTO AnioAcademico VALUES ('2025-2026','2025-09-08','2026-07-31','2025-07-01','2025-09-30')",
				"INSERT INTO AnioAcademico VALUES ('2026-2027','2026-09-07','2027-07-30','2026-07-01','2026-09-30')",
				"INSERT INTO Grado VALUES ('G001','Software','EII','Ingenieria y Arquitectura','VIGENTE','2024-09-01')",
				"INSERT INTO Grado VALUES ('G002','Tecnologias','EPI','Ingenieria y Arquitectura','VIGENTE','2025-09-01')",
				"INSERT INTO Grado VALUES ('G006','Doble Grado','EII','Ingenieria y Arquitectura','VIGENTE','2024-09-01')",
				"INSERT INTO Profesor VALUES ('P001','10000001A','Ana','Garcia','p001@uni.es')",
				"INSERT INTO Profesor VALUES ('P003','10000003C','Marta','Suarez','p003@uni.es')",
				"INSERT INTO Profesor VALUES ('P004','10000004D','Pablo','Menendez','p004@uni.es')",
				"INSERT INTO Profesor VALUES ('P006','10000006F','Jorge','Alonso','p006@uni.es')",
				"INSERT INTO Curso VALUES ('C001','P001','Proceso Software',6)",
				"INSERT INTO Curso VALUES ('C003','P003','Calidad',6)",
				"INSERT INTO Curso VALUES ('C005','P004','Fundamentos de Informatica',6)",
				"INSERT INTO Curso VALUES ('C006',NULL,'Calculo',6)",
				"INSERT INTO Curso VALUES ('C013','P003','Estructuras de Datos',6)",
				"INSERT INTO GradoCurso VALUES ('G001','C001',3,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G001','C003',3,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G001','C005',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G002','C005',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G006','C005',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G001','C006',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G001','C013',2,NULL,'ACTIVO')",
				"INSERT INTO Grupo VALUES ('C001','PA1','PA',40)", "INSERT INTO Grupo VALUES ('C001','PL2','PL',20)",
				"INSERT INTO Grupo VALUES ('C003','TE1','TE',80)", "INSERT INTO Grupo VALUES ('C003','PL1','PL',20)",
				"INSERT INTO Grupo VALUES ('C013','TE1','TE',80)", "INSERT INTO Grupo VALUES ('C005','TE1','TE',80)",
				"INSERT INTO Grupo VALUES ('C005','PA1','PA',40)",
				"INSERT INTO Imparte VALUES ('P003','C001','PA1')", "INSERT INTO Imparte VALUES ('P003','C001','PL2')",
				"INSERT INTO Imparte VALUES ('P003','C003','TE1')", "INSERT INTO Imparte VALUES ('P003','C003','PL1')",
				"INSERT INTO Imparte VALUES ('P003','C013','TE1')", "INSERT INTO Imparte VALUES ('P004','C005','TE1')",
				"INSERT INTO Imparte VALUES ('P006','C005','PA1')",
				"INSERT INTO Convocatoria VALUES ('2025-2026','ORDINARIA_ENERO','2026-01-08','2026-01-31')",
				"INSERT INTO Convocatoria VALUES ('2025-2026','EXTRAORDINARIA_JULIO','2026-06-29','2026-07-17')",
				"INSERT INTO Convocatoria VALUES ('2026-2027','ORDINARIA_ENERO','2027-01-07','2027-01-29')",
				"INSERT INTO Convocatoria VALUES ('2026-2027','ORDINARIA_JUNIO','2027-05-24','2027-06-14')",
				"INSERT INTO Acta VALUES ('C005','2025-2026','ORDINARIA_ENERO','CERRADA','2026-02-05',0)",
				"INSERT INTO Acta VALUES ('C001','2025-2026','EXTRAORDINARIA_JULIO','ABIERTA',NULL,0)",
				"INSERT INTO Acta VALUES ('C005','2026-2027','ORDINARIA_ENERO','ABIERTA',NULL,0)",
				"INSERT INTO Acta VALUES ('C003','2026-2027','ORDINARIA_ENERO','ABIERTA',NULL,0)",
				"INSERT INTO Acta VALUES ('C006','2026-2027','ORDINARIA_JUNIO','ABIERTA',NULL,0)",
				// 2025-2026: tres alumnos con nota en el acta cerrada de enero
				"INSERT INTO Alumno VALUES ('AL001','20000001A','Ana','Alvarez','al001@uni.es')",
				"INSERT INTO Alumno VALUES ('AL002','20000002B','Bruno','Blanco','al002@uni.es')",
				"INSERT INTO Alumno VALUES ('AL003','20000003C','Carla','Castro','al003@uni.es')",
				"INSERT INTO Matricula VALUES ('M001','AL001','G001','2025-2026','2025-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M002','AL002','G001','2025-2026','2025-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M003','AL003','G002','2025-2026','2025-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO MatriculaCurso VALUES ('M001','G001','C005','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M002','G001','C005','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M003','G002','C005','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-10',1)",
				"INSERT INTO Calificacion VALUES ('M001','C005','2025-2026','ORDINARIA_ENERO',7.5)",
				"INSERT INTO Calificacion VALUES ('M002','C005','2025-2026','ORDINARIA_ENERO',3.5)",
				"INSERT INTO Calificacion VALUES ('M003','C005','2025-2026','ORDINARIA_ENERO',6)",
				// 2026-2027: cuatro alumnos calificables en C005 (ACTIVA o PAGADA) y uno SOLICITADA que no sale
				"INSERT INTO Alumno VALUES ('AL011','20000011A','Diana','Diaz','al011@uni.es')",
				"INSERT INTO Alumno VALUES ('AL012','20000012B','Elena','Estevez','al012@uni.es')",
				"INSERT INTO Alumno VALUES ('AL013','20000013C','Fabio','Fernandez','al013@uni.es')",
				"INSERT INTO Alumno VALUES ('AL014','20000014D','Gema','Garcia','al014@uni.es')",
				"INSERT INTO Alumno VALUES ('AL015','20000015E','Hugo','Herrero','al015@uni.es')",
				"INSERT INTO Matricula VALUES ('M011','AL011','G001','2026-2027','2026-07-10','ACTIVA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M012','AL012','G001','2026-2027','2026-07-10','ACTIVA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M013','AL013','G002','2026-2027','2026-07-10','ACTIVA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M014','AL014','G006','2026-2027','2026-07-10','ACTIVA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M015','AL015','G001','2026-2027','2026-07-10','SOLICITADA',NULL,NULL)",
				"INSERT INTO MatriculaCurso VALUES ('M011','G001','C005','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M012','G001','C005','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M013','G002','C005','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M014','G006','C005','PAGADA',NULL,NULL,120,'TARJETA','2026-10-01',1)",
				"INSERT INTO MatriculaCurso VALUES ('M015','G001','C005','SOLICITADA',NULL,NULL,120,NULL,NULL,0)"
		});
	}

	// //////////////////////////////////////////////
	// Asignaciones del profesor
	// //////////////////////////////////////////////

	/** P003 solo ve sus grupos: DOCENTE en C001 y COORDINADOR en C003 y C013 */
	@Test
	public void testAsignacionesConRol() {
		List<AsignacionDocenteDisplayDTO> asignaciones = profesorRepository.getAsignacionesDocentes("P003");
		assertEquals(5, asignaciones.size());
		for (AsignacionDocenteDisplayDTO a : asignaciones) {
			String rolEsperado = "C001".equals(a.getCursoId()) ? "DOCENTE" : "COORDINADOR";
			assertEquals(rolEsperado, a.getRol(), "Rol de " + a.getCursoId() + " " + a.getGrupoCodigo());
		}
	}

	/** Una asignatura en varios grados muestra todos sus grados */
	@Test
	public void testAsignacionesConVariosGrados() {
		AsignacionDocenteDisplayDTO fundamentos = profesorRepository.getAsignacionesDocentes("P004").stream()
				.filter(a -> CURSO.equals(a.getCursoId())).findFirst().get();
		assertEquals(3, fundamentos.getGrado().split(", ").length);
	}

	// //////////////////////////////////////////////
	// Notas
	// //////////////////////////////////////////////

	/** El coordinador pone una nota, una nota con coma decimal y un NP */
	@Test
	public void testCoordinadorGuardaNotas() {
		List<CalificacionActaDisplayDTO> alumnos = getNotas();
		assertEquals(4, alumnos.size());

		Map<String, String> notas = new LinkedHashMap<>();
		notas.put(alumnos.get(0).getMatriculaId(), "7,5");
		notas.put(alumnos.get(1).getMatriculaId(), "NP");
		notas.put(alumnos.get(2).getMatriculaId(), "10");
		actaModel.guardarNotas("P004", CURSO, ANIO, CONV, notas);

		List<CalificacionActaDisplayDTO> guardadas = getNotas();
		assertAll(() -> assertEquals("7.5", guardadas.get(0).getNota()),
				() -> assertEquals("NP", guardadas.get(1).getNota()),
				() -> assertEquals("10", guardadas.get(2).getNota()),
				() -> assertEquals("", guardadas.get(3).getNota()));
	}

	/** El coordinador puede cambiar una nota y quitarla dejandola vacia */
	@Test
	public void testCoordinadorCambiaYQuitaNota() {
		String matricula = getNotas().get(0).getMatriculaId();
		actaModel.guardarNotas("P004", CURSO, ANIO, CONV, Map.of(matricula, "4"));
		actaModel.guardarNotas("P004", CURSO, ANIO, CONV, Map.of(matricula, "6.25"));
		assertEquals("6.25", getNotas().get(0).getNota());

		actaModel.guardarNotas("P004", CURSO, ANIO, CONV, Map.of(matricula, ""));
		assertEquals("", getNotas().get(0).getNota());
	}

	/** Las notas que no son numeros entre 0 y 10 se rechazan y no se guarda ninguna del acta */
	@Test
	public void testNotasNoValidas() {
		List<CalificacionActaDisplayDTO> alumnos = getNotas();
		for (String mal : new String[] { "11", "-1", "10.01", "abc", "5 puntos" }) {
			Map<String, String> notas = new LinkedHashMap<>();
			notas.put(alumnos.get(0).getMatriculaId(), "8");
			notas.put(alumnos.get(1).getMatriculaId(), mal);
			ValidationException e = assertThrows(ValidationException.class,
					() -> actaModel.guardarNotas("P004", CURSO, ANIO, CONV, notas), "Nota " + mal);
			assertTrue(e.getMessage().contains("entre 0 y 10"));
		}
		assertEquals("", getNotas().get(0).getNota());
	}

	/** Los limites 0 y 10 son validos */
	@Test
	public void testNotasLimite() {
		List<CalificacionActaDisplayDTO> alumnos = getNotas();
		Map<String, String> notas = new LinkedHashMap<>();
		notas.put(alumnos.get(0).getMatriculaId(), "0");
		notas.put(alumnos.get(1).getMatriculaId(), "10");
		actaModel.guardarNotas("P004", CURSO, ANIO, CONV, notas);
		assertEquals("0", getNotas().get(0).getNota());
	}

	/** El docente consulta las notas pero no puede modificarlas ni cerrar el acta */
	@Test
	public void testDocenteNoModifica() {
		Map<String, String> notas = Map.of(getNotas().get(0).getMatriculaId(), "5");
		ValidationException e = assertThrows(ValidationException.class,
				() -> actaModel.guardarNotas("P006", CURSO, ANIO, CONV, notas));
		assertEquals("Solo el coordinador de la asignatura puede modificar las notas", e.getMessage());
		assertThrows(ValidationException.class, () -> actaModel.cerrarActa("P006", CURSO, ANIO, CONV, HOY));
		assertEquals(4, getNotas().size());
		assertEquals("ABIERTA", getActa(CURSO, ANIO, CONV).getEstado());
	}

	/** Solo el coordinador con el acta abierta puede editar las notas (lo que habilita la tabla y los botones) */
	@Test
	public void testPuedeEditarNotas() {
		assertAll(() -> assertTrue(ActaModel.puedeEditarNotas(RolDocente.COORDINADOR, EstadoActa.ABIERTA)),
				() -> assertFalse(ActaModel.puedeEditarNotas(RolDocente.COORDINADOR, EstadoActa.CERRADA)),
				() -> assertFalse(ActaModel.puedeEditarNotas(RolDocente.DOCENTE, EstadoActa.ABIERTA)),
				() -> assertFalse(ActaModel.puedeEditarNotas(RolDocente.DOCENTE, EstadoActa.CERRADA)));
	}

	/** En un acta cerrada solo salen los alumnos con calificacion */
	@Test
	public void testConsultaActaCerrada() {
		List<CalificacionActaDisplayDTO> notas = actaModel.getCalificaciones(CURSO, "2025-2026", "ORDINARIA_ENERO");
		assertEquals(3, notas.size());
	}

	// //////////////////////////////////////////////
	// Cierre de actas
	// //////////////////////////////////////////////

	/** El coordinador cierra el acta: queda cerrada manualmente y las notas pasan a solo lectura */
	@Test
	public void testCierreManual() {
		actaModel.cerrarActa("P004", CURSO, ANIO, CONV, HOY);

		assertEquals("CERRADA", getActa(CURSO, ANIO, CONV).getEstado());
		List<Object[]> fila = db.executeQueryArray("SELECT fechaCierre, cierreAutomatico FROM Acta"
				+ " WHERE cursoId = ? AND anioAcademicoId = ? AND tipoConvocatoria = ?", CURSO, ANIO, CONV);
		assertEquals(HOY, fila.get(0)[0]);
		assertEquals(0, ((Number) fila.get(0)[1]).intValue());

		Map<String, String> notas = Map.of("M011", "5");
		ValidationException e = assertThrows(ValidationException.class,
				() -> actaModel.guardarNotas("P004", CURSO, ANIO, CONV, notas));
		assertEquals("El acta está cerrada: las notas son de solo lectura", e.getMessage());
		assertThrows(ValidationException.class, () -> actaModel.cerrarActa("P004", CURSO, ANIO, CONV, HOY));
	}

	/**
	 * Solo se cierra el acta abierta cuya convocatoria ya termino (C001 extraordinaria 2025-2026);
	 * las de 2026-2027 siguen abiertas. Cada acta cerrada asi aparece como notificacion para secretaria
	 */
	@Test
	public void testCierreAutomatico() {
		int notificacionesAntes = actaModel.getActasCerradasAutomaticamente().size();

		List<ActaDisplayDTO> cerradas = actaModel.cerrarActasVencidas(HOY);
		assertEquals(1, cerradas.size());
		assertEquals("C001", cerradas.get(0).getCursoId());
		assertEquals("EXTRAORDINARIA_JULIO", cerradas.get(0).getTipoConvocatoria());

		assertEquals("CERRADA", getActa("C001", "2025-2026", "EXTRAORDINARIA_JULIO").getEstado());
		assertEquals("ABIERTA", getActa(CURSO, ANIO, CONV).getEstado());
		assertEquals(notificacionesAntes + 1, actaModel.getActasCerradasAutomaticamente().size());

		// una segunda ejecucion no cierra nada mas
		assertEquals(0, actaModel.cerrarActasVencidas(HOY).size());
	}

	/** Al pasar el fin de la convocatoria de enero de 2027 se cierran todas las actas abiertas de enero */
	@Test
	public void testCierreAutomaticoTrasConvocatoria() {
		List<ActaDisplayDTO> cerradas = actaModel.cerrarActasVencidas("2027-01-30");
		// la vencida de 2025-2026 y las 2 de enero de 2026-2027 (C005 y C003); la de junio sigue abierta
		assertEquals(3, cerradas.size());
		assertEquals("ABIERTA", getActa("C006", ANIO, "ORDINARIA_JUNIO").getEstado());
	}

	/** Al cerrar a mano, los alumnos sin nota quedan como NP y siguen apareciendo en el acta cerrada */
	@Test
	public void testCierreManualPoneNPALosSinNota() {
		actaModel.guardarNotas("P004", CURSO, ANIO, CONV, Map.of("M011", "7"));
		actaModel.cerrarActa("P004", CURSO, ANIO, CONV, HOY);

		List<CalificacionActaDisplayDTO> notas = getNotas();
		assertEquals(4, notas.size());
		assertAll(() -> assertEquals("7", notas.get(0).getNota()),
				() -> assertEquals("NP", notas.get(1).getNota()),
				() -> assertEquals("NP", notas.get(2).getNota()),
				() -> assertEquals("NP", notas.get(3).getNota()));
		// el alumno SOLICITADA no entra en el acta, tampoco como NP
		assertTrue(db.executeQueryArray("SELECT 1 FROM Calificacion WHERE matriculaId = 'M015'").isEmpty());
	}

	/** El cierre automático también deja como NP a los alumnos sin nota (el acta no queda vacía) */
	@Test
	public void testCierreAutomaticoPoneNPALosSinNota() {
		actaModel.cerrarActasVencidas("2027-01-30");
		List<CalificacionActaDisplayDTO> notas = getNotas();
		assertEquals(4, notas.size());
		assertTrue(notas.stream().allMatch(n -> "NP".equals(n.getNota())));
	}

	/** Quien aprueba en enero no entra en el acta de una convocatoria posterior del mismo año; quien suspende sí */
	@Test
	public void testAprobadoNoApareceEnConvocatoriaPosterior() {
		db.executeUpdate("INSERT INTO Acta VALUES ('C005','2026-2027','ORDINARIA_JUNIO','ABIERTA',NULL,0)");
		Map<String, String> notas = new LinkedHashMap<>();
		notas.put("M011", "7");
		notas.put("M012", "3");
		actaModel.guardarNotas("P004", CURSO, ANIO, CONV, notas);

		List<CalificacionActaDisplayDTO> junio = actaModel.getCalificaciones(CURSO, ANIO, "ORDINARIA_JUNIO");
		assertEquals(3, junio.size());
		assertFalse(junio.stream().anyMatch(n -> "M011".equals(n.getMatriculaId())));
		assertTrue(junio.stream().anyMatch(n -> "M012".equals(n.getMatriculaId())));

		// al cerrar junio, el aprobado en enero no recibe NP
		actaModel.cerrarActa("P004", CURSO, ANIO, "ORDINARIA_JUNIO", HOY);
		assertTrue(db.executeQueryArray("SELECT 1 FROM Calificacion WHERE matriculaId = 'M011' "
				+ "AND tipoConvocatoria = 'ORDINARIA_JUNIO'").isEmpty());
		assertEquals(3, actaModel.getCalificaciones(CURSO, ANIO, "ORDINARIA_JUNIO").size());
	}

	private List<CalificacionActaDisplayDTO> getNotas() {
		return actaModel.getCalificaciones(CURSO, ANIO, CONV);
	}

	private ActaDisplayDTO getActa(String cursoId, String anio, String conv) {
		return actaModel.getActasCurso(cursoId).stream()
				.filter(a -> a.getAnioAcademicoId().equals(anio) && a.getTipoConvocatoria().equals(conv))
				.findFirst().get();
	}
}
