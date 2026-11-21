package controller.revisor;

import util.FechaSistema;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import model.grado.GradoAnioRevisorDisplayDTO;
import model.grado.GradoModel;
import model.grado.GradoRevisionDisplayDTO;
import model.grado.MetricasGradoModel;
import model.grado.ResultadoRevision;
import util.Database;
import util.SwingUtil;
import view.revisor.RevisorView;

/**
 * Controlador de la vista del revisor externo (HU 36411 y 36455).
 * Las métricas se cargan una sola vez al abrir la vista; el combo del año y los filtros
 * de rama y grado se aplican sobre esa lista en memoria. Desde el panel de métricas se entra
 * a un grado ("Ver" o doble clic) para tomar la decisión sobre su renovación.
 */
public class RevisorController {
	private static final String TODOS = "Todos";
	private static final String TITULO_DECISION = "Tomar decisión";

	private RevisorView view;
	private MetricasGradoModel metricasModel;
	private GradoModel gradoModel;

	private Runnable onVolverInicio = () -> { };

	private List<GradoAnioRevisorDisplayDTO> todasLasMetricas = new ArrayList<>();

	// Grado que se está viendo en la tarjeta GRADO
	private String gradoActualId;
	private String gradoActualNombre;

	public RevisorController(RevisorView view, Database db) {
		this.view = view;
		this.metricasModel = new MetricasGradoModel(db);
		this.gradoModel = new GradoModel(db);
	}

	public void setOnVolverInicio(Runnable onVolverInicio) {
		this.onVolverInicio = onVolverInicio;
	}

	public void initController() {
		view.getBtnVolverInicio().addActionListener(e -> volverInicio());

		cargarAnios();
		todasLasMetricas = metricasModel.getMetricasGrados(null, null, null, FechaSistema.hoyStr());

		view.getCbAnioAcademico().addActionListener(e -> aplicarFiltros());
		addDocumentListener(view.getTxtFiltroRama(), this::aplicarFiltros);
		addDocumentListener(view.getTxtFiltroGrado(), this::aplicarFiltros);
		aplicarFiltros();

		// Entrar a un grado: botón "Ver" o doble clic en cualquier fila
		view.setOnVerGrado(idGrado -> SwingUtil.exceptionWrapper(() -> abrirGrado(idGrado)));
		view.getTabMetricas().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				String idGrado = view.getIdGradoFila(view.getTabMetricas().rowAtPoint(e.getPoint()));
				if (e.getClickCount() == 2 && idGrado != null) {
					SwingUtil.exceptionWrapper(() -> abrirGrado(idGrado));
				}
			}
		});
		view.getBtnTomarDecision().addActionListener(e -> SwingUtil.exceptionWrapper(this::onTomarDecision));
		view.getBtnAtras().addActionListener(e -> SwingUtil.exceptionWrapper(this::volverAMetricas));

		// Se muestra al final: si la carga falla, no queda una ventana vacía abierta
		view.getFrame().setVisible(true);
	}

	/** Cierra la ventana del revisor y vuelve a la pantalla de selección de rol. */
	private void volverInicio() {
		view.getFrame().dispose();
		onVolverInicio.run();
	}

	/** "Todos" y después los años de más reciente a más antiguo; seleccionado el actual ("Todos" si no hay años). */
	private void cargarAnios() {
		DefaultComboBoxModel<String> anios = new DefaultComboBoxModel<>();
		anios.addElement(TODOS);
		for (String anio : metricasModel.getAniosAcademicos()) {
			anios.addElement(anio);
		}
		String porDefecto = metricasModel.getAnioAcademicoPorDefecto();
		anios.setSelectedItem(porDefecto == null ? TODOS : porDefecto);
		view.getCbAnioAcademico().setModel(anios);
	}

	private void aplicarFiltros() {
		String filtroRama = view.getTxtFiltroRama().getText().trim().toLowerCase();
		String filtroGrado = view.getTxtFiltroGrado().getText().trim().toLowerCase();
		String anio = (String) view.getCbAnioAcademico().getSelectedItem();
		boolean todos = anio == null || TODOS.equals(anio);

		List<GradoAnioRevisorDisplayDTO> filtradas = todasLasMetricas.stream()
				.filter(m -> filtroRama.isEmpty()
						|| (m.getRama() != null && m.getRama().toLowerCase().contains(filtroRama)))
				.filter(m -> filtroGrado.isEmpty()
						|| (m.getNombreGrado() != null && m.getNombreGrado().toLowerCase().contains(filtroGrado)))
				.collect(Collectors.toList());

		if (todos) {
			view.mostrarMetricasTodos(filtradas);
		} else {
			view.mostrarMetricasAnio(metricasModel.metricasDelAnio(filtradas, anio));
		}
	}

	/**
	 * Tarjeta del grado: cabecera, decisión y fecha desde la base de datos; las métricas de todos sus años de la
	 * lista ya cargada (sin aplicar el combo ni los filtros). "Tomar decisión" solo se habilita si el grado la admite.
	 */
	private void abrirGrado(String idGrado) {
		GradoRevisionDisplayDTO grado = gradoModel.getGradoRevision(idGrado);
		List<GradoAnioRevisorDisplayDTO> metricas = todasLasMetricas.stream()
				.filter(m -> m.getIdGrado().equals(idGrado) && m.getIdAnioAcademico() != null)
				.collect(Collectors.toList());
		gradoActualId = idGrado;
		gradoActualNombre = grado.getNombre();
		actualizarEstadoEnMemoria(idGrado, grado.getEstado());
		view.mostrarGrado(grado, metricas);
		view.getBtnTomarDecision().setEnabled(gradoModel.admiteDictamen(idGrado, FechaSistema.hoyStr()));
		view.showGradoView();
	}

	/** Elige la decisión, pide confirmación y la registra; al cerrar o responder "No" no cambia nada. */
	private void onTomarDecision() {
		Object[] opciones = { "Favorable", "Desfavorable" };
		int eleccion = JOptionPane.showOptionDialog(view.getFrame(), "Decisión:", TITULO_DECISION,
				JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
		if (eleccion != 0 && eleccion != 1) {
			return;
		}
		String resultado = (eleccion == 0 ? ResultadoRevision.FAVORABLE : ResultadoRevision.DESFAVORABLE).name();

		int confirm = JOptionPane.showConfirmDialog(view.getFrame(),
				"¿Seguro que desea emitir un dictamen " + resultado + " para el grado " + gradoActualNombre + "?",
				TITULO_DECISION, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (confirm != JOptionPane.YES_OPTION) {
			return;
		}

		gradoModel.emitirDictamen(gradoActualId, resultado, FechaSistema.hoyStr());
		JOptionPane.showMessageDialog(view.getFrame(),
				"Dictamen " + resultado + " registrado para el grado " + gradoActualNombre + ".", TITULO_DECISION,
				JOptionPane.INFORMATION_MESSAGE);
		abrirGrado(gradoActualId);
	}

	/** El estado actual del grado (cambia con cada dictamen) se refleja en la lista cargada, para el color al volver */
	private void actualizarEstadoEnMemoria(String idGrado, String estado) {
		for (GradoAnioRevisorDisplayDTO m : todasLasMetricas) {
			if (m.getIdGrado().equals(idGrado)) {
				m.setEstado(estado);
			}
		}
	}

	/** Vuelve al panel de métricas con el mismo año y los mismos filtros */
	private void volverAMetricas() {
		aplicarFiltros();
		view.showMetricasView();
	}

	private void addDocumentListener(JTextField textField, Runnable action) {
		textField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				action.run();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				action.run();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				action.run();
			}
		});
	}
}
