package repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import model.ActaDisplayDTO;
import model.AsignacionDocenteDisplayDTO;
import model.CalificacionDisplayDTO;
import util.ApplicationException;
import util.Database;

/**
 * Pruebas de la HU 35239: asignaciones del profesor, notas y cierre de actas.
 * Cada prueba parte de los datos de data.sql (fecha de referencia: octubre de 2026)
 */
public class TestActaRepository {
	private static final String HOY = "2026-10-12";
	// Acta abierta de Fundamentos de Informatica (coordinador P004, docente P006)
	private static final String CURSO = "C005";
	private static final String ANIO = "2026-2027";
	private static final String CONV = "ORDINARIA_ENERO";

	private static Database db = new Database();
	private ActaRepository actaRepository;
	private ProfesorRepository profesorRepository;

	@BeforeEach
	public void setUp() {
		db.createDatabase(false);
		db.loadDatabase();
		actaRepository = new ActaRepository();
		profesorRepository = new ProfesorRepository();
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
		List<CalificacionDisplayDTO> alumnos = getNotas();
		assertEquals(4, alumnos.size());

		Map<String, String> notas = new LinkedHashMap<>();
		notas.put(alumnos.get(0).getMatriculaId(), "7,5");
		notas.put(alumnos.get(1).getMatriculaId(), "NP");
		notas.put(alumnos.get(2).getMatriculaId(), "10");
		actaRepository.guardarNotas("P004", CURSO, ANIO, CONV, notas);

		List<CalificacionDisplayDTO> guardadas = getNotas();
		assertAll(() -> assertEquals("7.5", guardadas.get(0).getNota()),
				() -> assertEquals("NP", guardadas.get(1).getNota()),
				() -> assertEquals("10", guardadas.get(2).getNota()),
				() -> assertEquals("", guardadas.get(3).getNota()));
	}

	/** El coordinador puede cambiar una nota y quitarla dejandola vacia */
	@Test
	public void testCoordinadorCambiaYQuitaNota() {
		String matricula = getNotas().get(0).getMatriculaId();
		actaRepository.guardarNotas("P004", CURSO, ANIO, CONV, Map.of(matricula, "4"));
		actaRepository.guardarNotas("P004", CURSO, ANIO, CONV, Map.of(matricula, "6.25"));
		assertEquals("6.25", getNotas().get(0).getNota());

		actaRepository.guardarNotas("P004", CURSO, ANIO, CONV, Map.of(matricula, ""));
		assertEquals("", getNotas().get(0).getNota());
	}

	/** Las notas que no son numeros entre 0 y 10 se rechazan y no se guarda ninguna del acta */
	@Test
	public void testNotasNoValidas() {
		List<CalificacionDisplayDTO> alumnos = getNotas();
		for (String mal : new String[] { "11", "-1", "10.01", "abc", "5 puntos" }) {
			Map<String, String> notas = new LinkedHashMap<>();
			notas.put(alumnos.get(0).getMatriculaId(), "8");
			notas.put(alumnos.get(1).getMatriculaId(), mal);
			ApplicationException e = assertThrows(ApplicationException.class,
					() -> actaRepository.guardarNotas("P004", CURSO, ANIO, CONV, notas), "Nota " + mal);
			assertTrue(e.getMessage().contains("entre 0 y 10"));
		}
		assertEquals("", getNotas().get(0).getNota());
	}

	/** Los limites 0 y 10 son validos */
	@Test
	public void testNotasLimite() {
		List<CalificacionDisplayDTO> alumnos = getNotas();
		Map<String, String> notas = new LinkedHashMap<>();
		notas.put(alumnos.get(0).getMatriculaId(), "0");
		notas.put(alumnos.get(1).getMatriculaId(), "10");
		actaRepository.guardarNotas("P004", CURSO, ANIO, CONV, notas);
		assertEquals("0", getNotas().get(0).getNota());
	}

