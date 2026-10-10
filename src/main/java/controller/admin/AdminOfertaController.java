package controller.admin;

import view.admin.AdminOfertaView;
import giis.demo.util.SwingUtil;
import model.grado.GradoDisplayDTO;

import javax.swing.table.TableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class AdminOfertaController {
    private AdminOfertaView view;

    public AdminOfertaController(AdminOfertaView view) {
        this.view = view;
        this.initView();
    }

    public void initController() {
        view.getBtnCargarMocks().addActionListener(e -> cargarMocksGrados());

        view.getTabGrados().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                String idSeleccionado = SwingUtil.getSelectedKey(view.getTabGrados());
                if (!"".equals(idSeleccionado)) {
                    System.out.println("Grado seleccionado ID: " + idSeleccionado);
                    System.out.println("--> TODO: Aquí el gestor de ventanas abrirá la vista de detalles/cursos.");
                }
            }
        });
    }

    private void cargarMocksGrados() {
        List<GradoDisplayDTO> mockData = new ArrayList<>();
        mockData.add(new GradoDisplayDTO("1", "Ingeniería Informática", "Ciencias", "VIGENTE"));
        mockData.add(new GradoDisplayDTO("2", "Matemáticas", "Ciencias Básicas", "EN_EXTINCION"));

        TableModel tmodel = SwingUtil.getTableModelFromPojos(mockData,
                new String[] { "id", "nombre", "facultad", "estado" });
        view.getTabGrados().setModel(tmodel);
        SwingUtil.autoAdjustColumns(view.getTabGrados());
    }

    private void initView() {
        view.getFrame().setVisible(true);
    }
}