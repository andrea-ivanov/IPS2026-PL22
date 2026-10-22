package model.grado;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import model.fixtures.BaseModelTest;
import repository.GradoRepository;
import util.exception.ValidationException;

/**
 * Pruebas unitarias para el alta y la baja de Grados (HU 35223).
 * Año '1': el grado 1 tiene matrículas activas. Año '2': sin matrículas.
 */
public class GradoModelGestionTest extends BaseModelTest {
	private GradoModel gradoModel;

	@BeforeEach
	public void initModel() {
		this.gradoModel = new GradoModel(db);
	}

	// =========================================================================
	// 1. AÑADIR GRADO
	// =========================================================================
	@Nested
	@DisplayName("1. Añadir grado")
	public class CrearGrado {

		@Test
		@DisplayName("TG-01 Happy path: crea el grado VIGENTE con la vigencia del año académico")
		public void testCrearGrado_HappyPath() {
			String gradoId = gradoModel.crearGrado("Grado Nuevo", "Facultad X", "Ciencias", "1");

			assertEquals("G001", gradoId);
			List<Object[]> rows = db.executeQueryArray(
					"SELECT nombre, facultad, ramaConocimiento, estado, fechaInicioVigencia FROM Grado WHERE gradoId = ?", gradoId);
			assertEquals(1, rows.size(), "Debe existir el grado creado");
			Object[] row = rows.get(0);
			assertAll("Campos del grado creado",
					() -> assertEquals("Grado Nuevo", row[0]),
					() -> assertEquals("Facultad X", row[1]),
					() -> assertEquals("Ciencias", row[2]),
					() -> assertEquals("VIGENTE", row[3]),
					() -> assertEquals("2026-09-01", String.valueOf(row[4])),
					() -> assertEquals(3, contarFilas("Grado")));
		}

		@ParameterizedTest(name = "nombre = ''{0}''")
		@NullAndEmptySource
		@ValueSource(strings = { "   ", "\t" })
		@DisplayName("TG-02 Falta el nombre: lanza ValidationException y no inserta")
		public void testCrearGrado_SinNombre(String valor) {
			assertThrows(ValidationException.class,
					() -> gradoModel.crearGrado(valor, "Facultad X", "Ciencias", "1"));
			assertEquals(2, contarFilas("Grado"));
		}

		@ParameterizedTest(name = "facultad = ''{0}''")
		@NullAndEmptySource
		@ValueSource(strings = { "   ", "\t" })
		@DisplayName("TG-02 Falta la facultad: lanza ValidationException y no inserta")
		public void testCrearGrado_SinFacultad(String valor) {
			assertThrows(ValidationException.class,
					() -> gradoModel.crearGrado("Grado Nuevo", valor, "Ciencias", "1"));
			assertEquals(2, contarFilas("Grado"));
		}

		@ParameterizedTest(name = "rama = ''{0}''")
		@NullAndEmptySource
		@ValueSource(strings = { "   ", "\t" })
		@DisplayName("TG-02 Falta la rama de conocimiento: lanza ValidationException y no inserta")
		public void testCrearGrado_SinRama(String valor) {
			assertThrows(ValidationException.class,
					() -> gradoModel.crearGrado("Grado Nuevo", "Facultad X", valor, "1"));
			assertEquals(2, contarFilas("Grado"));
		}

		@ParameterizedTest(name = "año académico = ''{0}''")
		@NullAndEmptySource
		@ValueSource(strings = { "9999" })
		@DisplayName("TG-03 Año académico nulo o inexistente: lanza ValidationException y no inserta")
		public void testCrearGrado_SinAnoAcademico(String anio) {
			assertThrows(ValidationException.class,
					() -> gradoModel.crearGrado("Grado Nuevo", "Facultad X", "Ciencias", anio));
			assertEquals(2, contarFilas("Grado"));
		}

		@Test
		@DisplayName("TG-04 Duplicado de PK: el ID generado no colisiona con uno existente")
		public void testCrearGrado_IdNoColisiona() {
			db.executeUpdate("INSERT INTO Grado (gradoId, nombre, facultad, ramaConocimiento, estado, fechaInicioVigencia) "
					+ "VALUES ('G001', 'Existente', 'Facultad Y', 'Ciencias', 'VIGENTE', '2025-09-01')");

			String gradoId = gradoModel.crearGrado("Grado Nuevo", "Facultad X", "Ciencias", "1");

			assertEquals("G002", gradoId);
			assertEquals("Existente", db.executeQueryArray("SELECT nombre FROM Grado WHERE gradoId = 'G001'").get(0)[0]);
		}

