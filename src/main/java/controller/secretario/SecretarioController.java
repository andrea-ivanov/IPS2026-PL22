package controller.secretario;

import model.MatriculaDisplayDTO;
import view.secretario.SecretarioView;
import giis.demo.util.SwingUtil;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableColumn;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class SecretarioController {
    private SecretarioView view;

    public SecretarioController(SecretarioView view) {
        this.view = view;
        this.initView();
    }

   public void initController() {
        // Únicamente filtramos y cargamos cuando el usuario pulsa el botón
        view.getBtnCargarMatriculas().addActionListener(e -> cargarMatriculasFiltradas());

        // Evento al hacer clic en la tabla (para la futura gestión de la matrícula)
        view.getTabMatriculas().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                String idSeleccionado = SwingUtil.getSelectedKey(view.getTabMatriculas());
                if (!"".equals(idSeleccionado)) {
                    //System.out.println("Matrícula seleccionada ID: " + idSeleccionado);
                    // TODO: Aquí abrirás la ventana de gestión del ciclo de vida 
                    // (Aceptar, Modificar o Cancelar con motivos: IMPAGO, FUERZA_MAYOR, INCOMPATIBILIDAD)
                }
            }
        });
    }

    // Método que filtra y carga los datos en la tabla
    private void cargarMatriculasFiltradas() {
        String estadoSeleccionado = (String) view.getCbFiltroEstado().getSelectedItem();
        
        // Simulación de datos (en el futuro esto vendrá de tu Base de Datos)
        List<MatriculaDisplayDTO> listaCompleta = obtenerTodasLasMatriculasDeBD();
        List<MatriculaDisplayDTO> listaFiltrada = new ArrayList<>();

        // Lógica de filtrado por estado
        for (MatriculaDisplayDTO m : listaCompleta) {
            if ("TODOS".equals(estadoSeleccionado) || m.getEstado().equalsIgnoreCase(estadoSeleccionado)) {
                listaFiltrada.add(m);
            }
        }

        // Generar modelo de tabla
        TableModel tmodel = SwingUtil.getTableModelFromPojos(listaFiltrada, new String[]{"id", "estudiante", "grado", "estado"});
        view.getTabMatriculas().setModel(tmodel);
        
        // REQUISITO: Permitir ordenar haciendo clic en las cabeceras (ascendente/descendente/alfabética)
        view.getTabMatriculas().setAutoCreateRowSorter(true);
        
        // Ajustar ancho de la tabla a la ventana
        view.getTabMatriculas().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        view.getTabMatriculas().setFillsViewportHeight(true);

        // REQUISITO: Convertir las columnas de Grado y Estado en JComboBox (desplegables)
        configurarCeldasDesplegables();
    }

    // Método auxiliar para hacer que las celdas de Grado y Estado sean desplegables
    private void configurarCeldasDesplegables() {
        if (view.getTabMatriculas().getColumnCount() > 3) {
            // Columna de Estado (índice 3)
            TableColumn columnaEstado = view.getTabMatriculas().getColumnModel().getColumn(3);
            JComboBox<String> cbEstadosCelda = new JComboBox<>(new String[]{"SOLICITUD", "PAGADA", "ACTIVA", "FINALIZADA", "CANCELADA"});
            columnaEstado.setCellEditor(new DefaultCellEditor(cbEstadosCelda));

            // Columna de Grado (índice 2)
            TableColumn columnaGrado = view.getTabMatriculas().getColumnModel().getColumn(2);
            JComboBox<String> cbGradosCelda = new JComboBox<>(new String[]{"Ingeniería Informática", "Matemáticas", "Física"});
            columnaGrado.setCellEditor(new DefaultCellEditor(cbGradosCelda));
        }
    }

    // Simulación de conexión a Base de Datos (reemplazable en el futuro por tu DAO/BD real)
    private List<MatriculaDisplayDTO> obtenerTodasLasMatriculasDeBD() {
        List<MatriculaDisplayDTO> datos = new ArrayList<>();
        datos.add(new MatriculaDisplayDTO("101", "Alejandro El Harchi", "Ingeniería Informática", "SOLICITUD"));
        datos.add(new MatriculaDisplayDTO("102", "Lucía Gómez", "Matemáticas", "PAGADA"));
        datos.add(new MatriculaDisplayDTO("103", "Carlos Pérez", "Ingeniería Informática", "ACTIVA"));
        datos.add(new MatriculaDisplayDTO("104", "Sofía Martín", "Matemáticas", "CANCELADA"));
        return datos;
    }

    private void initView() {
        view.getFrame().setVisible(true);
    }
}