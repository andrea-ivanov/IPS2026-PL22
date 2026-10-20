package model.matriculacion;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import model.fixtures.BaseModelTest;
import util.Database;
import util.UnexpectedException;
import util.exception.ValidationException;

/**
 * Pruebas unitarias para la historia de usuario HU 35235:
 * Solicitud de matrícula en grados y cursos ofertados (Fases 1 a 5).
 */
public class MatriculacionSolicitudModelTest extends BaseModelTest {
	private MatriculacionModel matriculacionModel;

	@BeforeEach
	public void initModel() {
		this.matriculacionModel = new MatriculacionModel(db);
	}

	@Test
	@DisplayName("HU 35235 - Fase 2: Consultar grados disponibles excluyendo en extinción y extintos")
	public void testConsultarGradosDisponibles() {
		// Insertar grados con estados variados
		db.executeUpdate("INSERT INTO Grado (gradoId, nombre, facultad, ramaConocimiento, estado, fechaInicioVigencia) "
				+ "VALUES ('3', 'Grado Extinto', 'Facultad X', 'Ciencias', 'EXTINTO', '2020-09-01')");
		db.executeUpdate("INSERT INTO Grado (gradoId, nombre, facultad, ramaConocimiento, estado, fechaInicioVigencia) "
				+ "VALUES ('4', 'Grado Extinción', 'Facultad X', 'Ciencias', 'EN_EXTINCION', '2020-09-01')");
		db.executeUpdate("INSERT INTO Grado (gradoId, nombre, facultad, ramaConocimiento, estado, fechaInicioVigencia) "
				+ "VALUES ('5', 'Grado Renovación', 'Facultad X', 'Ciencias', 'PENDIENTE_RENOVACION', '2024-09-01')");

		List<OfertaGradoDTO> disponibles = matriculacionModel.getOfertaGradosDisponibles();
		assertNotNull(disponibles, "La lista de grados disponibles no debe ser nula");
		assertFalse(disponibles.isEmpty(), "Debe contener grados disponibles");

		for (OfertaGradoDTO g : disponibles) {
			assertNotEquals("EXTINTO", g.getEstado(), "No debe incluir grados extintos");
			assertNotEquals("EN_EXTINCION", g.getEstado(), "No debe incluir grados en extinción");
			assertTrue("VIGENTE".equals(g.getEstado()) || "PENDIENTE_RENOVACION".equals(g.getEstado()),
					"El estado debe ser VIGENTE o PENDIENTE_RENOVACION");
		}
	}

	@Test
	@DisplayName("HU 35235 - Fase 2: Consultar cursos activos por grado con cálculo de precio")
	public void testConsultarCursosPorGrado() {
		// Grado 1 tiene Curso 1 (Programacion, 6 cr) y Curso 2 (Bases de Datos, 6 cr)
		List<OfertaCursoDTO> cursos = matriculacionModel.getOfertaCursosPorGrado("1", "1");
		assertNotNull(cursos);
		assertEquals(2, cursos.size(), "Grado 1 debe ofertar 2 cursos activos");

		OfertaCursoDTO c1 = cursos.stream().filter(c -> "1".equals(c.getCursoId())).findFirst().orElse(null);
		assertNotNull(c1, "Debe contener el curso con ID 1");
		assertEquals("Programación", c1.getNombre());
		assertEquals(6.0, c1.getCreditos());
		assertEquals(120.0, c1.getPrecio(), "El precio debe ser 6 * 20.0 = 120 €");
		assertEquals(0, c1.getConvocatoriasConsumidas());
		assertTrue(c1.isConvocatoriasDisponibles());
	}

	@Test
	@DisplayName("HU 35235 - Fase 2: Comprobar matrícula existente para evitar duplicados")
	public void testComprobarMatriculaExistente() {
		// Alumno 1 ya está matriculado en Grado 1 para Año 1
		assertTrue(matriculacionModel.comprobarMatriculaExistente("1", "1", "1"),
				"Debe detectar matrícula duplicada para el alumno 1 en grado 1");

		// Alumno 1 no está matriculado en Grado 2 para Año 1
		assertFalse(matriculacionModel.comprobarMatriculaExistente("1", "2", "1"),
				"No debe detectar matrícula para alumno 1 en grado 2");
	}

