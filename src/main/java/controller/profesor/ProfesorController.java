package controller.profesor;

import model.AsignacionDocenteDisplayDTO;
import repository.ProfesorRepository;
import util.SwingUtil;
import view.profesor.ProfesorView;

import javax.swing.table.TableModel;
import java.util.List;

public class ProfesorController {
    private ProfesorView view;
    private ProfesorRepository repository;

    public ProfesorController(ProfesorView view) {
        this.view = view;
        this.repository = new ProfesorRepository();
        this.initView();
    }

    public void initController() {
        view.getBtnCargarAsignaciones().addActionListener(e -> SwingUtil.exceptionWrapper(() -> cargarAsignaciones()));
    }

    private void cargarAsignaciones() {
        Object seleccionado = view.getCbProfesores().getSelectedItem();
        if (seleccionado == null)
            return;
        // el item del combo es "P001 - Nombre Apellidos": el id es lo que va antes del separador
        String profesorId = seleccionado.toString().split(" - ")[0];

        List<AsignacionDocenteDisplayDTO> asignaciones = repository.getAsignacionesDocentes(profesorId);
        TableModel tmodel = SwingUtil.getTableModelFromPojos(asignaciones,
                new String[] { "cursoId", "curso", "grado", "grupoCodigo", "tipo", "rol" });
        view.getTabAsignaciones().setModel(tmodel);
        SwingUtil.autoAdjustColumns(view.getTabAsignaciones());
    }

    private void initView() {
        view.getCbProfesores().setModel(SwingUtil.getComboModelFromList(repository.getListaProfesores()));
        view.getFrame().setVisible(true);
    }
}
