package model.matriculacion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Reglas que calculan los DTO que ve el estudiante (HU 35232), sin base de datos.
 */
public class ReglasDTOTest {

	@ParameterizedTest(name = "nota {0} -> {1}")
	@CsvSource(value = { "NULL, NO PRESENTADO", "5.0, APROBADO", "4.99, SUSPENSO", "10, APROBADO" }, nullValues = "NULL")
	@DisplayName("Resultado de una calificación según NOTA_APROBADO")
	public void testResultado(Double nota, String esperado) {
		assertEquals(esperado, new CalificacionDisplayDTO("1", "C1", "Curso", "ORDINARIA_ENERO", nota).getResultado());
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@CsvSource(value = { "ORDINARIA_ENERO, Ordinaria Enero", "EXTRAORDINARIA_JULIO, Extraordinaria Julio", "NULL, -" },
			nullValues = "NULL")
	@DisplayName("Nombre legible de la convocatoria")
	public void testConvocatoriaLegible(String tipo, String esperado) {
		assertEquals(esperado, new CalificacionDisplayDTO("1", "C1", "Curso", tipo, 7.0).getConvocatoriaLegible());
	}

	@Test
	@DisplayName("Hito anual: sin año evaluado está en curso; con él, 12 créditos lo superan y 11 no")
	public void testHitoAnualTexto() {
		assertEquals("1º AÑO EN CURSO", new HitosPermanenciaDTO("A", "G", null, 0, 0, 1).getEstadoHitoAnualTexto());
		assertEquals("SUPERADO", new HitosPermanenciaDTO("A", "G", "2025-2026", 12, 12, 1).getEstadoHitoAnualTexto());
		assertEquals("NO SUPERADO", new HitosPermanenciaDTO("A", "G", "2025-2026", 11, 11, 1).getEstadoHitoAnualTexto());
	}

	@Test
	@DisplayName("Hito del tercer año: en progreso hasta el tercer año; después, 90 créditos lo superan y 89 no")
	public void testHitoTercerAnioTexto() {
		assertEquals("EN PROGRESO (2º año de 3)",
				new HitosPermanenciaDTO("A", "G", "2025-2026", 12, 40, 2).getEstadoHitoTercerAnioTexto());
		assertEquals("SUPERADO", new HitosPermanenciaDTO("A", "G", "2025-2026", 12, 90, 3).getEstadoHitoTercerAnioTexto());
		assertEquals("NO SUPERADO", new HitosPermanenciaDTO("A", "G", "2025-2026", 12, 89, 3).getEstadoHitoTercerAnioTexto());
	}
}