		@Test
		@DisplayName("TG-05 El nombre no es UNIQUE: dos altas iguales crean dos grados distintos")
		public void testCrearGrado_MismoNombreDosVeces() {
			String id1 = gradoModel.crearGrado("Grado Nuevo", "Facultad X", "Ciencias", "1");
			String id2 = gradoModel.crearGrado("Grado Nuevo", "Facultad X", "Ciencias", "1");

			assertNotEquals(id1, id2);
			assertEquals(4, contarFilas("Grado"));
		}
	}

	// =========================================================================
	// 2. ELIMINAR GRADO
	// =========================================================================
	@Nested
	@DisplayName("2. Eliminar grado")
	public class EliminarGrado {

		@Test
		@DisplayName("TE-G01 Comprobación: ningún curso tiene matrícula en el año")
		public void testValidarEliminacion_SinMatriculas() {
			assertDoesNotThrow(() -> gradoModel.validarEliminacion("1", "2"));
		}

		@Test
		@DisplayName("TE-G02 Comprobación: algún curso tiene matrícula en el año")
		public void testValidarEliminacion_ConMatriculas() {
			assertThrows(ValidationException.class, () -> gradoModel.validarEliminacion("1", "1"));
		}

		@Test
		@DisplayName("TE-G03 Happy path: el grado pasa a EN_EXTINCION y todos sus cursos a INACTIVO, sin borrar filas")
		public void testEliminarGrado_HappyPath() {
			gradoModel.eliminarGrado("1", "2");

			assertAll("Estados tras eliminar el grado",
					() -> assertEquals("EN_EXTINCION", estadoGrado("1")),
					() -> assertEquals("INACTIVO", estadoGradoCurso("1", "1")),
					() -> assertEquals("INACTIVO", estadoGradoCurso("1", "2")),
					() -> assertEquals(2, contarFilas("Grado")),
					() -> assertEquals(2, contarFilas("Curso")),
					() -> assertEquals(2, contarFilas("GradoCurso")));
		}

		@Test
		@DisplayName("TE-G04 Grado sin cursos: pasa a EN_EXTINCION")
		public void testEliminarGrado_SinCursos() {
			gradoModel.eliminarGrado("2", "2");

			assertEquals("EN_EXTINCION", estadoGrado("2"));
		}

		@Test
		@DisplayName("TE-G05 Bloqueo: con matrículas en el año no se hace nada")
		public void testEliminarGrado_ConMatriculas_NoHaceNada() {
			assertThrows(ValidationException.class, () -> gradoModel.eliminarGrado("1", "1"));

			assertSinCambios();
		}

		@Test
		@DisplayName("TE-G06 Bloqueo si solo un curso tiene matrícula: ningún curso cambia")
		public void testEliminarGrado_UnSoloCursoConMatricula_NoHaceNada() {
			db.executeBatch(new String[] {
					"INSERT INTO Matricula (matriculaId, alumnoId, gradoId, anioAcademicoId, fechaSolicitud, estado, motivoCancelacion, fechaCancelacion) "
							+ "VALUES ('3', '1', '1', '2', '2027-09-01', 'ACTIVA', NULL, NULL)",
					"INSERT INTO MatriculaCurso (matriculaId, gradoId, cursoId, estado, motivoCancelacion, fechaCancelacion, importe, metodoPago, fechaPago, pagoValidado) "
							+ "VALUES ('3', '1', '1', 'ACTIVA', NULL, NULL, 120, 'TARJETA', '2027-09-01', 1)"
			});

			assertThrows(ValidationException.class, () -> gradoModel.eliminarGrado("1", "2"));

			assertSinCambios();
		}

		@Test
		@DisplayName("TE-G07 Las matrículas canceladas no cuentan: se elimina")
		public void testEliminarGrado_SoloMatriculasCanceladas() {
			db.executeBatch(new String[] {
					"INSERT INTO Matricula (matriculaId, alumnoId, gradoId, anioAcademicoId, fechaSolicitud, estado, motivoCancelacion, fechaCancelacion) "
							+ "VALUES ('3', '1', '1', '2', '2027-09-01', 'CANCELADA', 'IMPAGO', '2027-09-10')",
					"INSERT INTO MatriculaCurso (matriculaId, gradoId, cursoId, estado, motivoCancelacion, fechaCancelacion, importe, metodoPago, fechaPago, pagoValidado) "
							+ "VALUES ('3', '1', '1', 'CANCELADA', 'IMPAGO', '2027-09-10', 120, NULL, NULL, 0)"
			});

			gradoModel.eliminarGrado("1", "2");

			assertEquals("EN_EXTINCION", estadoGrado("1"));
			assertEquals("INACTIVO", estadoGradoCurso("1", "1"));
			assertEquals("INACTIVO", estadoGradoCurso("1", "2"));
		}