	@Test
	@DisplayName("HU 35235 - Fase 2: Comprobar que estudiante con impago previo no está al corriente de pago")
	public void testComprobarAlCorrienteDePago() {
		// Alumno 1 está al corriente de pago
		assertTrue(matriculacionModel.comprobarAlCorrienteDePago("1"),
				"El alumno 1 debe estar al corriente de pago");

		// Alumno 2 tiene matrícula cancelada por IMPAGO
		assertFalse(matriculacionModel.comprobarAlCorrienteDePago("2"),
				"El alumno 2 no debe estar al corriente por tener cancelación por IMPAGO");
	}

	@Test
	@DisplayName("HU 35235 - Fase 2: Comprobar cómputo de convocatorias consumidas")
	public void testComprobarConvocatorias() {
		// Sin notas
		assertEquals(0, matriculacionModel.comprobarConvocatorias("1", "1"));

		// Añadir acta y calificación
		db.executeUpdate("INSERT INTO Convocatoria (anioAcademicoId, tipoConvocatoria, fechaInicio, fechaFin) "
				+ "VALUES ('1', 'ORDINARIA_ENERO', '2027-01-10', '2027-01-25')");
		db.executeUpdate(
				"INSERT INTO Acta (cursoId, anioAcademicoId, tipoConvocatoria, estado, fechaCierre, cierreAutomatico) "
						+ "VALUES ('1', '1', 'ORDINARIA_ENERO', 'CERRADA', '2027-01-26', 0)");
		db.executeUpdate("INSERT INTO Calificacion (matriculaId, cursoId, anioAcademicoId, tipoConvocatoria, nota) "
				+ "VALUES ('1', '1', '1', 'ORDINARIA_ENERO', 4.0)");

		// Nota numérica cuenta como 1 convocatoria consumida
		assertEquals(1, matriculacionModel.comprobarConvocatorias("1", "1"));

		// Nota nula (no presentado) no debe sumar
		db.executeUpdate("INSERT INTO Convocatoria (anioAcademicoId, tipoConvocatoria, fechaInicio, fechaFin) "
				+ "VALUES ('1', 'ORDINARIA_JUNIO', '2027-06-10', '2027-06-25')");
		db.executeUpdate(
				"INSERT INTO Acta (cursoId, anioAcademicoId, tipoConvocatoria, estado, fechaCierre, cierreAutomatico) "
						+ "VALUES ('1', '1', 'ORDINARIA_JUNIO', 'CERRADA', '2027-06-26', 0)");
		db.executeUpdate("INSERT INTO Calificacion (matriculaId, cursoId, anioAcademicoId, tipoConvocatoria, nota) "
				+ "VALUES ('1', '1', '1', 'ORDINARIA_JUNIO', NULL)");

		assertEquals(1, matriculacionModel.comprobarConvocatorias("1", "1"),
				"Las notas nulas no deben computar como convocatoria consumida");
	}

