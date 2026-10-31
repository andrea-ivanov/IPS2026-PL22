package model.grado;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import util.Database;

/**
 * Pruebas de la HU 36411: métricas de rendimiento de los grados para el revisor externo (MetricasGradoModel).
 * Cada prueba parte de sus propios datos (loadCleanDatabase), independientes de data.sql,
 * con fecha de referencia 2026-10-15 (ya ha terminado el periodo de matrícula de 2026-2027).
 * Los valores esperados están calculados a mano a partir de estos datos.
 */
public class MetricasGradoModelTest {
	private static final String HOY = "2026-10-15";
	// antes del fin del periodo de matrícula de 2026-2027 (2026-09-30)
	private static final String HOY_EN_MATRICULACION = "2026-09-15";

	private static Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private MetricasGradoModel metricasModel;

	@BeforeEach
	public void setUp() {
		db.createDatabase(false);
		loadCleanDatabase(db);
		metricasModel = new MetricasGradoModel(db);
	}

	/**
	 * Datos de prueba (todas las asignaturas de 6 créditos, sin itinerarios):
	 * - G001 Biologia (Facultad de Ciencias, Ciencias, VIGENTE): C001 y C002, total 12 cr
	 * - G004 Biologia (Facultad de Veterinaria, Ciencias de la Salud, VIGENTE): C005 y C001 (compartida con G001)
	 * - G002 Derecho (Ciencias Sociales y Juridicas, EN_EXTINCION): C003
	 * - G003 Fisica (EXTINTO): C004, con una matrícula válida que no debe aparecer
	 * - G005 Quimica (VIGENTE): C006, solo con una matrícula CANCELADA
	 * - G001 2024-2025: AL01 (aprueba), AL06 (no vuelve), AL11 (supera los 12 cr: titulado), AL12 (NP; falta en
	 *   2025-2026 y vuelve en 2026-2027), AL04 (en 2025-2026 su matrícula se cancela), AL13 (se pasa a G004,
	 *   que comparte C001) y AL14 (se pasa a G002, que no comparte nada)
	 * - G001 2025-2026: AL01, AL02 (suspende C001 en enero y aprueba en junio), AL03 (C002 cancelada con un 8),
	 *   AL04 CANCELADA y AL09 SOLICITADA
	 * - G001 2026-2027: AL01 (aprueba C002 en enero), AL02 y AL12 sin notas
	 * - G004 2025-2026: AL08 y AL13
	 * - G002: AL05 en 2023-2024 y 2024-2025 (no vuelve), AL14 en 2025-2026 y AL10 en 2026-2027 sin notas
	 */
	public static void loadCleanDatabase(Database db) {
		db.executeBatch(new String[] {
				"DELETE FROM Calificacion", "DELETE FROM MatriculaGrupo", "DELETE FROM MatriculaCurso",
				"DELETE FROM Matricula", "DELETE FROM Acta", "DELETE FROM Convocatoria", "DELETE FROM Imparte",
				"DELETE FROM Grupo", "DELETE FROM PropuestaCurso", "DELETE FROM GradoCurso", "DELETE FROM Curso",
				"DELETE FROM Grado", "DELETE FROM AnioAcademico", "DELETE FROM Profesor", "DELETE FROM Alumno",
				"INSERT INTO AnioAcademico VALUES ('2023-2024','2023-09-01','2024-07-31','2023-07-01','2023-09-30')",
				"INSERT INTO AnioAcademico VALUES ('2024-2025','2024-09-01','2025-07-31','2024-07-01','2024-09-30')",
				"INSERT INTO AnioAcademico VALUES ('2025-2026','2025-09-01','2026-07-31','2025-07-01','2025-09-30')",
				"INSERT INTO AnioAcademico VALUES ('2026-2027','2026-09-01','2027-07-31','2026-07-01','2026-09-30')",
				"INSERT INTO Convocatoria VALUES ('2023-2024','ORDINARIA_JUNIO','2024-05-25','2024-06-15')",
				"INSERT INTO Convocatoria VALUES ('2024-2025','ORDINARIA_JUNIO','2025-05-25','2025-06-15')",
				"INSERT INTO Convocatoria VALUES ('2025-2026','ORDINARIA_ENERO','2026-01-08','2026-01-31')",
				"INSERT INTO Convocatoria VALUES ('2025-2026','ORDINARIA_JUNIO','2026-05-25','2026-06-15')",
				"INSERT INTO Convocatoria VALUES ('2026-2027','ORDINARIA_ENERO','2027-01-08','2027-01-31')",
				"INSERT INTO Grado VALUES ('G001','Biologia','Facultad de Ciencias','Ciencias','VIGENTE','2023-09-01')",
				"INSERT INTO Grado VALUES ('G004','Biologia','Facultad de Veterinaria','Ciencias de la Salud','VIGENTE','2023-09-01')",
				"INSERT INTO Grado VALUES ('G002','Derecho','Facultad de Derecho','Ciencias Sociales y Juridicas','EN_EXTINCION','2022-09-01')",
				"INSERT INTO Grado VALUES ('G003','Fisica','Facultad de Ciencias','Ciencias','EXTINTO','2020-09-01')",
				"INSERT INTO Grado VALUES ('G005','Quimica','Facultad de Quimica','Ciencias','VIGENTE','2023-09-01')",
				"INSERT INTO Curso VALUES ('C001',NULL,'Genetica',6)",
				"INSERT INTO Curso VALUES ('C002',NULL,'Botanica',6)",
				"INSERT INTO Curso VALUES ('C003',NULL,'Derecho Civil',6)",
				"INSERT INTO Curso VALUES ('C004',NULL,'Mecanica',6)",
				"INSERT INTO Curso VALUES ('C005',NULL,'Anatomia Animal',6)",
				"INSERT INTO Curso VALUES ('C006',NULL,'Quimica Organica',6)",
				"INSERT INTO GradoCurso VALUES ('G001','C001',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G001','C002',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G004','C005',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G004','C001',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G002','C003',1,NULL,'ACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G003','C004',1,NULL,'INACTIVO')",
				"INSERT INTO GradoCurso VALUES ('G005','C006',1,NULL,'ACTIVO')",
				"INSERT INTO Acta VALUES ('C003','2023-2024','ORDINARIA_JUNIO','CERRADA','2024-06-20',0)",
				"INSERT INTO Acta VALUES ('C001','2024-2025','ORDINARIA_JUNIO','CERRADA','2025-06-20',0)",
				"INSERT INTO Acta VALUES ('C002','2024-2025','ORDINARIA_JUNIO','CERRADA','2025-06-20',0)",
				"INSERT INTO Acta VALUES ('C003','2024-2025','ORDINARIA_JUNIO','CERRADA','2025-06-20',0)",
				"INSERT INTO Acta VALUES ('C004','2024-2025','ORDINARIA_JUNIO','CERRADA','2025-06-20',0)",
				"INSERT INTO Acta VALUES ('C001','2025-2026','ORDINARIA_ENERO','CERRADA','2026-02-05',0)",
				"INSERT INTO Acta VALUES ('C002','2025-2026','ORDINARIA_ENERO','CERRADA','2026-02-05',0)",
				"INSERT INTO Acta VALUES ('C001','2025-2026','ORDINARIA_JUNIO','CERRADA','2026-06-20',0)",
				"INSERT INTO Acta VALUES ('C002','2025-2026','ORDINARIA_JUNIO','CERRADA','2026-06-20',0)",
				"INSERT INTO Acta VALUES ('C003','2025-2026','ORDINARIA_JUNIO','CERRADA','2026-06-20',0)",
				"INSERT INTO Acta VALUES ('C005','2025-2026','ORDINARIA_JUNIO','CERRADA','2026-06-20',0)",
				"INSERT INTO Acta VALUES ('C002','2026-2027','ORDINARIA_ENERO','ABIERTA',NULL,0)",
				"INSERT INTO Alumno VALUES ('AL01','30000001A','Ana','Alonso','al01@uni.es')",
				"INSERT INTO Alumno VALUES ('AL02','30000002B','Blas','Barrio','al02@uni.es')",
				"INSERT INTO Alumno VALUES ('AL03','30000003C','Celia','Cano','al03@uni.es')",
				"INSERT INTO Alumno VALUES ('AL04','30000004D','Dario','Diez','al04@uni.es')",
				"INSERT INTO Alumno VALUES ('AL05','30000005E','Eva','Escobar','al05@uni.es')",
				"INSERT INTO Alumno VALUES ('AL06','30000006F','Fede','Fuentes','al06@uni.es')",
				"INSERT INTO Alumno VALUES ('AL07','30000007G','Gloria','Gil','al07@uni.es')",
				"INSERT INTO Alumno VALUES ('AL08','30000008H','Hector','Hidalgo','al08@uni.es')",
				"INSERT INTO Alumno VALUES ('AL09','30000009I','Ines','Iglesias','al09@uni.es')",
				"INSERT INTO Alumno VALUES ('AL10','30000010J','Jaime','Jimenez','al10@uni.es')",
				"INSERT INTO Alumno VALUES ('AL11','30000011K','Lara','Lopez','al11@uni.es')",
				"INSERT INTO Alumno VALUES ('AL12','30000012L','Mario','Moreno','al12@uni.es')",
				"INSERT INTO Alumno VALUES ('AL13','30000013M','Nora','Navarro','al13@uni.es')",
				"INSERT INTO Alumno VALUES ('AL14','30000014N','Oscar','Ortiz','al14@uni.es')",
				"INSERT INTO Alumno VALUES ('AL15','30000015O','Paula','Prieto','al15@uni.es')",
				// G001 2024-2025: 7 matriculados
				"INSERT INTO Matricula VALUES ('M01','AL01','G001','2024-2025','2024-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M02','AL06','G001','2024-2025','2024-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M03','AL11','G001','2024-2025','2024-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M04','AL12','G001','2024-2025','2024-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M05','AL04','G001','2024-2025','2024-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M06','AL13','G001','2024-2025','2024-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M07','AL14','G001','2024-2025','2024-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO MatriculaCurso VALUES ('M01','G001','C001','FINALIZADA',NULL,NULL,120,'TARJETA','2024-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M02','G001','C001','FINALIZADA',NULL,NULL,120,'TARJETA','2024-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M03','G001','C001','FINALIZADA',NULL,NULL,120,'TARJETA','2024-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M03','G001','C002','FINALIZADA',NULL,NULL,120,'TARJETA','2024-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M04','G001','C001','FINALIZADA',NULL,NULL,120,'TARJETA','2024-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M05','G001','C001','FINALIZADA',NULL,NULL,120,'TARJETA','2024-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M06','G001','C001','FINALIZADA',NULL,NULL,120,'TARJETA','2024-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M07','G001','C001','FINALIZADA',NULL,NULL,120,'TARJETA','2024-07-10',1)",
				"INSERT INTO Calificacion VALUES ('M01','C001','2024-2025','ORDINARIA_JUNIO',5)",
				"INSERT INTO Calificacion VALUES ('M02','C001','2024-2025','ORDINARIA_JUNIO',3)",
				"INSERT INTO Calificacion VALUES ('M03','C001','2024-2025','ORDINARIA_JUNIO',6)",
				"INSERT INTO Calificacion VALUES ('M03','C002','2024-2025','ORDINARIA_JUNIO',7)",
				"INSERT INTO Calificacion VALUES ('M04','C001','2024-2025','ORDINARIA_JUNIO',NULL)",
				"INSERT INTO Calificacion VALUES ('M05','C001','2024-2025','ORDINARIA_JUNIO',4)",
				"INSERT INTO Calificacion VALUES ('M06','C001','2024-2025','ORDINARIA_JUNIO',2)",
				"INSERT INTO Calificacion VALUES ('M07','C001','2024-2025','ORDINARIA_JUNIO',1)",
				// G001 2025-2026: 3 matriculados (fuera la CANCELADA y la SOLICITADA)
				"INSERT INTO Matricula VALUES ('M08','AL01','G001','2025-2026','2025-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M09','AL02','G001','2025-2026','2025-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M10','AL03','G001','2025-2026','2025-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M11','AL04','G001','2025-2026','2025-07-10','CANCELADA','FUERZA_MAYOR','2025-10-01')",
				"INSERT INTO Matricula VALUES ('M12','AL09','G001','2025-2026','2025-07-10','SOLICITADA',NULL,NULL)",
				"INSERT INTO MatriculaCurso VALUES ('M08','G001','C001','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M08','G001','C002','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M09','G001','C001','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M09','G001','C002','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M10','G001','C001','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M10','G001','C002','CANCELADA','FUERZA_MAYOR','2026-02-10',120,'TARJETA','2025-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M11','G001','C001','CANCELADA','FUERZA_MAYOR','2025-10-01',120,'TARJETA','2025-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M12','G001','C001','SOLICITADA',NULL,NULL,120,NULL,NULL,0)",
				"INSERT INTO Calificacion VALUES ('M08','C001','2025-2026','ORDINARIA_JUNIO',7)",
				"INSERT INTO Calificacion VALUES ('M08','C002','2025-2026','ORDINARIA_JUNIO',3)",
				"INSERT INTO Calificacion VALUES ('M09','C001','2025-2026','ORDINARIA_ENERO',4)",
				"INSERT INTO Calificacion VALUES ('M09','C001','2025-2026','ORDINARIA_JUNIO',6)",
				"INSERT INTO Calificacion VALUES ('M09','C002','2025-2026','ORDINARIA_JUNIO',NULL)",
				"INSERT INTO Calificacion VALUES ('M10','C001','2025-2026','ORDINARIA_JUNIO',NULL)",
				"INSERT INTO Calificacion VALUES ('M10','C002','2025-2026','ORDINARIA_ENERO',8)",
				// G001 2026-2027 (año en curso): 3 matriculados
				"INSERT INTO Matricula VALUES ('M13','AL01','G001','2026-2027','2026-07-10','ACTIVA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M14','AL02','G001','2026-2027','2026-07-10','ACTIVA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M15','AL12','G001','2026-2027','2026-07-10','ACTIVA',NULL,NULL)",
				"INSERT INTO MatriculaCurso VALUES ('M13','G001','C002','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M14','G001','C002','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M15','G001','C001','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M15','G001','C002','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-10',1)",
				"INSERT INTO Calificacion VALUES ('M13','C002','2026-2027','ORDINARIA_ENERO',6)",
				// G004 2025-2026
				"INSERT INTO Matricula VALUES ('M16','AL08','G004','2025-2026','2025-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M17','AL13','G004','2025-2026','2025-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO MatriculaCurso VALUES ('M16','G004','C005','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M17','G004','C001','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-10',1)",
				"INSERT INTO Calificacion VALUES ('M16','C005','2025-2026','ORDINARIA_JUNIO',9)",
				"INSERT INTO Calificacion VALUES ('M17','C001','2025-2026','ORDINARIA_JUNIO',5)",
				// G002
				"INSERT INTO Matricula VALUES ('M18','AL05','G002','2023-2024','2023-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M19','AL05','G002','2024-2025','2024-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M20','AL14','G002','2025-2026','2025-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO Matricula VALUES ('M21','AL10','G002','2026-2027','2026-07-10','ACTIVA',NULL,NULL)",
				"INSERT INTO MatriculaCurso VALUES ('M18','G002','C003','FINALIZADA',NULL,NULL,120,'TARJETA','2023-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M19','G002','C003','FINALIZADA',NULL,NULL,120,'TARJETA','2024-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M20','G002','C003','FINALIZADA',NULL,NULL,120,'TARJETA','2025-07-10',1)",
				"INSERT INTO MatriculaCurso VALUES ('M21','G002','C003','ACTIVA',NULL,NULL,120,'TARJETA','2026-07-10',1)",
				"INSERT INTO Calificacion VALUES ('M18','C003','2023-2024','ORDINARIA_JUNIO',2)",
				"INSERT INTO Calificacion VALUES ('M19','C003','2024-2025','ORDINARIA_JUNIO',3)",
				"INSERT INTO Calificacion VALUES ('M20','C003','2025-2026','ORDINARIA_JUNIO',5)",
				// G003 (EXTINTO) y G005 (solo una matrícula CANCELADA)
				"INSERT INTO Matricula VALUES ('M22','AL07','G003','2024-2025','2024-07-10','FINALIZADA',NULL,NULL)",
				"INSERT INTO MatriculaCurso VALUES ('M22','G003','C004','FINALIZADA',NULL,NULL,120,'TARJETA','2024-07-10',1)",
				"INSERT INTO Calificacion VALUES ('M22','C004','2024-2025','ORDINARIA_JUNIO',6)",
				"INSERT INTO Matricula VALUES ('M23','AL15','G005','2025-2026','2025-07-10','CANCELADA','IMPAGO','2025-10-01')",
				"INSERT INTO MatriculaCurso VALUES ('M23','G005','C006','CANCELADA','IMPAGO','2025-10-01',120,NULL,NULL,0)"
		});
	}

