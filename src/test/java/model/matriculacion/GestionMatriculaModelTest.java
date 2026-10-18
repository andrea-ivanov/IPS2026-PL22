package model.matriculacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import util.Database;
import util.exception.ResourceNotFoundException;
import util.exception.ValidationException;

/**
 * Pruebas de la Secretaría sobre los datos de data.sql:
 * HU 35226 (consulta de matriculaciones) y HU 35229 (ciclo de vida de las matrículas).
 * Fecha de referencia de los datos: octubre de 2026 (el año 2026-2027 empezó el 2026-09-07).
 */
class GestionMatriculaModelTest {
	private static final String ANTES_INICIO = "2026-08-01";
	private static final String CURSO_INICIADO = "2026-10-17";
	private static final String ANIO_TERMINADO = "2027-08-01";

	private static Database db = new Database();
	private GestionMatriculaModel model;

	@BeforeEach
	void setUp() {
		db.createDatabase(false);
		db.loadDatabase();
		model = new GestionMatriculaModel(db);
	}

	private String estadoMatricula(String matriculaId) {
		return db.executeQueryArray("SELECT estado FROM Matricula WHERE matriculaId = ?", matriculaId).get(0)[0].toString();
	}

	// =========================================================================
	// HU 35226: consulta
	// =========================================================================

	@Test
	void listadoIncluyeTodosLosCursosMatriculados() {
		List<MatriculaSecretariaDTO> lista = model.getMatriculas();
		Number total = (Number) db.executeQueryArray("SELECT count(*) FROM MatriculaCurso").get(0)[0];
		assertEquals(total.intValue(), lista.size());
		// Ordenado del año más reciente al más antiguo
		assertEquals("2026-2027", lista.get(0).getAnioAcademicoId());
		assertEquals("2025-2026", lista.get(lista.size() - 1).getAnioAcademicoId());
	}

	@Test
	void detalleMuestraEstudianteGradoCursoGruposYEstado() {
		MatriculaSecretariaDTO m = model.getMatricula("M007", "C005");
		assertEquals("20000002B", m.getDni());
		assertEquals("Ingenieria Informatica en Tecnologias de la Informacion", m.getGrado());
		assertEquals("Fundamentos de Informatica", m.getCurso());
		assertEquals("ACTIVA", m.getEstado());
		assertTrue(m.getGrupo().contains("TE1"));
		assertTrue(m.getGrupo().contains("PA1"));
		assertTrue(m.getGrupo().contains("PL1"));
		assertEquals(120.0, m.getImporte(), 0.001);
	}

	@Test
	void cursoSinGruposSeMuestraSinAsignar() {
		assertEquals("Sin asignar", model.getMatricula("M015", "C005").getGrupo());
	}

	@Test
	void detalleDeMatriculaInexistenteFalla() {
		assertThrows(ResourceNotFoundException.class, () -> model.getMatricula("M999", "C005"));
	}

	// =========================================================================
	// HU 35226: consulta de grupos
	// =========================================================================

	@Test
	void aniosAcademicosOrdenadosYAnioActual() {
		assertEquals(List.of("2026-2027", "2025-2026"), model.getAniosAcademicos());
		assertEquals("2026-2027", model.getAnioAcademicoActual(CURSO_INICIADO));
		assertEquals("2025-2026", model.getAnioAcademicoActual(ANTES_INICIO));
	}

	@Test
	void gruposMuestranGradosProfesoresYOcupacionDelAnio() {
		List<GrupoConsultaDTO> grupos = model.getGruposConsulta("2026-2027");
		assertEquals(((Number) db.executeQueryArray("SELECT count(*) FROM Grupo").get(0)[0]).intValue(), grupos.size());
		GrupoConsultaDTO te1 = grupos.stream()
				.filter(g -> "C005".equals(g.getCursoId()) && "TE1".equals(g.getGrupoCodigo())).findFirst().get();
		assertEquals("Fundamentos de Informatica", te1.getCurso());
		assertEquals("TE", te1.getTipo());
		// Fundamentos está en Software, Tecnologías de la Información y el doble grado
		assertEquals(3, te1.getGrados().split("; ").length);
		assertTrue(te1.getProfesores().contains("Pablo"));
		// En 2026-2027 ocupan plaza M007, M011 y M016
		assertEquals(3, te1.getOcupadas());
		assertEquals(te1.getCapacidad() - 3, te1.getPlazasLibres());
		// En 2025-2026 la ocupación se cuenta aparte (M001, M002 y M004 ya están FINALIZADAS)
		GrupoConsultaDTO te1Anterior = model.getGruposConsulta("2025-2026").stream()
				.filter(g -> "C005".equals(g.getCursoId()) && "TE1".equals(g.getGrupoCodigo())).findFirst().get();
		assertEquals(0, te1Anterior.getOcupadas());
	}