	@Test
	@DisplayName("HU 35235 - Fase 5: testSolicitudCorrectaConTarjeta() - Verifica estado PAGADA y pagoValidado = 1")
	public void testSolicitudCorrectaConTarjeta() {
		// Asignar curso a Grado 2
		db.executeUpdate("INSERT INTO GradoCurso (gradoId, cursoId, anio, itinerario, estado) "
				+ "VALUES ('2', '1', 1, 'General', 'ACTIVO')");

		List<OfertaCursoDTO> asignaturas = new ArrayList<>();
		asignaturas.add(new OfertaCursoDTO("1", "Programación", 1, 6.0, "General"));

		ResumenSolicitudDTO solicitud = new ResumenSolicitudDTO();
		solicitud.setAlumnoId("1");
		solicitud.setGradoId("2");
		solicitud.setAnioAcademicoId("2");
		solicitud.setMetodoPago("TARJETA");
		solicitud.setAsignaturas(asignaturas);

		String matriculaId = matriculacionModel.registrarSolicitudMatricula(solicitud);
		// mismo formato que data.sql (M00001 ... M02100): la carga limpia no tiene ids con M, así que es la primera
		assertEquals("M00001", matriculaId);

		// Verificar en BD que la cabecera Matricula tiene estado SOLICITADA
		List<Object[]> rowsMat = db.executeQueryArray("SELECT estado FROM Matricula WHERE matriculaId = ?",
				matriculaId);
		assertEquals(1, rowsMat.size());
		assertEquals("SOLICITADA", rowsMat.get(0)[0], "La matrícula debe quedar registrada como SOLICITADA");

		// Verificar que MatriculaCurso con tarjeta pasa a PAGADA y pagoValidado = 1
		List<Object[]> rowsCur = db.executeQueryArray(
				"SELECT estado, metodoPago, pagoValidado, importe FROM MatriculaCurso WHERE matriculaId = ? AND cursoId = '1'",
				matriculaId);
		assertEquals(1, rowsCur.size());
		assertEquals("PAGADA", rowsCur.get(0)[0], "Con tarjeta la asignatura pasa a estado PAGADA");
		assertEquals("TARJETA", rowsCur.get(0)[1], "El método de pago registrado debe ser TARJETA");
		assertEquals(1, ((Number) rowsCur.get(0)[2]).intValue(),
				"Con tarjeta el pago queda validado automáticamente (1)");
		assertEquals(120.0, ((Number) rowsCur.get(0)[3]).doubleValue(), 0.01,
				"El importe debe ser 120 € (6 créditos * 20 €)");
	}

	@Test
	@DisplayName("HU 35235 - Fase 5: testSolicitudCorrectaConTransferencia() - Verifica estado SOLICITADA y pagoValidado = 0")
	public void testSolicitudCorrectaConTransferencia() {
		// Asignar curso a Grado 2
		db.executeUpdate("INSERT INTO GradoCurso (gradoId, cursoId, anio, itinerario, estado) "
				+ "VALUES ('2', '1', 1, 'General', 'ACTIVO')");

		List<OfertaCursoDTO> asignaturas = new ArrayList<>();
		asignaturas.add(new OfertaCursoDTO("1", "Programación", 1, 6.0, "General"));

		ResumenSolicitudDTO solicitud = new ResumenSolicitudDTO();
		solicitud.setAlumnoId("1");
		solicitud.setGradoId("2");
		solicitud.setAnioAcademicoId("2");
		solicitud.setMetodoPago("TRANSFERENCIA");
		solicitud.setAsignaturas(asignaturas);

		String matriculaId = matriculacionModel.registrarSolicitudMatricula(solicitud);
		assertNotNull(matriculaId);

		// Verificar que MatriculaCurso queda en estado SOLICITADA con pagoValidado = 0
		List<Object[]> rowsCur = db.executeQueryArray(
				"SELECT estado, metodoPago, pagoValidado, importe FROM MatriculaCurso WHERE matriculaId = ? AND cursoId = '1'",
				matriculaId);
		assertEquals(1, rowsCur.size());
		assertEquals("SOLICITADA", rowsCur.get(0)[0], "Con transferencia la asignatura queda en SOLICITADA");
		assertEquals("TRANSFERENCIA", rowsCur.get(0)[1]);
		assertEquals(0, ((Number) rowsCur.get(0)[2]).intValue(),
				"Con transferencia requiere validación manual por secretaría (0)");
		assertEquals(120.0, ((Number) rowsCur.get(0)[3]).doubleValue(), 0.01);
	}

	@Test
	@DisplayName("HU 35235 - Fase 5: testImpedirSolicitudDuplicada() - Intento de matricularse dos veces en el mismo grado y año")
	public void testImpedirSolicitudDuplicada() {
		// Alumno 1 ya está matriculado en Grado 1 para Año 1
		List<OfertaCursoDTO> asignaturas = new ArrayList<>();
		asignaturas.add(new OfertaCursoDTO("1", "Programación", 1, 6.0, "General"));

		ResumenSolicitudDTO solicitud = new ResumenSolicitudDTO();
		solicitud.setAlumnoId("1");
		solicitud.setGradoId("1");
		solicitud.setAnioAcademicoId("1");
		solicitud.setMetodoPago("TARJETA");
		solicitud.setAsignaturas(asignaturas);

		ValidationException ex = assertThrows(ValidationException.class, () -> {
			matriculacionModel.registrarSolicitudMatricula(solicitud);
		});
		assertTrue(ex.getMessage().contains("Ya existe una matrícula"),
				"Debe indicar que ya existe una matrícula previa para ese grado y año");
	}

