package controller.admin;

import view.admin.AdminOfertaView;
import view.admin.AnoAcademicoFormDialog;
import view.admin.CursoFormDialog;
import view.admin.GradoFormDialog;
import model.anoacademico.AnoAcademico;
import model.curso.CursoDisplayDTO;
import model.curso.CursoEdicionDTO;
import model.grado.GradoDisplayDTO;
import model.matriculacion.MatriculacionDisplayDTO;
import repository.AlumnoRepository;
import repository.AnoAcademicoRepository;
import repository.CursoRepository;
import repository.GradoRepository;
import repository.MatriculacionRepository;
import repository.ProfesorRepository;
import util.SwingUtil;

import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador de la vista de administración de oferta académica
 * (AdminOfertaView).
 * Conecta la vista con los repositorios y modelos de Grado, Curso,
 * Matriculación y Año Académico.
 * Soporta navegación entre paneles (Grados -> Cursos -> Matrícula), filtrado
 * reactivo,
 * gestión del Año Académico actual y mockeo estructurado.
 */
public class AdminOfertaController {
	private AdminOfertaView view;

	//Boton de volver inicio
	private Runnable onVolverInicio = () -> { };

	// Repositorios de acceso a datos
	private GradoRepository gradoRepository;
	private CursoRepository cursoRepository;
	private MatriculacionRepository matriculacionRepository;
	private AnoAcademicoRepository anoAcademicoRepository;

	// Estado del Año Académico actual
	private AnoAcademico anoAcademicoActual;

	// Datos en memoria para las listas activas y filtros
	private List<GradoDisplayDTO> todosLosGrados = new ArrayList<>();
	private List<CursoDisplayDTO> todosLosCursos = new ArrayList<>();
	private List<MatriculacionDisplayDTO> todasLasMatriculaciones = new ArrayList<>();

	// Grado y curso que se están viendo en los paneles CURSOS y MATRICULACION
	private String gradoActualId;
	private String gradoActualNombre;
	private String cursoActualId;
	private String cursoActualNombre;

	public AdminOfertaController(AdminOfertaView view) {
		this.view = view;
		try {
			this.gradoRepository = new GradoRepository();
			this.cursoRepository = new CursoRepository();
			this.matriculacionRepository = new MatriculacionRepository();
			this.anoAcademicoRepository = new AnoAcademicoRepository();
		} catch (Exception ex) {
			System.err.println("Aviso: No se pudo conectar con los repositorios en base de datos. Modo mock activado: "
					+ ex.getMessage());
			this.anoAcademicoRepository = new AnoAcademicoRepository();
		}
		this.initView();
	}

	public AdminOfertaController(AdminOfertaView view,
			GradoRepository gradoRepository,
			CursoRepository cursoRepository,
			MatriculacionRepository matriculacionRepository,
			AlumnoRepository alumnoRepository) {
		this(view, gradoRepository, cursoRepository, matriculacionRepository, alumnoRepository,
				new AnoAcademicoRepository());
	}

	public AdminOfertaController(AdminOfertaView view,
			GradoRepository gradoRepository,
			CursoRepository cursoRepository,
			MatriculacionRepository matriculacionRepository,
			AlumnoRepository alumnoRepository,
			AnoAcademicoRepository anoAcademicoRepository) {
		this.view = view;
		this.gradoRepository = gradoRepository;
		this.cursoRepository = cursoRepository;
		this.matriculacionRepository = matriculacionRepository;
		this.anoAcademicoRepository = anoAcademicoRepository;
		this.initView();
	}

	public void setOnVolverInicio(Runnable onVolverInicio) {
        this.onVolverInicio = onVolverInicio;
    }

	public void initController() {
		view.getBtnVolverInicio().addActionListener(e -> volverInicio());
		// 1. Carga inicial del Año Académico actual desde la base de datos y reflejo en
		// la vista
		cargarAnoAcademicoActual();

		// Configuración de un nuevo año académico y su periodo de matriculación
		view.getBtnNuevoAnoAcademico().addActionListener(e -> SwingUtil.exceptionWrapper(this::onNuevoAnoAcademico));

		// 2. Carga inicial de datos de Grados
		cargarGrados();

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

		// 5. Conexión de filtros reactivos
		configurarFiltrosGrados();
		configurarFiltrosCursos();
		configurarFiltrosMatriculacion();

		// 6. Alta, modificación y baja de grados y cursos (HU 35223)
		view.getBtnAnadirGrado().addActionListener(e -> SwingUtil.exceptionWrapper(this::onAnadirGrado));
		view.getBtnAnadirCurso().addActionListener(e -> SwingUtil.exceptionWrapper(this::onAnadirCurso));
		view.getBtnModificarGrado().addActionListener(e -> SwingUtil.exceptionWrapper(this::onModificarGrado));
		view.getBtnEliminarGrado().addActionListener(e -> SwingUtil.exceptionWrapper(this::onEliminarGrado));
		view.getBtnModificarCurso().addActionListener(e -> SwingUtil.exceptionWrapper(this::onModificarCurso));
		view.getBtnEliminarCurso().addActionListener(e -> SwingUtil.exceptionWrapper(this::onEliminarCurso));
	}