	@Test
	void alumnosDeUnGrupoEnUnAnio() {
		List<MatriculaSecretariaDTO> alumnos = model.getMatriculasGrupo("C005", "TE1", "2026-2027");
		assertEquals(3, alumnos.size());
		assertTrue(alumnos.stream().anyMatch(m -> "M007".equals(m.getMatriculaId())));
		assertTrue(alumnos.stream().anyMatch(m -> "M011".equals(m.getMatriculaId())));
		assertTrue(alumnos.stream().anyMatch(m -> "M016".equals(m.getMatriculaId())));
		assertEquals(3, model.getMatriculasGrupo("C005", "TE1", "2025-2026").size());
		assertTrue(model.getMatriculasGrupo("C005", "PL2", "2025-2026").stream()
				.allMatch(m -> "2025-2026".equals(m.getAnioAcademicoId())));
	}

	// =========================================================================
	// HU 35229: activación al iniciar el curso
	// =========================================================================

	@Test
	void pagadaPasaAActivaAlIniciarElCurso() {
		assertEquals(1, model.activarMatriculasIniciadas(CURSO_INICIADO));
		assertEquals("ACTIVA", model.getMatricula("M015", "C005").getEstado());
		assertEquals("ACTIVA", estadoMatricula("M015"));
		// Las solicitadas sin pagar no cambian
		assertEquals("SOLICITADA", model.getMatricula("M010", "C005").getEstado());
		assertEquals("SOLICITADA", estadoMatricula("M010"));
	}

	@Test
	void pagadaNoSeActivaAntesDeIniciarElCurso() {
		assertEquals(0, model.activarMatriculasIniciadas(ANTES_INICIO));
		assertEquals("PAGADA", model.getMatricula("M015", "C005").getEstado());
		assertEquals("SOLICITADA", estadoMatricula("M015"));
	}

	// =========================================================================
	// HU 35229: aceptar solicitudes
	// =========================================================================

	@Test
	void aceptarAntesDeIniciarElCursoQuedaPagada() {
		assertEquals("PAGADA", model.aceptarSolicitud("M010", "C005", null, ANTES_INICIO));
		MatriculaSecretariaDTO m = model.getMatricula("M010", "C005");
		assertEquals(1, m.getPagoValidado());
		assertEquals("TRANSFERENCIA", m.getMetodoPago());
		assertEquals("SOLICITADA", estadoMatricula("M010"));
	}

	@Test
	void aceptarConElCursoIniciadoQuedaActiva() {
		assertEquals("ACTIVA", model.aceptarSolicitud("M010", "C006", null, CURSO_INICIADO));
		assertEquals("ACTIVA", estadoMatricula("M010"));
		// Solo cambia el curso aceptado
		assertEquals("SOLICITADA", model.getMatricula("M010", "C005").getEstado());
	}

	@Test
	void aceptarSinMetodoDePagoExigeIndicarlo() {
		assertThrows(ValidationException.class, () -> model.aceptarSolicitud("M011", "C006", null, CURSO_INICIADO));
		assertEquals("ACTIVA", model.aceptarSolicitud("M011", "C006", "TARJETA", CURSO_INICIADO));
		MatriculaSecretariaDTO m = model.getMatricula("M011", "C006");
		assertEquals("TARJETA", m.getMetodoPago());
		assertEquals(CURSO_INICIADO, m.getFechaPago());
		// El otro curso de la matrícula, que ya estaba activo, no cambia
		assertEquals("ACTIVA", model.getMatricula("M011", "C005").getEstado());
	}