	/** El docente consulta las notas pero no puede modificarlas ni cerrar el acta */
	@Test
	public void testDocenteNoModifica() {
		Map<String, String> notas = Map.of(getNotas().get(0).getMatriculaId(), "5");
		ApplicationException e = assertThrows(ApplicationException.class,
				() -> actaRepository.guardarNotas("P006", CURSO, ANIO, CONV, notas));
		assertEquals("Solo el coordinador de la asignatura puede modificar las notas", e.getMessage());
		assertThrows(ApplicationException.class, () -> actaRepository.cerrarActa("P006", CURSO, ANIO, CONV, HOY));
		assertEquals(4, getNotas().size());
		assertEquals("ABIERTA", getActa(CURSO, ANIO, CONV).getEstado());
	}

	/** En un acta cerrada solo salen los alumnos con calificacion */
	@Test
	public void testConsultaActaCerrada() {
		List<CalificacionDisplayDTO> notas = actaRepository.getCalificaciones(CURSO, "2025-2026", "ORDINARIA_ENERO");
		assertEquals(3, notas.size());
	}

	// //////////////////////////////////////////////
	// Cierre de actas
	// //////////////////////////////////////////////

	/** El coordinador cierra el acta: queda cerrada manualmente y las notas pasan a solo lectura */
	@Test
	public void testCierreManual() {
		actaRepository.cerrarActa("P004", CURSO, ANIO, CONV, HOY);

		assertEquals("CERRADA", getActa(CURSO, ANIO, CONV).getEstado());
		List<Object[]> fila = db.executeQueryArray("SELECT fechaCierre, cierreAutomatico FROM Acta"
				+ " WHERE cursoId = ? AND anioAcademicoId = ? AND tipoConvocatoria = ?", CURSO, ANIO, CONV);
		assertEquals(HOY, fila.get(0)[0]);
		assertEquals(0, ((Number) fila.get(0)[1]).intValue());

		Map<String, String> notas = Map.of("M015", "5");
		ApplicationException e = assertThrows(ApplicationException.class,
				() -> actaRepository.guardarNotas("P004", CURSO, ANIO, CONV, notas));
		assertEquals("El acta esta cerrada: las notas son de solo lectura", e.getMessage());
		assertThrows(ApplicationException.class, () -> actaRepository.cerrarActa("P004", CURSO, ANIO, CONV, HOY));
	}

	/**
	 * Solo se cierra el acta abierta cuya convocatoria ya termino (C001 extraordinaria 2025-2026);
	 * las de 2026-2027 siguen abiertas. Cada acta cerrada asi aparece como notificacion para secretaria
	 */
	@Test
	public void testCierreAutomatico() {
		int notificacionesAntes = actaRepository.getActasCerradasAutomaticamente().size();

		List<ActaDisplayDTO> cerradas = actaRepository.cerrarActasVencidas(HOY);
		assertEquals(1, cerradas.size());
		assertEquals("C001", cerradas.get(0).getCursoId());
		assertEquals("EXTRAORDINARIA_JULIO", cerradas.get(0).getTipoConvocatoria());

		assertEquals("CERRADA", getActa("C001", "2025-2026", "EXTRAORDINARIA_JULIO").getEstado());
		assertEquals("ABIERTA", getActa(CURSO, ANIO, CONV).getEstado());
		assertEquals(notificacionesAntes + 1, actaRepository.getActasCerradasAutomaticamente().size());

		// una segunda ejecucion no cierra nada mas
		assertEquals(0, actaRepository.cerrarActasVencidas(HOY).size());
	}

	/** Al pasar el fin de la convocatoria de enero de 2027 se cierran todas las actas abiertas de enero */
	@Test
	public void testCierreAutomaticoTrasConvocatoria() {
		List<ActaDisplayDTO> cerradas = actaRepository.cerrarActasVencidas("2027-01-30");
		// la vencida de 2025-2026 y las 7 de enero de 2026-2027; la de junio sigue abierta
		assertEquals(8, cerradas.size());
		assertEquals("ABIERTA", getActa("C006", ANIO, "ORDINARIA_JUNIO").getEstado());
	}

	private List<CalificacionDisplayDTO> getNotas() {
		return actaRepository.getCalificaciones(CURSO, ANIO, CONV);
	}

	private ActaDisplayDTO getActa(String cursoId, String anio, String conv) {
		return actaRepository.getActasCurso(cursoId).stream()
				.filter(a -> a.getAnioAcademicoId().equals(anio) && a.getTipoConvocatoria().equals(conv))
				.findFirst().get();
	}
}