	@Test
	@DisplayName("HU 35235 - Fase 5: testRechazoPorLimiteConvocatorias() - Alumno con 6 convocatorias consumidas en una asignatura")
	public void testRechazoPorLimiteConvocatorias() {
		// Asignar Curso 1 también a Grado 2
		db.executeUpdate("INSERT INTO GradoCurso (gradoId, cursoId, anio, itinerario, estado) "
				+ "VALUES ('2', '1', 1, 'General', 'ACTIVO')");

		// Convocatorias del Año 1 (3 convocatorias con notas evaluadas no nulas)
		db.executeUpdate("INSERT INTO Convocatoria (anioAcademicoId, tipoConvocatoria, fechaInicio, fechaFin) "
				+ "VALUES ('1', 'ORDINARIA_ENERO', '2027-01-10', '2027-01-25')");
		db.executeUpdate("INSERT INTO Convocatoria (anioAcademicoId, tipoConvocatoria, fechaInicio, fechaFin) "
				+ "VALUES ('1', 'ORDINARIA_JUNIO', '2027-06-10', '2027-06-25')");
		db.executeUpdate("INSERT INTO Convocatoria (anioAcademicoId, tipoConvocatoria, fechaInicio, fechaFin) "
				+ "VALUES ('1', 'EXTRAORDINARIA_JULIO', '2027-07-01', '2027-07-15')");
		db.executeUpdate(
				"INSERT INTO Acta (cursoId, anioAcademicoId, tipoConvocatoria, estado, fechaCierre, cierreAutomatico) "
						+ "VALUES ('1', '1', 'ORDINARIA_ENERO', 'CERRADA', '2027-01-26', 0)");
		db.executeUpdate(
				"INSERT INTO Acta (cursoId, anioAcademicoId, tipoConvocatoria, estado, fechaCierre, cierreAutomatico) "
						+ "VALUES ('1', '1', 'ORDINARIA_JUNIO', 'CERRADA', '2027-06-26', 0)");
		db.executeUpdate(
				"INSERT INTO Acta (cursoId, anioAcademicoId, tipoConvocatoria, estado, fechaCierre, cierreAutomatico) "
						+ "VALUES ('1', '1', 'EXTRAORDINARIA_JULIO', 'CERRADA', '2027-07-16', 0)");

		db.executeUpdate("INSERT INTO Calificacion (matriculaId, cursoId, anioAcademicoId, tipoConvocatoria, nota) "
				+ "VALUES ('1', '1', '1', 'ORDINARIA_ENERO', 3.0)");
		db.executeUpdate("INSERT INTO Calificacion (matriculaId, cursoId, anioAcademicoId, tipoConvocatoria, nota) "
				+ "VALUES ('1', '1', '1', 'ORDINARIA_JUNIO', 4.0)");
		db.executeUpdate("INSERT INTO Calificacion (matriculaId, cursoId, anioAcademicoId, tipoConvocatoria, nota) "
				+ "VALUES ('1', '1', '1', 'EXTRAORDINARIA_JULIO', 4.5)");

		// Convocatorias del Año 2 (otras 3 convocatorias con notas evaluadas no nulas
		// en matrícula previa M099)
		db.executeUpdate("INSERT INTO Convocatoria (anioAcademicoId, tipoConvocatoria, fechaInicio, fechaFin) "
				+ "VALUES ('2', 'ORDINARIA_ENERO', '2028-01-10', '2028-01-25')");
		db.executeUpdate("INSERT INTO Convocatoria (anioAcademicoId, tipoConvocatoria, fechaInicio, fechaFin) "
				+ "VALUES ('2', 'ORDINARIA_JUNIO', '2028-06-10', '2028-06-25')");
		db.executeUpdate("INSERT INTO Convocatoria (anioAcademicoId, tipoConvocatoria, fechaInicio, fechaFin) "
				+ "VALUES ('2', 'EXTRAORDINARIA_JULIO', '2028-07-01', '2028-07-15')");
		db.executeUpdate(
				"INSERT INTO Acta (cursoId, anioAcademicoId, tipoConvocatoria, estado, fechaCierre, cierreAutomatico) "
						+ "VALUES ('1', '2', 'ORDINARIA_ENERO', 'CERRADA', '2028-01-26', 0)");
		db.executeUpdate(
				"INSERT INTO Acta (cursoId, anioAcademicoId, tipoConvocatoria, estado, fechaCierre, cierreAutomatico) "
						+ "VALUES ('1', '2', 'ORDINARIA_JUNIO', 'CERRADA', '2028-06-26', 0)");
		db.executeUpdate(
				"INSERT INTO Acta (cursoId, anioAcademicoId, tipoConvocatoria, estado, fechaCierre, cierreAutomatico) "
						+ "VALUES ('1', '2', 'EXTRAORDINARIA_JULIO', 'CERRADA', '2028-07-16', 0)");

		db.executeUpdate(
				"INSERT INTO Matricula (matriculaId, alumnoId, gradoId, anioAcademicoId, fechaSolicitud, estado, motivoCancelacion, fechaCancelacion) "
						+ "VALUES ('M099', '1', '1', '2', '2027-09-01', 'FINALIZADA', NULL, NULL)");
		db.executeUpdate(
				"INSERT INTO MatriculaCurso (matriculaId, gradoId, cursoId, estado, motivoCancelacion, fechaCancelacion, importe, metodoPago, fechaPago, pagoValidado) "
						+ "VALUES ('M099', '1', '1', 'FINALIZADA', NULL, NULL, 120, 'TARJETA', '2027-09-01', 1)");

		db.executeUpdate("INSERT INTO Calificacion (matriculaId, cursoId, anioAcademicoId, tipoConvocatoria, nota) "
				+ "VALUES ('M099', '1', '2', 'ORDINARIA_ENERO', 2.0)");
		db.executeUpdate("INSERT INTO Calificacion (matriculaId, cursoId, anioAcademicoId, tipoConvocatoria, nota) "
				+ "VALUES ('M099', '1', '2', 'ORDINARIA_JUNIO', 4.0)");
		db.executeUpdate("INSERT INTO Calificacion (matriculaId, cursoId, anioAcademicoId, tipoConvocatoria, nota) "
				+ "VALUES ('M099', '1', '2', 'EXTRAORDINARIA_JULIO', 3.5)");

		// Total convocatorias consumidas con nota en Curso 1 = 6
		int convs = matriculacionModel.getConvocatoriasConsumidas("1", "1");
		assertEquals(6, convs, "El alumno debe tener exactamente 6 convocatorias consumidas");

		// Intentar solicitar nueva matrícula con la asignatura 1
		List<OfertaCursoDTO> asignaturas = new ArrayList<>();
		asignaturas.add(new OfertaCursoDTO("1", "Programación", 1, 6.0, "General"));

		ResumenSolicitudDTO solicitud = new ResumenSolicitudDTO();
		solicitud.setAlumnoId("1");
		solicitud.setGradoId("2");
		solicitud.setAnioAcademicoId("2");
		solicitud.setMetodoPago("TARJETA");
		solicitud.setAsignaturas(asignaturas);

		ValidationException ex = assertThrows(ValidationException.class, () -> {
			matriculacionModel.registrarSolicitudMatricula(solicitud);
		});
		assertTrue(ex.getMessage().contains("límite de 6 convocatorias") || ex.getMessage().contains("convocatorias"),
				"Debe rechazar la matrícula por alcanzar el límite de 6 convocatorias");
	}