	// =========================================================================
	// 1. MÉTRICAS DE UN GRADO EN UN AÑO
	// =========================================================================
	@Nested
	@DisplayName("1. Métricas de un grado en un año académico")
	public class MetricasDeUnPar {

		@Test
		@DisplayName("TM-01 Biologia en 2025-2026: un DTO por cada grado con ese nombre, con sus métricas")
		public void testMetricasDeUnPar() {
			List<GradoAnioRevisorDisplayDTO> lista = metricasModel.getMetricasGrados(null, "Biologia", "2025-2026", HOY);

			assertEquals(List.of("G001 2025-2026", "G004 2025-2026"), claves(lista));
			// éxito: presentados 18 cr (AL01 C001 y C002, AL02 C001), superados 12 cr (AL01 C001, AL02 C001)
			assertFila(lista.get(0), "G001", "Biologia", "Ciencias", "2025-2026", 66.7, null, 2, 3);
		}

		@Test
		@DisplayName("TM-02 Una asignatura aprobada en junio tras suspender en enero cuenta una vez; la cancelada con nota no cuenta")
		public void testRepeticionYAsignaturaCancelada() {
			GradoAnioRevisorDisplayDTO fila = getFila(todos(), "G001", "2025-2026");

			assertAll(
					() -> assertEquals(2, fila.getAprobados()),
					() -> assertEquals(66.7, fila.getTasaExito()));
		}

