package model.matriculacion;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import model.fixtures.BaseModelTest;
import util.exception.ValidationException;

/**
 * Pruebas unitarias para la historia de usuario HU 35235:
 * Solicitud de matrícula en grados y cursos ofertados (Fases 1 y 2).
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
		db.executeUpdate("INSERT INTO Acta (cursoId, anioAcademicoId, tipoConvocatoria, estado, fechaCierre, cierreAutomatico) "
				+ "VALUES ('1', '1', 'ORDINARIA_ENERO', 'CERRADA', '2027-01-26', 0)");
		db.executeUpdate("INSERT INTO Calificacion (matriculaId, cursoId, anioAcademicoId, tipoConvocatoria, nota) "
				+ "VALUES ('1', '1', '1', 'ORDINARIA_ENERO', 4.0)");

		// Nota numérica cuenta como 1 convocatoria consumida
		assertEquals(1, matriculacionModel.comprobarConvocatorias("1", "1"));

		// Nota nula (no presentado) no debe sumar
		db.executeUpdate("INSERT INTO Convocatoria (anioAcademicoId, tipoConvocatoria, fechaInicio, fechaFin) "
				+ "VALUES ('1', 'ORDINARIA_JUNIO', '2027-06-10', '2027-06-25')");
		db.executeUpdate("INSERT INTO Acta (cursoId, anioAcademicoId, tipoConvocatoria, estado, fechaCierre, cierreAutomatico) "
				+ "VALUES ('1', '1', 'ORDINARIA_JUNIO', 'CERRADA', '2027-06-26', 0)");
		db.executeUpdate("INSERT INTO Calificacion (matriculaId, cursoId, anioAcademicoId, tipoConvocatoria, nota) "
				+ "VALUES ('1', '1', '1', 'ORDINARIA_JUNIO', NULL)");

		assertEquals(1, matriculacionModel.comprobarConvocatorias("1", "1"),
				"Las notas nulas no deben computar como convocatoria consumida");
	}

	@Test
	@DisplayName("HU 35235 - Fase 2: Registrar solicitud de matrícula exitosa con pago por TARJETA")
	public void testRegistrarSolicitudMatricula_Tarjeta() {
		// Matricular Alumno 1 en Grado 2 para el año 2 (sin duplicidad)
		// Asignar cursos a Grado 2
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
		assertNotNull(matriculaId, "Debe generar y retornar un ID de matrícula");

		// Verificar en BD que Matricula tiene estado SOLICITADA
		List<Object[]> rowsMat = db.executeQueryArray("SELECT estado FROM Matricula WHERE matriculaId = ?", matriculaId);
		assertEquals(1, rowsMat.size());
		assertEquals("SOLICITADA", rowsMat.get(0)[0], "La matrícula debe quedar registrada como SOLICITADA");

		// Verificar que MatriculaCurso con tarjeta queda PAGADA y pagoValidado = 1
		List<Object[]> rowsCur = db.executeQueryArray("SELECT estado, metodoPago, pagoValidado, importe FROM MatriculaCurso WHERE matriculaId = ? AND cursoId = '1'", matriculaId);
		assertEquals(1, rowsCur.size());
		assertEquals("PAGADA", rowsCur.get(0)[0], "Con tarjeta la asignatura pasa a PAGADA");
		assertEquals("TARJETA", rowsCur.get(0)[1]);
		assertEquals(1, ((Number) rowsCur.get(0)[2]).intValue(), "Con tarjeta el pago está validado (1)");
		assertEquals(120.0, ((Number) rowsCur.get(0)[3]).doubleValue(), 0.01);
	}

	@Test
	@DisplayName("HU 35235 - Fase 2: Registrar solicitud de matrícula con pago por TRANSFERENCIA (pendiente de validación)")
	public void testRegistrarSolicitudMatricula_Transferencia() {
		// Asignar cursos a Grado 2
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

		// Verificar que MatriculaCurso queda SOLICITADA con pagoValidado = 0
		List<Object[]> rowsCur = db.executeQueryArray("SELECT estado, metodoPago, pagoValidado FROM MatriculaCurso WHERE matriculaId = ? AND cursoId = '1'", matriculaId);
		assertEquals(1, rowsCur.size());
		assertEquals("SOLICITADA", rowsCur.get(0)[0], "Con transferencia la asignatura queda SOLICITADA");
		assertEquals("TRANSFERENCIA", rowsCur.get(0)[1]);
		assertEquals(0, ((Number) rowsCur.get(0)[2]).intValue(), "Con transferencia requiere validación manual (0)");
	}

	@Test
	@DisplayName("HU 35235 - Fase 2: Rechazar solicitud duplicada")
	public void testRechazarSolicitudDuplicada() {
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
		assertTrue(ex.getMessage().contains("Ya existe una matrícula"));
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
}