	@Test
	@DisplayName("HU 35235 - Fase 5: testRechazoPorIncumplimientoPermanencia() - Alumno que no superó los mínimos exigidos")
	public void testRechazoPorIncumplimientoPermanencia() {
		// Alumno 1 cursó Año 1 en Grado 1 (Matrícula 1 con Curso 1 de 6 cr y Curso 2 de
		// 6 cr).
		// Solo aprueba Curso 1 (6 créditos superados) y suspende Curso 2 (4.0).
		// Como 6 créditos < 12 créditos mínimos anuales, no cumple permanencia.
		db.executeUpdate("INSERT INTO Convocatoria (anioAcademicoId, tipoConvocatoria, fechaInicio, fechaFin) "
				+ "VALUES ('1', 'ORDINARIA_JUNIO', '2027-06-01', '2027-06-20')");
		db.executeUpdate(
				"INSERT INTO Acta (cursoId, anioAcademicoId, tipoConvocatoria, estado, fechaCierre, cierreAutomatico) "
						+ "VALUES ('1', '1', 'ORDINARIA_JUNIO', 'CERRADA', '2027-06-21', 0)");
		db.executeUpdate(
				"INSERT INTO Acta (cursoId, anioAcademicoId, tipoConvocatoria, estado, fechaCierre, cierreAutomatico) "
						+ "VALUES ('2', '1', 'ORDINARIA_JUNIO', 'CERRADA', '2027-06-21', 0)");

		db.executeUpdate("INSERT INTO Calificacion (matriculaId, cursoId, anioAcademicoId, tipoConvocatoria, nota) "
				+ "VALUES ('1', '1', '1', 'ORDINARIA_JUNIO', 6.0)");
		db.executeUpdate("INSERT INTO Calificacion (matriculaId, cursoId, anioAcademicoId, tipoConvocatoria, nota) "
				+ "VALUES ('1', '2', '1', 'ORDINARIA_JUNIO', 4.0)");

		// Verificar que el modelo detecta el incumplimiento del hito anual
		HitosPermanenciaDTO hitos = matriculacionModel.getHitosPermanencia("1", "1");
		assertEquals(6, hitos.getCreditosAnualesSuperados(), "Solo superó 6 créditos en el año");
		assertFalse(hitos.isCumpleHitoAnual(), "No debe cumplir el hito anual de permanencia (exigidos 12 créditos)");
		assertFalse(matriculacionModel.comprobarCumplimientoPermanencia("1", "1"));

		// Intentar matricularse en el Año 2 en el mismo Grado 1
		List<OfertaCursoDTO> asignaturas = new ArrayList<>();
		asignaturas.add(new OfertaCursoDTO("2", "Bases de Datos", 1, 6.0, "General"));

		ResumenSolicitudDTO solicitud = new ResumenSolicitudDTO();
		solicitud.setAlumnoId("1");
		solicitud.setGradoId("1");
		solicitud.setAnioAcademicoId("2");
		solicitud.setMetodoPago("TARJETA");
		solicitud.setAsignaturas(asignaturas);

		ValidationException ex = assertThrows(ValidationException.class, () -> {
			matriculacionModel.registrarSolicitudMatricula(solicitud);
		});
		assertTrue(ex.getMessage().contains("permanencia"),
				"Debe rechazar la solicitud indicando incumplimiento del régimen de permanencia");
	}

