package view.revisor;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComboBox;
import javax.swing.table.TableModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import controller.revisor.RevisorController;
import model.grado.MetricasGradoModelTest;
import util.Database;
import view.login.MainView;

/**
 * Pruebas de la vista del revisor externo (HU 36411) a través de su controlador: combo de años, tabla de un año,
 * tabla agrupada de "Todos", filtros en memoria y vuelta al inicio.
 * Usa los datos de MetricasGradoModelTest. El controlador calcula con la fecha de hoy, y todos los periodos de
 * matrícula de esos datos terminan como muy tarde el 2026-09-30, así que las tasas son las del modelo.
 */
public class RevisorVistaTest {
	private static final Database db = new Database("jdbc:sqlite:TestDB.db?foreign_keys=true");
	private RevisorView view;
	private final AtomicBoolean volvioAlInicio = new AtomicBoolean(false);

	@BeforeEach
	public void setUp() {
		db.createDatabase(false);
		MetricasGradoModelTest.loadCleanDatabase(db);
	}

	@AfterEach
	public void tearDown() {
		if (view != null && view.getFrame() != null) {
			view.getFrame().dispose();
		}
	}

	private void abrir() {
		view = new RevisorView();
		RevisorController controller = new RevisorController(view, db);
		controller.setOnVolverInicio(() -> volvioAlInicio.set(true));
		controller.initController();
	}

	@Test
	@DisplayName("TV-01 El combo tiene Todos y los años del más reciente al más antiguo, con el año actual elegido")
	public void testComboAnios() {
		abrir();

		JComboBox<String> cb = view.getCbAnioAcademico();
		assertAll(
				() -> assertEquals(List.of("Todos", "2026-2027", "2025-2026", "2024-2025", "2023-2024"), items(cb)),
				() -> assertEquals("2026-2027", cb.getSelectedItem()));
	}

	@Test
	@DisplayName("TV-02 Al abrir se ve el año actual: una fila por grado, tasas con 1 decimal y — si no hay valor")
	public void testTablaAnioActual() {
		abrir();

		TableModel t = view.getTabMetricas().getModel();
		assertAll(
				() -> assertEquals(List.of("Nombre Grado", "Rama", "Tasa Éxito", "Tasa abandono", "Aprobados",
						"Matriculados", "Acciones"), columnas(t)),
				() -> assertEquals(2, t.getRowCount()),
				() -> assertEquals(List.of("Biologia", "Ciencias", "100.0%", "42.9%", 1, 3, "Ver"), fila(t, 0)),
				() -> assertEquals(List.of("Derecho", "Ciencias Sociales y Juridicas", "—", "100.0%", 0, 1, "Ver"),
						fila(t, 1)));
	}

	@Test
	@DisplayName("TV-03 Elegir otro año recarga la tabla con los grados de ese año")
	public void testCambiarAnio() {
		abrir();
		view.getCbAnioAcademico().setSelectedItem("2025-2026");

		TableModel t = view.getTabMetricas().getModel();
		assertAll(
				() -> assertEquals(3, t.getRowCount()),
				() -> assertEquals(List.of("Biologia", "Ciencias", "66.7%", "—", 2, 3, "Ver"), fila(t, 0)),
				() -> assertEquals(List.of("Biologia", "Ciencias de la Salud", "100.0%", "—", 2, 2, "Ver"), fila(t, 1)),
				() -> assertEquals(List.of("Derecho", "Ciencias Sociales y Juridicas", "100.0%", "0.0%", 1, 1, "Ver"),
						fila(t, 2)));
	}

	@Test
	@DisplayName("TV-04 Todos: un bloque por grado con su cabecera Nombre - Facultad - Rama y una fila por año")
	public void testTodosAgrupado() {
		abrir();
		view.getCbAnioAcademico().setSelectedItem("Todos");

		TableModel t = view.getTabMetricas().getModel();
		assertAll(
				() -> assertEquals(List.of("Año Académico", "Éxito", "Abandono", "Aprobados", "Matriculados",
						"Acciones"), columnas(t)),
				// 3 cabeceras (G001, G004, G002) + 8 filas de años
				() -> assertEquals(11, t.getRowCount()),
				() -> assertEquals("Biologia - Facultad de Ciencias - Ciencias", t.getValueAt(0, 0)),
				() -> assertNull(t.getValueAt(0, 1), "La cabecera del bloque solo ocupa la primera columna"),
				() -> assertEquals("Ver", t.getValueAt(0, 5), "El botón Ver solo va en la cabecera del bloque"),
				() -> assertEquals(Arrays.asList("2026-2027", "100.0%", "42.9%", 1, 3, null), fila(t, 1)),
				() -> assertEquals(Arrays.asList("2024-2025", "42.9%", "—", 3, 7, null), fila(t, 3)),
				() -> assertEquals("Biologia - Facultad de Veterinaria - Ciencias de la Salud", t.getValueAt(4, 0)),
				() -> assertEquals("Derecho - Facultad de Derecho - Ciencias Sociales y Juridicas", t.getValueAt(6, 0)),
				() -> assertEquals(Arrays.asList("2023-2024", "0.0%", "—", 0, 1, null), fila(t, 10)));
	}