	@Test
	void soloSeAceptanSolicitudes() {
		assertThrows(ValidationException.class, () -> model.aceptarSolicitud("M007", "C005", "TARJETA", CURSO_INICIADO));
		assertThrows(ValidationException.class, () -> model.aceptarSolicitud("M011", "C006", "BIZUM", CURSO_INICIADO));
	}

	// =========================================================================
	// HU 35229: modificar
	// =========================================================================

	@Test
	void modificarActualizaMetodoDePagoYEmail() {
		String fechaPagoAntes = model.getMatricula("M010", "C005").getFechaPago();
		model.modificarMatricula("M010", "C005", "CARTA_PAGO", "nuevo@alumno.es", CURSO_INICIADO);
		MatriculaSecretariaDTO m = model.getMatricula("M010", "C005");
		assertEquals("CARTA_PAGO", m.getMetodoPago());
		assertEquals("nuevo@alumno.es", m.getEmail());
		// Ya tenía fecha de pago: se conserva
		assertEquals(fechaPagoAntes, m.getFechaPago());
	}

	@Test
	void modificarSinMetodoMantieneElActual() {
		model.modificarMatricula("M007", "C005", null, "otro@alumno.es", CURSO_INICIADO);
		assertEquals("TRANSFERENCIA", model.getMatricula("M007", "C005").getMetodoPago());
	}

	@Test
	void modificarRegistraElPagoPendienteDeValidar() {
		// AL007 no había pagado C006: al registrar el método queda con fecha y pendiente de validar
		model.modificarMatricula("M011", "C006", "TRANSFERENCIA", "lsanchez2@alumno.es", CURSO_INICIADO);
		MatriculaSecretariaDTO m = model.getMatricula("M011", "C006");
		assertEquals("SOLICITADA", m.getEstado());
		assertEquals("TRANSFERENCIA", m.getMetodoPago());
		assertEquals(CURSO_INICIADO, m.getFechaPago());
		assertTrue(m.isPagoPendienteValidar());
	}

	@Test
	void modificarNoCambiaElMetodoDeUnPagoValidado() {
		// M007 pagó por transferencia y el pago está validado
		assertThrows(ValidationException.class,
				() -> model.modificarMatricula("M007", "C005", "TARJETA", "otro@alumno.es", CURSO_INICIADO));
		// Indicar el mismo método sí está permitido
		model.modificarMatricula("M007", "C005", "TRANSFERENCIA", "otro@alumno.es", CURSO_INICIADO);
		assertEquals("TRANSFERENCIA", model.getMatricula("M007", "C005").getMetodoPago());
	}

	@Test
	void modificarValidaLosDatos() {
		assertThrows(ValidationException.class,
				() -> model.modificarMatricula("M010", "C005", null, "sin-arroba", CURSO_INICIADO));
		// El email ya pertenece a otro estudiante (AL001)
		String emailOtro = db.executeQueryArray("SELECT email FROM Alumno WHERE alumnoId = 'AL001'").get(0)[0].toString();
		assertThrows(ValidationException.class,
				() -> model.modificarMatricula("M010", "C005", null, emailOtro, CURSO_INICIADO));
		// Matrícula cancelada
		assertThrows(ValidationException.class,
				() -> model.modificarMatricula("M013", "C005", null, "x@alumno.es", CURSO_INICIADO));
	}

	// =========================================================================
	// HU 35229: cancelar
	// =========================================================================

	@Test
	void cancelarUnCursoNoCancelaElResto() {
		model.cancelar("M010", "C005", "IMPAGO", CURSO_INICIADO, false);
		MatriculaSecretariaDTO m = model.getMatricula("M010", "C005");
		assertEquals("CANCELADA", m.getEstado());
		assertEquals("IMPAGO", m.getMotivoCancelacion());
		assertEquals(CURSO_INICIADO, m.getFechaCancelacion());
		assertEquals("SOLICITADA", model.getMatricula("M010", "C006").getEstado());
		assertEquals("SOLICITADA", estadoMatricula("M010"));
	}

	@Test
	void cancelarElUltimoCursoCancelaLaMatricula() {
		model.cancelar("M006", "C013", "FUERZA_MAYOR", CURSO_INICIADO, false);
		assertEquals("CANCELADA", estadoMatricula("M006"));
	}