		@Test
		@DisplayName("TM-03 Solo cuentan como matriculados las matrículas ACTIVA o FINALIZADA")
		public void testEstadosDeLaMatricula() {
			assertEquals(3, getFila(todos(), "G001", "2025-2026").getMatriculados());
		}

		@Test
		@DisplayName("TM-04 Una asignatura compartida cuenta solo para el grado de la matrícula")
		public void testAsignaturaCompartida() {
			assertFila(getFila(todos(), "G004", "2025-2026"),
					"G004", "Biologia", "Ciencias de la Salud", "2025-2026", 100.0, null, 2, 2);
		}
	}

	// =========================================================================
	// 2. FILTRO POR RAMA
	// =========================================================================
	@Nested
	@DisplayName("2. Filtro por rama de conocimiento")
	public class FiltroRama {

		@Test
		@DisplayName("TM-05 Contiene el texto, sin distinguir mayúsculas")
		public void testRamaSinMayusculas() {
			assertEquals(List.of("G004 2025-2026"), claves(metricasModel.getMetricasGrados("SALUD", null, null, HOY)));
		}

		@Test
		@DisplayName("TM-06 Coincidencia parcial")
		public void testRamaParcial() {
			assertEquals(List.of("G002 2026-2027", "G002 2025-2026", "G002 2024-2025", "G002 2023-2024"),
					claves(metricasModel.getMetricasGrados("sociales", null, null, HOY)));
		}
	}