	@Test
	@DisplayName("TV-05 El filtro de rama se aplica al escribir, sin distinguir mayúsculas")
	public void testFiltroRama() {
		abrir();
		view.getCbAnioAcademico().setSelectedItem("Todos");
		view.getTxtFiltroRama().setText("SALUD");

		TableModel t = view.getTabMetricas().getModel();
		assertAll(
				() -> assertEquals(2, t.getRowCount()),
				() -> assertEquals("Biologia - Facultad de Veterinaria - Ciencias de la Salud", t.getValueAt(0, 0)),
				() -> assertEquals(Arrays.asList("2025-2026", "100.0%", "—", 2, 2, null), fila(t, 1)));
	}

	@Test
	@DisplayName("TV-06 Filtros de grado y año a la vez; al borrar el filtro vuelve la lista completa del año")
	public void testFiltroGradoYAnio() {
		abrir();
		view.getCbAnioAcademico().setSelectedItem("2025-2026");
		view.getTxtFiltroGrado().setText("  der ");

		TableModel t = view.getTabMetricas().getModel();
		assertEquals(1, t.getRowCount());
		assertEquals("Derecho", t.getValueAt(0, 0));

		view.getTxtFiltroGrado().setText("");
		assertEquals(3, view.getTabMetricas().getModel().getRowCount());
	}

	@Test
	@DisplayName("TV-07 Filtros sin coincidencias dejan la tabla vacía")
	public void testFiltroSinResultados() {
		abrir();
		view.getTxtFiltroRama().setText("sociales");
		view.getTxtFiltroGrado().setText("bio");

		assertEquals(0, view.getTabMetricas().getModel().getRowCount());
	}

	@Test
	@DisplayName("TV-08 Sin años académicos el combo solo tiene Todos y solo sale la cabecera del grado EN_EXTINCION")
	public void testSinAnios() {
		db.executeBatch(new String[] {
				"DELETE FROM Calificacion", "DELETE FROM MatriculaCurso", "DELETE FROM Matricula",
				"DELETE FROM Acta", "DELETE FROM Convocatoria", "DELETE FROM AnioAcademico"
		});
		abrir();

		// HU 36455: un grado que requiere acción (G002 Derecho, EN_EXTINCION) aparece aunque no tenga métricas
		TableModel t = view.getTabMetricas().getModel();
		assertAll(
				() -> assertEquals(List.of("Todos"), items(view.getCbAnioAcademico())),
				() -> assertEquals("Todos", view.getCbAnioAcademico().getSelectedItem()),
				() -> assertEquals(1, t.getRowCount()),
				() -> assertEquals("Derecho - Facultad de Derecho - Ciencias Sociales y Juridicas", t.getValueAt(0, 0)));
	}

	@Test
	@DisplayName("TV-09 Volver al inicio cierra el panel y avisa a la pantalla de inicio")
	public void testVolverInicio() {
		abrir();
		assertTrue(view.getFrame().isVisible());

		view.getBtnVolverInicio().doClick();

		assertAll(
				() -> assertFalse(view.getFrame().isDisplayable(), "La ventana del revisor se cierra"),
				() -> assertTrue(volvioAlInicio.get(), "Se vuelve a mostrar la pantalla de inicio"));
	}

	@Test
	@DisplayName("TV-10 La pantalla de inicio ofrece el rol Revisor externo")
	public void testRolEnPantallaInicio() {
		MainView main = new MainView();
		try {
			List<String> roles = new ArrayList<>();
			for (int i = 0; i < main.getCbRoles().getItemCount(); i++) {
				roles.add(main.getCbRoles().getItemAt(i));
			}
			assertTrue(roles.contains("Revisor externo"), "Roles: " + roles);
		} finally {
			main.getFrame().dispose();
		}
	}

	private static List<String> items(JComboBox<String> cb) {
		List<String> items = new ArrayList<>();
		for (int i = 0; i < cb.getItemCount(); i++) {
			items.add(cb.getItemAt(i));
		}
		return items;
	}

	private static List<String> columnas(TableModel t) {
		List<String> columnas = new ArrayList<>();
		for (int i = 0; i < t.getColumnCount(); i++) {
			columnas.add(t.getColumnName(i));
		}
		return columnas;
	}

	private static List<Object> fila(TableModel t, int fila) {
		List<Object> valores = new ArrayList<>();
		for (int i = 0; i < t.getColumnCount(); i++) {
			valores.add(t.getValueAt(fila, i));
		}
		return valores;
	}
}