		@Test
		@DisplayName("TE-G08 Curso compartido: sigue ACTIVO en el otro grado")
		public void testEliminarGrado_CursoCompartido() {
			db.executeUpdate("INSERT INTO GradoCurso (gradoId, cursoId, anio, itinerario, estado) VALUES ('2', '1', 1, 'General', 'ACTIVO')");

			gradoModel.eliminarGrado("1", "2");

			assertEquals("INACTIVO", estadoGradoCurso("1", "1"));
			assertEquals("ACTIVO", estadoGradoCurso("2", "1"));
		}

		@Test
		@DisplayName("TE-G09 Grado PENDIENTE_RENOVACION: se puede eliminar")
		public void testEliminarGrado_PendienteRenovacion() {
			db.executeUpdate("UPDATE Grado SET estado = 'PENDIENTE_RENOVACION' WHERE gradoId = '1'");

			gradoModel.eliminarGrado("1", "2");

			assertEquals("EN_EXTINCION", estadoGrado("1"));
			assertEquals("INACTIVO", estadoGradoCurso("1", "1"));
			assertEquals("INACTIVO", estadoGradoCurso("1", "2"));
		}

		@ParameterizedTest(name = "estado = {0}")
		@ValueSource(strings = { "EN_EXTINCION", "EXTINTO" })
		@DisplayName("TE-G10/TE-G11 Grado EN_EXTINCION o EXTINTO: lanza ValidationException y no hace nada")
		public void testEliminarGrado_EstadoNoPermitido(String estado) {
			db.executeUpdate("UPDATE Grado SET estado = ? WHERE gradoId = '1'", estado);

			assertThrows(ValidationException.class, () -> gradoModel.validarEliminacion("1", "2"));
			assertThrows(ValidationException.class, () -> gradoModel.eliminarGrado("1", "2"));

			assertEquals(estado, estadoGrado("1"));
			assertEquals("ACTIVO", estadoGradoCurso("1", "1"));
			assertEquals("ACTIVO", estadoGradoCurso("1", "2"));
		}

		@Test
		@DisplayName("TE-G12 Sin año académico actual: lanza ValidationException y no hace nada")
		public void testEliminarGrado_SinAnoAcademico() {
			assertThrows(ValidationException.class, () -> gradoModel.validarEliminacion("1", null));
			assertThrows(ValidationException.class, () -> gradoModel.eliminarGrado("1", null));

			assertSinCambios();
		}

		private void assertSinCambios() {
			assertAll("Sin cambios en el grado 1 ni en sus cursos",
					() -> assertEquals("VIGENTE", estadoGrado("1")),
					() -> assertEquals("ACTIVO", estadoGradoCurso("1", "1")),
					() -> assertEquals("ACTIVO", estadoGradoCurso("1", "2")));
		}
	}

	// =========================================================================
	// 3. ESTADO DEL GRADO EN JAVA (F-3)
	// =========================================================================
	@Nested
	@DisplayName("3. Mapeo del estado del grado")
	public class MapeoEstado {

		@Test
		@DisplayName("TF-06 getTodosLosGrados muestra EN_EXTINCION para un grado EXTINTO")
		public void testGetTodosLosGrados_ExtintoComoEnExtincion() {
			db.executeUpdate("UPDATE Grado SET estado = 'EXTINTO' WHERE gradoId = '2'");

			String estado = new GradoRepository(db).getTodosLosGrados().stream()
					.filter(g -> "2".equals(g.getId()))
					.findFirst().orElseThrow().getEstado();

			assertEquals("EN_EXTINCION", estado);
		}
	}

	private int contarFilas(String tabla) {
		return ((Number) db.executeQueryArray("SELECT COUNT(*) FROM " + tabla).get(0)[0]).intValue();
	}

	private String estadoGrado(String gradoId) {
		return (String) db.executeQueryArray("SELECT estado FROM Grado WHERE gradoId = ?", gradoId).get(0)[0];
	}

	private String estadoGradoCurso(String gradoId, String cursoId) {
		return (String) db.executeQueryArray("SELECT estado FROM GradoCurso WHERE gradoId = ? AND cursoId = ?",
				gradoId, cursoId).get(0)[0];
	}
}