	// =========================================================================
	// 3. FILTRO POR GRADO
	// =========================================================================
	@Nested
	@DisplayName("3. Filtro por nombre del grado")
	public class FiltroGrado {

		@Test
		@DisplayName("TM-07 Contiene el texto; un grado EN_EXTINCION sí aparece")
		public void testGradoEnExtincion() {
			assertEquals(List.of("G002 2026-2027", "G002 2025-2026", "G002 2024-2025", "G002 2023-2024"),
					claves(metricasModel.getMetricasGrados(null, "derecho", null, HOY)));
		}

		@Test
		@DisplayName("TM-08 Dos grados con el mismo nombre: se ordenan por facultad")
		public void testNombreRepetido() {
			assertEquals(List.of("G001 2026-2027", "G001 2025-2026", "G001 2024-2025", "G004 2025-2026"),
					claves(metricasModel.getMetricasGrados(null, "bio", null, HOY)));
		}
	}

	// =========================================================================
	// 4. FILTRO POR AÑO ACADÉMICO Y "TODOS"
	// =========================================================================
	@Nested
	@DisplayName("4. Filtro por año académico y Todos")
	public class FiltroAnioYTodos {

		@Test
		@DisplayName("TM-09 Un año: un DTO por cada grado con matriculados ese año")
		public void testFiltroAnio() {
			assertEquals(List.of("G001 2025-2026", "G004 2025-2026", "G002 2025-2026"),
					claves(metricasModel.getMetricasGrados(null, null, "2025-2026", HOY)));
		}