	@Test
	void cancelarTodaLaMatriculaCancelaSusCursosYNoLaBorra() {
		model.cancelar("M016", "C005", "INCOMPATIBILIDAD", CURSO_INICIADO, true);
		assertEquals("CANCELADA", model.getMatricula("M016", "C005").getEstado());
		assertEquals("CANCELADA", model.getMatricula("M016", "C009").getEstado());
		Object[] fila = db.executeQueryArray(
				"SELECT estado, motivoCancelacion, fechaCancelacion FROM Matricula WHERE matriculaId = 'M016'").get(0);
		assertEquals("CANCELADA", fila[0]);
		assertEquals("INCOMPATIBILIDAD", fila[1]);
		assertEquals(CURSO_INICIADO, fila[2]);
	}

	// =========================================================================
	// HU 35229: finalización al terminar el año
	// =========================================================================

	@Test
	void activasPasanAFinalizadaAlTerminarElAnio() {
		// El 2026-2027 termina el 2027-07-30
		assertEquals(0, model.finalizarMatriculasTerminadas(CURSO_INICIADO));
		assertTrue(model.finalizarMatriculasTerminadas("2027-08-01") > 0);
		assertEquals("FINALIZADA", model.getMatricula("M007", "C005").getEstado());
		assertEquals("FINALIZADA", estadoMatricula("M007"));
		// Las canceladas y las que no llegaron a activarse no cambian
		assertEquals("CANCELADA", model.getMatricula("M006", "C001").getEstado());
		assertEquals("CANCELADA", estadoMatricula("M013"));
		assertEquals("SOLICITADA", model.getMatricula("M010", "C005").getEstado());
	}

	@Test
	void pagadaDeUnAnioYaTerminadoPasaAFinalizada() {
		// M015 pagó C005 pero la aplicación no se abrió hasta acabar el año: no puede quedarse PAGADA
		model.actualizarCicloVida(ANIO_TERMINADO);
		assertEquals("FINALIZADA", model.getMatricula("M015", "C005").getEstado());
		assertEquals("FINALIZADA", estadoMatricula("M015"));
		assertEquals("FINALIZADA", model.getMatricula("M007", "C005").getEstado());
	}

	@Test
	void actualizarCicloVidaActivaLasPagadasDelAnioEnCurso() {
		assertEquals(1, model.actualizarCicloVida(CURSO_INICIADO));
		assertEquals("ACTIVA", model.getMatricula("M015", "C005").getEstado());
		assertEquals("ACTIVA", estadoMatricula("M015"));
		// Ejecutarlo otra vez no cambia nada
		assertEquals(0, model.actualizarCicloVida(CURSO_INICIADO));
	}

	@Test
	void aceptarConElAnioTerminadoQuedaFinalizada() {
		assertEquals("FINALIZADA", model.aceptarSolicitud("M010", "C005", null, ANIO_TERMINADO));
		assertEquals(1, model.getMatricula("M010", "C005").getPagoValidado());
	}

	// =========================================================================
	// HU 35229: situaciones a revisar (consultas de base-de-datos.md)
	// =========================================================================

	@Test
	void detectaLasSituacionesQueRevisaElSecretario() {
		// AL006 pagó por transferencia: pendiente de validar
		assertTrue(model.getMatricula("M010", "C005").isPagoPendienteValidar());
		// AL007 tiene un curso sin pagar con el año iniciado
		assertTrue(model.getMatricula("M011", "C006").isSinPagarAnioIniciado(CURSO_INICIADO));
		assertTrue(!model.getMatricula("M011", "C006").isSinPagarAnioIniciado(ANTES_INICIO));
		// AL007 solo tiene TE en Fundamentos (le faltan PA y PL)
		MatriculaSecretariaDTO m011 = model.getMatricula("M011", "C005");
		assertTrue(m011.isFaltanGrupos());
		assertEquals(2, m011.getGruposPendientes());
		// M007 tiene todos sus grupos
		assertTrue(!model.getMatricula("M007", "C005").isFaltanGrupos());
	}