	@Test
	@DisplayName("HU 35235 - Fase 2: Rechazar solicitud de estudiante con impago")
	public void testRechazarSolicitudEstudianteConImpago() {
		// Alumno 2 tiene historial de IMPAGO
		List<OfertaCursoDTO> asignaturas = new ArrayList<>();
		asignaturas.add(new OfertaCursoDTO("1", "Programación", 1, 6.0, "General"));

		ResumenSolicitudDTO solicitud = new ResumenSolicitudDTO();
		solicitud.setAlumnoId("2");
		solicitud.setGradoId("1");
		solicitud.setAnioAcademicoId("2");
		solicitud.setMetodoPago("TARJETA");
		solicitud.setAsignaturas(asignaturas);

		ValidationException ex = assertThrows(ValidationException.class, () -> {
			matriculacionModel.registrarSolicitudMatricula(solicitud);
		});
		assertTrue(ex.getMessage().contains("pagos pendientes") || ex.getMessage().contains("deudas"));
	}

	@Test
	@DisplayName("HU 35235: si la BD falla, la solicitud se rechaza en vez de darse por válida")
	public void testSolicitudRechazadaSiFallaLaBaseDeDatos() {
		// BD en memoria sin tablas: todas las consultas de validación fallan.
		// Antes cada comprobación atrapaba el error y devolvía "todo correcto".
		MatriculacionModel sinTablas = new MatriculacionModel(new Database("jdbc:sqlite::memory:"));

		assertThrows(UnexpectedException.class,
				() -> sinTablas.validarSolicitudMatricula("1", "1", "1", List.of("1")));
	}
}
