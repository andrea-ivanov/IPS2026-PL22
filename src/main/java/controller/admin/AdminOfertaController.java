package controller.admin;

import view.admin.AdminOfertaView;
import view.admin.TestDataFactory;
import model.curso.Curso;
import model.curso.CursoDisplayDTO;
import model.grado.Grado;
import model.grado.GradoDisplayDTO;
import model.matriculacion.Matriculacion;
import model.matriculacion.MatriculacionDisplayDTO;
import repository.AlumnoRepository;
import repository.CursoRepository;
import repository.GradoRepository;
import repository.MatriculacionRepository;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador de la vista de administración de oferta académica (AdminOfertaView).
 * Conecta la vista con los repositorios y modelos de Grado, Curso y Matriculación.
 * Soporta navegación entre paneles (Grados -> Cursos -> Matrícula), filtrado reactivo
 * y mockeo estructurado con las clases y DTOs de dominio existentes.
 */
public class AdminOfertaController {
	private AdminOfertaView view;

	// Repositorios de acceso a datos
	private GradoRepository gradoRepository;
	private CursoRepository cursoRepository;
	private MatriculacionRepository matriculacionRepository;
	private AlumnoRepository alumnoRepository;

	// Datos en memoria para las listas activas y filtros
	private List<GradoDisplayDTO> todosLosGrados = new ArrayList<>();
	private List<CursoDisplayDTO> todosLosCursos = new ArrayList<>();
	private List<MatriculacionDisplayDTO> todasLasMatriculaciones = new ArrayList<>();

	public AdminOfertaController(AdminOfertaView view) {
		this.view = view;
		try {
			this.gradoRepository = new GradoRepository();
			this.cursoRepository = new CursoRepository();
			this.matriculacionRepository = new MatriculacionRepository();
			this.alumnoRepository = new AlumnoRepository();
		} catch (Exception ex) {
			System.err.println("Aviso: No se pudo conectar con los repositorios en base de datos. Modo mock activado: " + ex.getMessage());
		}
		this.initView();
	}

	public AdminOfertaController(AdminOfertaView view,
								 GradoRepository gradoRepository,
								 CursoRepository cursoRepository,
								 MatriculacionRepository matriculacionRepository,
								 AlumnoRepository alumnoRepository) {
		this.view = view;
		this.gradoRepository = gradoRepository;
		this.cursoRepository = cursoRepository;
		this.matriculacionRepository = matriculacionRepository;
		this.alumnoRepository = alumnoRepository;
		this.initView();
	}