	/** Cierra la ventana de Secretaría y vuelve a la pantalla de selección de rol. */
	private void volverInicio() {
		view.getFrame().dispose();
		onVolverInicio.run();
	}

	private void onAnadirGrado() {
		GradoFormDialog form = new GradoFormDialog(view.getFrame());
		form.getBtnGuardar().addActionListener(e -> SwingUtil.exceptionWrapper(() -> {
			String nombre = form.getTxtNombre().getText();
			gradoRepository.crearGrado(nombre, form.getTxtFacultad().getText(), form.getTxtRama().getText(),
					getAnioActualId());
			form.cerrar();
			JOptionPane.showMessageDialog(view.getFrame(), "Grado " + nombre.trim() + " añadido correctamente.",
					"Añadir grado", JOptionPane.INFORMATION_MESSAGE);
			cargarGrados();
		}));
		form.mostrar();
	}

	private void onAnadirCurso() {
		CursoFormDialog form = new CursoFormDialog(view.getFrame());
		cargarCoordinadores(form);
		form.getBtnGuardar().addActionListener(e -> SwingUtil.exceptionWrapper(() -> {
			String nombre = form.getTxtNombre().getText();
			cursoRepository.crearCurso(gradoActualId, nombre, form.getTxtCreditos().getText(),
					form.getTxtAnio().getText(), extraerProfesorId((String) form.getCbCoordinador().getSelectedItem()),
					form.getTxtItinerario().getText());
			form.cerrar();
			JOptionPane.showMessageDialog(view.getFrame(), "Curso " + nombre.trim() + " añadido correctamente.",
					"Añadir curso", JOptionPane.INFORMATION_MESSAGE);
			cargarCursosDeGrado(gradoActualId, gradoActualNombre);
		}));
		form.mostrar();
	}

	private void onModificarGrado() {
		GradoFormDialog form = new GradoFormDialog(view.getFrame(), "Modificar grado");
		for (GradoDisplayDTO grado : todosLosGrados) {
			if (grado.getId() != null && grado.getId().equals(gradoActualId)) {
				form.getTxtNombre().setText(grado.getNombre());
				form.getTxtFacultad().setText(grado.getFacultad());
				form.getTxtRama().setText(grado.getRama());
			}
		}
		form.getBtnGuardar().addActionListener(e -> SwingUtil.exceptionWrapper(() -> {
			String nombre = form.getTxtNombre().getText();
			gradoRepository.modificarGrado(gradoActualId, nombre, form.getTxtFacultad().getText(),
					form.getTxtRama().getText());
			form.cerrar();
			JOptionPane.showMessageDialog(view.getFrame(), "Grado " + nombre.trim() + " modificado correctamente.",
					"Modificar grado", JOptionPane.INFORMATION_MESSAGE);
			gradoActualNombre = nombre.trim();
			cargarGrados();
			view.showCursosView(gradoActualNombre);
		}));
		form.mostrar();
	}

	private void onModificarCurso() {
		CursoEdicionDTO datos = cursoRepository.buscarDatosEdicion(gradoActualId, cursoActualId);
		CursoFormDialog form = new CursoFormDialog(view.getFrame(), "Modificar curso");
		cargarCoordinadores(form);
		form.getCbCoordinador().setSelectedIndex(-1);
		for (int i = 0; i < form.getCbCoordinador().getItemCount(); i++) {
			if (extraerProfesorId(form.getCbCoordinador().getItemAt(i)).equals(datos.getCoordinadorId())) {
				form.getCbCoordinador().setSelectedIndex(i);
			}
		}
		form.getTxtNombre().setText(datos.getNombre());
		form.getTxtItinerario().setText(datos.getItinerario());
		form.getTxtCreditos().setText(datos.getCreditos());
		form.getTxtAnio().setText(datos.getAnio());
		form.deshabilitarNoEditables();
		if (cursoRepository.contarGradosDelCurso(cursoActualId) > 1) {
			form.mostrarAvisoCreditos();
		}
		form.getBtnGuardar().addActionListener(e -> SwingUtil.exceptionWrapper(() -> {
			cursoRepository.modificarCurso(gradoActualId, cursoActualId, form.getTxtAnio().getText(),
					form.getTxtItinerario().getText(), form.getTxtCreditos().getText());
			form.cerrar();
			JOptionPane.showMessageDialog(view.getFrame(), "Curso " + cursoActualNombre + " modificado correctamente.",
					"Modificar curso", JOptionPane.INFORMATION_MESSAGE);
			cargarCursosDeGrado(gradoActualId, gradoActualNombre);
		}));
		form.mostrar();
	}

