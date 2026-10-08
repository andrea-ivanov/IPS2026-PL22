package controller;

import view.SecretarioView;
import giis.demo.util.SwingUtil;
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
        view.getBtnCargarMatriculas().addActionListener(e -> cargarMatriculas());

        view.getTabMatriculas().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                String idSeleccionado = SwingUtil.getSelectedKey(view.getTabMatriculas());
                if (!"".equals(idSeleccionado)) {
                    System.out.println("Matrícula seleccionada ID: " + idSeleccionado);
                    // TODO: Aquí abrirás la ventana de gestión del ciclo de vida 
                    // (Aceptar, Modificar o Cancelar con motivos: IMPAGO, FUERZA_MAYOR, INCOMPATIBILIDAD)[cite: 1]
                }
            }
        });
    }

    private void cargarMatriculas() {
        // Aquí puedes meter mocks o conectar con tu modelo de entidades para listar las matrículas y alumnos
        List<Object> mockData = new ArrayList<>(); 
        // TableModel tmodel = SwingUtil.getTableModelFromPojos(mockData, new String[]{"id", "estudiante", "grado", "estado"});
        // view.getTabMatriculas().setModel(tmodel);
        // SwingUtil.autoAdjustColumns(view.getTabMatriculas());
    }

    private void initView() {
        view.getFrame().setVisible(true);
    }
}