	public void initController() {
		// 1. Carga inicial de datos de Grados
		cargarMocksGrados();

		// 3. Conexión de callbacks del botón 'Ver' en JTable
		view.setOnVerGrado((idGrado, nombreGrado) -> {
			cargarCursosDeGrado(idGrado, nombreGrado);
		});

		view.setOnVerCurso((idCurso, nombreCurso) -> {
			cargarMatriculasDeCurso(idCurso, nombreCurso);
		});

		// 4. Selección por fila / doble clic en tablas
		view.getTabGrados().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int row = view.getTabGrados().getSelectedRow();
				if (row >= 0 && e.getClickCount() == 2) {
					String idGrado = String.valueOf(view.getTabGrados().getValueAt(row, 0));
					String nombreGrado = String.valueOf(view.getTabGrados().getValueAt(row, 1));
					view.showCursosView(nombreGrado);
					cargarCursosDeGrado(idGrado, nombreGrado);
				}
			}
		});

		view.getTabCursos().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int row = view.getTabCursos().getSelectedRow();
				if (row >= 0 && e.getClickCount() == 2) {
					String idCurso = String.valueOf(view.getTabCursos().getValueAt(row, 0));
					String nombreCurso = String.valueOf(view.getTabCursos().getValueAt(row, 1));
					view.showMatriculacionView(nombreCurso);
					cargarMatriculasDeCurso(idCurso, nombreCurso);
				}
			}
		});

		// 5. Configurar listeners de filtrado reactivo
		configurarFiltrosGrados();
		configurarFiltrosCursos();
		configurarFiltrosMatriculacion();
	}

	// =========================================================================
	// Carga de datos (con fallback a mocks estructurados)
	// =========================================================================

	public void cargarMocksGrados() {
		todosLosGrados.clear();
		try {
			// Intenta obtener grado desde repositorio si la base de datos está disponible
			Grado g1 = gradoRepository.findById("1");
			if (g1 != null) {
				todosLosGrados.add(g1.toDisplayDTO());
			}
		} catch (Exception ex) {
			// En caso de base de datos no inicializada, usa los mocks estructurados
		}

		if (todosLosGrados.isEmpty()) {
			todosLosGrados = TestDataFactory.crearMockGradosDisplay();
		} else {
			// Añade los demás grados mockeados para una experiencia visual completa
			List<GradoDisplayDTO> mocks = TestDataFactory.crearMockGradosDisplay();
			for (GradoDisplayDTO m : mocks) {
				if (!todosLosGrados.get(0).getId().equals(m.getId())) {
					todosLosGrados.add(m);
				}
			}
		}

		aplicarFiltrosGrados();
	}

	public void cargarCursosDeGrado(String idGrado, String nombreGrado) {
		todosLosCursos.clear();
		try {
			Grado grado = gradoRepository.findById(idGrado);
			if (grado != null && grado.getCursos() != null && !grado.getCursos().isEmpty()) {
				for (Curso c : grado.getCursos()) {
					todosLosCursos.add(c.toDisplayDTO());
				}
			}
		} catch (Exception ex) {
			// Fallback a mocks del dominio
		}

		if (todosLosCursos.isEmpty()) {
			todosLosCursos = TestDataFactory.crearMockCursosDisplay(idGrado);
		}

		aplicarFiltrosCursos();
	}

	public void cargarMatriculasDeCurso(String idCurso, String nombreCurso) {
		todasLasMatriculaciones.clear();
		try {
			Matriculacion mat = matriculacionRepository.findById("1");
			if (mat != null && mat.getAlumno() != null) {
				todasLasMatriculaciones.add(mat.getAlumno().toDisplayDTO());
			}
		} catch (Exception ex) {
			// Fallback a mocks del dominio
		}

		if (todasLasMatriculaciones.isEmpty()) {
			todasLasMatriculaciones = TestDataFactory.crearMockMatriculacionesDisplay(idCurso);
		}

		aplicarFiltrosMatriculacion();
	}

	// =========================================================================
	// Filtrado reactivo en tablas
	// =========================================================================

	private void configurarFiltrosGrados() {
		addDocumentListener(view.getTxtFiltroFacultadGrado(), this::aplicarFiltrosGrados);
		addDocumentListener(view.getTxtFiltroRamaGrado(), this::aplicarFiltrosGrados);
		view.getCbEstadoGrado().addActionListener(e -> aplicarFiltrosGrados());
	}

	private void aplicarFiltrosGrados() {
		String filtroFacultad = view.getTxtFiltroFacultadGrado().getText().trim().toLowerCase();
		String filtroRama = view.getTxtFiltroRamaGrado().getText().trim().toLowerCase();
		String filtroEstado = (String) view.getCbEstadoGrado().getSelectedItem();

		List<GradoDisplayDTO> filtrados = todosLosGrados.stream()
				.filter(g -> filtroFacultad.isEmpty() || (g.getFacultad() != null && g.getFacultad().toLowerCase().contains(filtroFacultad)))
				.filter(g -> filtroRama.isEmpty() || (g.getRama() != null && g.getRama().toLowerCase().contains(filtroRama)))
				.filter(g -> filtroEstado == null || "TODOS".equalsIgnoreCase(filtroEstado) || (g.getEstado() != null && g.getEstado().equalsIgnoreCase(filtroEstado)))
				.collect(Collectors.toList());

		view.actualizarTablaGrados(filtrados);
	}

	private void configurarFiltrosCursos() {
		addDocumentListener(view.getTxtFiltroAnoCurso(), this::aplicarFiltrosCursos);
		addDocumentListener(view.getTxtFiltroItinerarioCurso(), this::aplicarFiltrosCursos);
		addDocumentListener(view.getTxtFiltroProfesorCurso(), this::aplicarFiltrosCursos);
		view.getCbFiltroEstadoCurso().addActionListener(e -> aplicarFiltrosCursos());
	}

	private void aplicarFiltrosCursos() {
		String filtroAno = view.getTxtFiltroAnoCurso().getText().trim().toLowerCase();
		String filtroItinerario = view.getTxtFiltroItinerarioCurso().getText().trim().toLowerCase();
		String filtroProfesor = view.getTxtFiltroProfesorCurso().getText().trim().toLowerCase();
		String filtroEstado = (String) view.getCbFiltroEstadoCurso().getSelectedItem();

		List<CursoDisplayDTO> filtrados = todosLosCursos.stream()
				.filter(c -> filtroAno.isEmpty() || (c.getAno() != null && c.getAno().toLowerCase().contains(filtroAno)))
				.filter(c -> filtroItinerario.isEmpty() || (c.getItinerario() != null && c.getItinerario().toLowerCase().contains(filtroItinerario)))
				.filter(c -> filtroProfesor.isEmpty() || (c.getProfesor() != null && c.getProfesor().toLowerCase().contains(filtroProfesor)))
				.filter(c -> filtroEstado == null || "TODOS".equalsIgnoreCase(filtroEstado) || (c.getEstado() != null && c.getEstado().equalsIgnoreCase(filtroEstado)))
				.collect(Collectors.toList());

		view.actualizarTablaCursos(filtrados);
	}

	private void configurarFiltrosMatriculacion() {
		addDocumentListener(view.getTxtFiltroAlumno(), this::aplicarFiltrosMatriculacion);
	}

	private void aplicarFiltrosMatriculacion() {
		String filtroAlumno = view.getTxtFiltroAlumno().getText().trim().toLowerCase();

		List<MatriculacionDisplayDTO> filtrados = todasLasMatriculaciones.stream()
				.filter(m -> filtroAlumno.isEmpty()
						|| (m.getId() != null && m.getId().toLowerCase().contains(filtroAlumno))
						|| (m.getNombreAlumno() != null && m.getNombreAlumno().toLowerCase().contains(filtroAlumno)))
				.collect(Collectors.toList());

		view.actualizarTablaMatriculacion(filtrados);
	}

	private void addDocumentListener(JTextField textField, Runnable action) {
		textField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) { action.run(); }
			@Override
			public void removeUpdate(DocumentEvent e) { action.run(); }
			@Override
			public void changedUpdate(DocumentEvent e) { action.run(); }
		});
	}

	private void initView() {
		view.getFrame().setVisible(true);
	}
}