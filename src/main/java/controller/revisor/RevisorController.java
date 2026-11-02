package controller.revisor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import model.grado.GradoAnioRevisorDisplayDTO;
import repository.MatriculacionRepository;
import repository.MetricasGradoRepository;
import view.revisor.RevisorView;

/**
 * Controlador de la vista del revisor externo (HU 36411).
 * Las métricas se cargan una sola vez al abrir la vista; el combo del año y los filtros
 * de rama y grado se aplican sobre esa lista en memoria.
 */
public class RevisorController {
	private static final String TODOS = "Todos";

	private RevisorView view;
	private MetricasGradoRepository metricasGradoRepository;
	private MatriculacionRepository matriculacionRepository;

	private Runnable onVolverInicio = () -> { };

	private List<GradoAnioRevisorDisplayDTO> todasLasMetricas = new ArrayList<>();

	public RevisorController(RevisorView view) {
		this(view, new MetricasGradoRepository(), new MatriculacionRepository());
	}

	public RevisorController(RevisorView view, MetricasGradoRepository metricasGradoRepository,
			MatriculacionRepository matriculacionRepository) {
		this.view = view;
		this.metricasGradoRepository = metricasGradoRepository;
		this.matriculacionRepository = matriculacionRepository;
	}

	public void setOnVolverInicio(Runnable onVolverInicio) {
		this.onVolverInicio = onVolverInicio;
	}

	public void initController() {
		view.getBtnVolverInicio().addActionListener(e -> volverInicio());

		cargarAnios();
		todasLasMetricas = metricasGradoRepository.getMetricasGrados(null, null, null);

		view.getCbAnioAcademico().addActionListener(e -> aplicarFiltros());
		addDocumentListener(view.getTxtFiltroRama(), this::aplicarFiltros);
		addDocumentListener(view.getTxtFiltroGrado(), this::aplicarFiltros);
		aplicarFiltros();

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
		for (String anio : matriculacionRepository.getAniosAcademicos()) {
			anios.addElement(anio);
		}
		String porDefecto = metricasGradoRepository.getAnioAcademicoPorDefecto();
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
				.filter(m -> todos || anio.equals(m.getIdAnioAcademico()))
				.collect(Collectors.toList());

		if (todos) {
			view.mostrarMetricasTodos(filtradas);
		} else {
			view.mostrarMetricasAnio(filtradas);
		}
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