	// =========================================================================
	// HU 35229: asignación de grupos
	// =========================================================================

	private static Map<String, String> grupos(String... tipoYCodigo) {
		Map<String, String> map = new LinkedHashMap<>();
		for (int i = 0; i < tipoYCodigo.length; i += 2) {
			map.put(tipoYCodigo[i], tipoYCodigo[i + 1]);
		}
		return map;
	}

	@Test
	void gruposMuestranPlazasOcupadasDelAnio() {
		List<GrupoPlazasDTO> lista = model.getGrupos("M015", "C005");
		assertEquals(4, lista.size());
		GrupoPlazasDTO te1 = lista.stream().filter(g -> "TE1".equals(g.getGrupoCodigo())).findFirst().get();
		// En 2026-2027 están en TE1 de Fundamentos: M007, M011 y M016
		assertEquals(3, te1.getOcupadas());
		assertEquals(77, te1.getPlazasLibres());
		assertTrue(!te1.estaAsignado());
		// M007 sí está en TE1
		assertTrue(model.getGrupos("M007", "C005").stream()
				.anyMatch(g -> "TE1".equals(g.getGrupoCodigo()) && g.estaAsignado()));
	}

	@Test
	void asignarGruposCompletaLosQueFaltan() {
		model.asignarGrupos("M011", "C005", grupos("TE", "TE1", "PA", "PA1", "PL", "PL2"));
		MatriculaSecretariaDTO m = model.getMatricula("M011", "C005");
		assertEquals(0, m.getGruposPendientes());
		assertTrue(m.getGrupo().contains("PL2"));
	}

	@Test
	void asignarGruposPermiteCambiarDeGrupo() {
		model.asignarGrupos("M007", "C005", grupos("TE", "TE1", "PA", "PA1", "PL", "PL2"));
		String g = model.getMatricula("M007", "C005").getGrupo();
		assertTrue(g.contains("PL2"));
		assertTrue(!g.contains("PL1"));
	}

	@Test
	void noSePuedeSuperarLaCapacidadDelGrupo() {
		// PL1 de Fundamentos ya tiene a M007: con capacidad 1 está completo
		db.executeUpdate("UPDATE Grupo SET capacidad = 1 WHERE cursoId = 'C005' AND grupoCodigo = 'PL1'");
		model.activarMatriculasIniciadas(CURSO_INICIADO);
		assertThrows(ValidationException.class,
				() -> model.asignarGrupos("M015", "C005", grupos("TE", "TE1", "PA", "PA1", "PL", "PL1")));
		// Quien ya está en el grupo puede conservarlo
		model.asignarGrupos("M007", "C005", grupos("TE", "TE1", "PA", "PA1", "PL", "PL1"));
	}

	@Test
	void asignarGruposValidaEstadoTipoYQueEstenTodos() {
		// Solo a cursos PAGADOS o ACTIVOS
		assertThrows(ValidationException.class,
				() -> model.asignarGrupos("M010", "C005", grupos("TE", "TE1", "PA", "PA1", "PL", "PL1")));
		// Un grupo de cada tipo
		assertThrows(ValidationException.class, () -> model.asignarGrupos("M015", "C005", grupos("TE", "TE1")));
		// El grupo tiene que ser del tipo indicado
		assertThrows(ValidationException.class,
				() -> model.asignarGrupos("M015", "C005", grupos("TE", "PL1", "PA", "PA1", "PL", "PL2")));
		// El grupo tiene que existir en el curso
		assertThrows(ValidationException.class,
				() -> model.asignarGrupos("M015", "C005", grupos("TE", "TE9", "PA", "PA1", "PL", "PL2")));
	}

	@Test
	void cancelarValidaMotivoYEstado() {
		assertThrows(ValidationException.class, () -> model.cancelar("M010", "C005", "OTRO", CURSO_INICIADO, false));
		assertThrows(ValidationException.class, () -> model.cancelar("M013", "C005", "IMPAGO", CURSO_INICIADO, false));
		assertThrows(ValidationException.class, () -> model.cancelar("M001", "C005", "IMPAGO", CURSO_INICIADO, false));
		assertNull(model.getMatricula("M010", "C005").getMotivoCancelacion());
	}
}