		@Test
		@DisplayName("TM-10 Todos: varios DTO por grado, por nombre, facultad y año de más reciente a más antiguo")
		public void testTodos() {
			List<GradoAnioRevisorDisplayDTO> lista = todos();

			assertEquals(List.of("G001 2026-2027", "G001 2025-2026", "G001 2024-2025", "G004 2025-2026",
					"G002 2026-2027", "G002 2025-2026", "G002 2024-2025", "G002 2023-2024"), claves(lista));
			assertAll(
					() -> assertFila(lista.get(0), "G001", "Biologia", "Ciencias", "2026-2027", 100.0, 42.9, 1, 3),
					() -> assertFila(lista.get(1), "G001", "Biologia", "Ciencias", "2025-2026", 66.7, null, 2, 3),
					() -> assertFila(lista.get(2), "G001", "Biologia", "Ciencias", "2024-2025", 42.9, null, 3, 7),
					() -> assertFila(lista.get(3), "G004", "Biologia", "Ciencias de la Salud", "2025-2026", 100.0, null, 2, 2),
					() -> assertFila(lista.get(4), "G002", "Derecho", "Ciencias Sociales y Juridicas", "2026-2027", null, 100.0, 0, 1),
					() -> assertFila(lista.get(5), "G002", "Derecho", "Ciencias Sociales y Juridicas", "2025-2026", 100.0, 0.0, 1, 1),
					() -> assertFila(lista.get(6), "G002", "Derecho", "Ciencias Sociales y Juridicas", "2024-2025", 0.0, null, 0, 1),
					() -> assertFila(lista.get(7), "G002", "Derecho", "Ciencias Sociales y Juridicas", "2023-2024", 0.0, null, 0, 1));
		}