	private void cargarCoordinadores(CursoFormDialog form) {
		for (Object[] profesor : new ProfesorRepository().getListaProfesores()) {
			form.getCbCoordinador().addItem(String.valueOf(profesor[0]));
		}
	}

	private void onEliminarGrado() {
		String anioActualId = getAnioActualId();
		gradoRepository.validarEliminacion(gradoActualId, anioActualId);

		int confirm = JOptionPane.showConfirmDialog(view.getFrame(),
				"¿Desea eliminar el grado " + gradoActualNombre + "?\n"
						+ "El grado pasará a EN_EXTINCION y todos sus cursos a INACTIVO.",
				"Eliminar grado", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (confirm != JOptionPane.YES_OPTION) {
			return;
		}

		gradoRepository.eliminarGrado(gradoActualId, anioActualId);
		JOptionPane.showMessageDialog(view.getFrame(), "Grado " + gradoActualNombre + " eliminado correctamente.",
				"Eliminar grado", JOptionPane.INFORMATION_MESSAGE);
		cargarGrados();
		view.showGradosView();
	}

	private void onEliminarCurso() {
		String anioActualId = getAnioActualId();
		cursoRepository.validarEliminacion(gradoActualId, cursoActualId, anioActualId);

		int confirm = JOptionPane.showConfirmDialog(view.getFrame(),
				"¿Desea eliminar el curso " + cursoActualNombre + " del grado " + gradoActualNombre + "?\n"
						+ "El curso pasará a INACTIVO en este grado.",
				"Eliminar curso", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (confirm != JOptionPane.YES_OPTION) {
			return;
		}

		cursoRepository.eliminarCurso(gradoActualId, cursoActualId, anioActualId);
		JOptionPane.showMessageDialog(view.getFrame(), "Curso " + cursoActualNombre + " eliminado correctamente.",
				"Eliminar curso", JOptionPane.INFORMATION_MESSAGE);
		cargarCursosDeGrado(gradoActualId, gradoActualNombre);
		view.showCursosView(gradoActualNombre);
	}

	private String getAnioActualId() {
		return anoAcademicoActual != null ? anoAcademicoActual.getAnioAcademicoId() : null;
	}

	private String extraerProfesorId(String profesor) {
		if (profesor == null) {
			return null;
		}
		int separador = profesor.indexOf(" - ");
		return separador >= 0 ? profesor.substring(0, separador) : profesor;
	}

	public void cargarAnoAcademicoActual() {
		if (anoAcademicoRepository != null) {
			this.anoAcademicoActual = anoAcademicoRepository.getAnoAcademicoActual();
			if (anoAcademicoActual != null) {
				view.actualizarAnoAcademico(anoAcademicoActual.getAnoAcademicoDisplay());
				view.actualizarPeriodoMatriculacion(anoAcademicoActual.getPeriodoMatriculacionDisplay());
			}
		}
	}

	private void onNuevoAnoAcademico() {
		anoAcademicoRepository.validarAnoAnteriorTerminado();

		AnoAcademicoFormDialog form = new AnoAcademicoFormDialog(view.getFrame());
		form.getBtnGuardar().addActionListener(e -> SwingUtil.exceptionWrapper(() -> {
			Date inicioAnio = parsearFechaInput(form.getTxtInicioAnio().getText());
			Date finAnio = parsearFechaInput(form.getTxtFinAnio().getText());
			Date inicioMatriculacion = parsearFechaInput(form.getTxtInicioMatriculacion().getText());
			Date finMatriculacion = parsearFechaInput(form.getTxtFinMatriculacion().getText());

			String anioId = anoAcademicoRepository.validarNuevoAnoAcademico(inicioAnio, finAnio, inicioMatriculacion,
					finMatriculacion);

			int confirm = JOptionPane.showConfirmDialog(form.getDialog(),
					"Se configurará el año académico " + anioId + ", que pasará a ser el actual.\n"
							+ "Los grados con la vigencia caducada pasarán a PENDIENTE_RENOVACION\n"
							+ "y los cursos de grados en extinción a INACTIVO. ¿Desea continuar?",
					"Configurar Nuevo Año Académico", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (confirm != JOptionPane.YES_OPTION) {
				return;
			}

			anoAcademicoRepository.iniciarNuevoAnoAcademico(inicioAnio, finAnio, inicioMatriculacion, finMatriculacion);
			form.cerrar();
			JOptionPane.showMessageDialog(view.getFrame(), "Año académico " + anioId + " configurado correctamente.",
					"Configurar Nuevo Año Académico", JOptionPane.INFORMATION_MESSAGE);
			cargarAnoAcademicoActual();
			cargarGrados();
			view.showGradosView();
		}));
		form.mostrar();
	}

	private Date parsearFechaInput(String text) {
		if (text == null)
			return null;
		String clean = text.trim();
		if (clean.isEmpty() || clean.contains("_"))
			return null;
		String[] formatos = new String[] { "dd/MM/yy", "dd/MM/yyyy", "yyyy-MM-dd" };
		for (String f : formatos) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat(f);
				sdf.setLenient(false);
				return sdf.parse(clean);
			} catch (ParseException ignored) {
			}
		}
		return null;
	}

	public AnoAcademico getAnoAcademicoActual() {
		return this.anoAcademicoActual;
	}

	public void cargarGrados() {
		todosLosGrados.clear();
		try {
			if (gradoRepository != null) {
				todosLosGrados = gradoRepository.getTodosLosGrados();
			}
		} catch (Exception ex) {
			System.err.println("Aviso al cargar grados desde repositorio: " + ex.getMessage());
		}

		aplicarFiltrosGrados();
	}

	public void cargarMocksGrados() {
		cargarGrados();
	}

	public void cargarCursosDeGrado(String idGrado, String nombreGrado) {
		this.gradoActualId = idGrado;
		this.gradoActualNombre = nombreGrado;
		todosLosCursos.clear();
		try {
			if (cursoRepository != null) {
				todosLosCursos = cursoRepository.getCursosPorGrado(idGrado);
			}
		} catch (Exception ex) {
			System.err.println("Aviso al cargar cursos desde repositorio: " + ex.getMessage());
		}

		aplicarFiltrosCursos();
	}

	public void cargarMatriculasDeCurso(String idCurso, String nombreCurso) {
		this.cursoActualId = idCurso;
		this.cursoActualNombre = nombreCurso;
		todasLasMatriculaciones.clear();
		try {
			if (matriculacionRepository != null) {
				if (anoAcademicoActual != null) {
					todasLasMatriculaciones = matriculacionRepository.getMatriculacionesPorCursoYAnoActual(
							idCurso, 
							anoAcademicoActual.getAnioAcademicoId()
					);
				} else {
					todasLasMatriculaciones = matriculacionRepository.getMatriculacionesPorCurso(idCurso);
				}
			}
		} catch (Exception ex) {
			System.err.println("Aviso al cargar matrículas desde repositorio: " + ex.getMessage());
		}

		Long idAnoActual = anoAcademicoActual != null ? anoAcademicoActual.getId() : 1L;
		for (MatriculacionDisplayDTO m : todasLasMatriculaciones) {
			if (m.getIdAnoAcademico() == null) {
				m.setIdAnoAcademico(idAnoActual);
			}
		}

		aplicarFiltrosMatriculacion();
	}

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
				.filter(g -> filtroFacultad.isEmpty()
						|| (g.getFacultad() != null && g.getFacultad().toLowerCase().contains(filtroFacultad)))
				.filter(g -> filtroRama.isEmpty()
						|| (g.getRama() != null && g.getRama().toLowerCase().contains(filtroRama)))
				.filter(g -> filtroEstado == null || "TODOS".equalsIgnoreCase(filtroEstado)
						|| (g.getEstado() != null && g.getEstado().equalsIgnoreCase(filtroEstado)))
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
				.filter(c -> filtroAno.isEmpty()
						|| (c.getAno() != null && c.getAno().toLowerCase().contains(filtroAno)))
				.filter(c -> filtroItinerario.isEmpty()
						|| (c.getItinerario() != null && c.getItinerario().toLowerCase().contains(filtroItinerario)))
				.filter(c -> filtroProfesor.isEmpty()
						|| (c.getProfesor() != null && c.getProfesor().toLowerCase().contains(filtroProfesor)))
				.filter(c -> filtroEstado == null || "TODOS".equalsIgnoreCase(filtroEstado)
						|| (c.getEstado() != null && c.getEstado().equalsIgnoreCase(filtroEstado)))
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

	private void initView() {
		view.showGradosView();
		view.getFrame().setVisible(true);
	}
}