		@Test
		@DisplayName("TM-25 Filtros vacíos equivalen a no filtrar")
		public void testFiltrosVacios() {
			assertEquals(claves(todos()), claves(metricasModel.getMetricasGrados("", "  ", null, HOY)));
		}
	}

	// =========================================================================
	// 5. COMBINACIÓN DE FILTROS
	// =========================================================================
	@Nested
	@DisplayName("5. Combinación de filtros")
	public class CombinacionFiltros {

		@Test
		@DisplayName("TM-11 Rama, grado y año a la vez")
		public void testCombinacion() {
			assertEquals(List.of("G004 2025-2026"),
					claves(metricasModel.getMetricasGrados("salud", "bio", "2025-2026", HOY)));
		}

		@Test
		@DisplayName("TM-12 Combinación sin resultados")
		public void testCombinacionSinResultados() {
			assertTrue(metricasModel.getMetricasGrados("sociales", "bio", null, HOY).isEmpty());
		}
	}

	// =========================================================================
	// 6. CASOS LÍMITE
	// =========================================================================
	@Nested
	@DisplayName("6. Casos límite")
	public class CasosLimite {

		@Test
		@DisplayName("TM-13 Un grado EXTINTO no aparece aunque tenga matrículas válidas")
		public void testGradoExtinto() {
			assertTrue(metricasModel.getMetricasGrados(null, "fisica", null, HOY).isEmpty());
		}

		@Test
		@DisplayName("TM-14 Grado sin matriculados: no genera DTO")
		public void testSinMatriculados() {
			assertTrue(metricasModel.getMetricasGrados(null, "quimica", null, HOY).isEmpty());
		}

		@Test
		@DisplayName("TM-15 Ningún presentado: tasa de éxito null")
		public void testSinPresentados() {
			GradoAnioRevisorDisplayDTO fila = getFila(todos(), "G002", "2026-2027");

			assertAll(
					() -> assertNull(fila.getTasaExito()),
					() -> assertEquals(1, fila.getMatriculados()));
		}

		@Test
		@DisplayName("TM-16 Presentados sin aprobar: tasa de éxito 0, no null")
		public void testExitoCero() {
			List<GradoAnioRevisorDisplayDTO> lista = todos();

			assertAll(
					() -> assertEquals(0.0, getFila(lista, "G002", "2024-2025").getTasaExito()),
					() -> assertEquals(0.0, getFila(lista, "G002", "2023-2024").getTasaExito()));
		}

		@Test
		@DisplayName("TM-17 Abandono null si no existe el año de dos cursos antes")
		public void testAbandonoSinAnioPrevio() {
			List<GradoAnioRevisorDisplayDTO> lista = todos();

			assertAll(
					() -> assertNull(getFila(lista, "G001", "2024-2025").getTasaAbandono()),
					() -> assertNull(getFila(lista, "G002", "2024-2025").getTasaAbandono()),
					() -> assertNull(getFila(lista, "G002", "2023-2024").getTasaAbandono()));
		}

		@Test
		@DisplayName("TM-18 Abandono null si no hubo matriculados dos cursos antes")
		public void testAbandonoSinMatriculadosPrevios() {
			List<GradoAnioRevisorDisplayDTO> lista = todos();

			assertAll(
					() -> assertNull(getFila(lista, "G001", "2025-2026").getTasaAbandono()),
					() -> assertNull(getFila(lista, "G004", "2025-2026").getTasaAbandono()));
		}

		@Test
		@DisplayName("TM-19 Abandono null mientras no termina el periodo de matrícula del año")
		public void testAbandonoEnPeriodoMatriculacion() {
			List<GradoAnioRevisorDisplayDTO> lista = metricasModel.getMetricasGrados(null, null, null, HOY_EN_MATRICULACION);

			assertAll(
					() -> assertNull(getFila(lista, "G001", "2026-2027").getTasaAbandono()),
					() -> assertNull(getFila(lista, "G002", "2026-2027").getTasaAbandono()),
					() -> assertEquals(0.0, getFila(lista, "G002", "2025-2026").getTasaAbandono()));
		}

		@Test
		@DisplayName("TM-20 Abandono: AL06, AL04 (cancelada) y AL14 (otro grado sin asignaturas comunes) de 7; "
				+ "no abandonan el titulado, el que vuelve ni el que pasa a un grado que comparte asignatura")
		public void testAbandono() {
			assertEquals(42.9, getFila(todos(), "G001", "2026-2027").getTasaAbandono());
		}

		@Test
		@DisplayName("TM-21 El abandono se registra dos años después de la última matrícula")
		public void testAbandonoEnElAnioDeDeteccion() {
			List<GradoAnioRevisorDisplayDTO> lista = todos();

			assertAll(
					() -> assertEquals(100.0, getFila(lista, "G002", "2026-2027").getTasaAbandono()),
					() -> assertNull(getFila(lista, "G002", "2024-2025").getTasaAbandono()));
		}
	}

	// =========================================================================
	// 7. AÑO ACADÉMICO POR DEFECTO
	// =========================================================================
	@Nested
	@DisplayName("7. Año académico por defecto")
	public class AnioPorDefecto {

		@Test
		@DisplayName("TM-22 Es el año actual (el de mayor fecha de inicio)")
		public void testAnioActual() {
			assertEquals("2026-2027", metricasModel.getAnioAcademicoPorDefecto());
		}

		@Test
		@DisplayName("TM-23 Es el año actual aunque todavía no tenga datos")
		public void testAnioActualSinDatos() {
			db.executeUpdate("INSERT INTO AnioAcademico VALUES ('2027-2028','2027-09-01','2028-07-31','2027-07-01','2027-09-30')");

			assertEquals("2027-2028", metricasModel.getAnioAcademicoPorDefecto());
		}

		@Test
		@DisplayName("TM-24 Sin años académicos devuelve null")
		public void testSinAnios() {
			db.executeBatch(new String[] {
					"DELETE FROM Calificacion", "DELETE FROM MatriculaCurso", "DELETE FROM Matricula",
					"DELETE FROM Acta", "DELETE FROM Convocatoria", "DELETE FROM AnioAcademico"
			});

			assertNull(metricasModel.getAnioAcademicoPorDefecto());
		}
	}

	private List<GradoAnioRevisorDisplayDTO> todos() {
		return metricasModel.getMetricasGrados(null, null, null, HOY);
	}

	private static List<String> claves(List<GradoAnioRevisorDisplayDTO> lista) {
		return lista.stream().map(d -> d.getIdGrado() + " " + d.getIdAnioAcademico()).collect(Collectors.toList());
	}

	private static GradoAnioRevisorDisplayDTO getFila(List<GradoAnioRevisorDisplayDTO> lista, String gradoId, String anio) {
		return lista.stream().filter(d -> gradoId.equals(d.getIdGrado()) && anio.equals(d.getIdAnioAcademico()))
				.findFirst().orElseThrow(() -> new AssertionError("No hay DTO para " + gradoId + " " + anio));
	}

	private static void assertFila(GradoAnioRevisorDisplayDTO fila, String idGrado, String nombreGrado, String rama,
			String anio, Double tasaExito, Double tasaAbandono, int aprobados, int matriculados) {
		String fallo = idGrado + " " + anio;
		assertAll(fallo,
				() -> assertEquals(idGrado, fila.getIdGrado(), fallo + ": idGrado"),
				() -> assertEquals(nombreGrado, fila.getNombreGrado(), fallo + ": nombreGrado"),
				() -> assertEquals(rama, fila.getRama(), fallo + ": rama"),
				() -> assertEquals(anio, fila.getIdAnioAcademico(), fallo + ": idAnioAcademico"),
				() -> assertEquals(tasaExito, fila.getTasaExito(), fallo + ": tasaExito"),
				() -> assertEquals(tasaAbandono, fila.getTasaAbandono(), fallo + ": tasaAbandono"),
				() -> assertEquals(aprobados, fila.getAprobados(), fallo + ": aprobados"),
				() -> assertEquals(matriculados, fila.getMatriculados(), fallo + ": matriculados"));
	}
}
