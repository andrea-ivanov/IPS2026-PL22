package repository;

import java.util.Date;

import model.anoacademico.AnoAcademico;
import model.anoacademico.AnoAcademicoModel;
import util.Database;

/**
 * Repositorio para la gestión y persistencia del Año Académico.
 * Proporciona acceso directo a la base de datos.
 */
public class AnoAcademicoRepository {
    private final AnoAcademicoModel anoAcademicoModel;
    private Database db;

    public AnoAcademicoRepository() {
        AnoAcademicoModel model = null;
        try {
            this.db = new Database();
            model = new AnoAcademicoModel(this.db);
        } catch (Exception ex) {
            model = new AnoAcademicoModel();
        }
        this.anoAcademicoModel = model;
    }

    public AnoAcademicoRepository(Database db) {
        this.db = db;
        this.anoAcademicoModel = new AnoAcademicoModel(db);
    }

    public AnoAcademicoRepository(AnoAcademicoModel model) {
        this.anoAcademicoModel = model;
    }

    /**
     * Obtiene el año académico actual directamente desde la base de datos.
     */
    public AnoAcademico getAnoAcademicoActual() {
        return anoAcademicoModel.buscarActual();
    }

    /**
     * Actualiza las fechas del año académico actual en la base de datos.
     */
    public void actualizarFechasAnoAcademico(Date inicio, Date fin) {
        AnoAcademico actual = getAnoAcademicoActual();
        if (actual != null) {
            String anioId = actual.getAnioAcademicoId();
            if (anioId != null && !anioId.isEmpty()) {
                anoAcademicoModel.actualizarFechasAnoAcademico(anioId, inicio, fin);
            } else if (actual.getId() != null) {
                anoAcademicoModel.actualizarFechasAnoAcademico(actual.getId(), inicio, fin);
            }
        }
    }

    /**
     * Actualiza las fechas del periodo de matriculación del año académico actual en la base de datos.
     */
    public void actualizarFechasMatriculacion(Date inicio, Date fin) {
        AnoAcademico actual = getAnoAcademicoActual();
        if (actual != null) {
            String anioId = actual.getAnioAcademicoId();
            if (anioId != null && !anioId.isEmpty()) {
                anoAcademicoModel.actualizarFechasMatriculacion(anioId, inicio, fin);
            } else if (actual.getId() != null) {
                anoAcademicoModel.actualizarFechasMatriculacion(actual.getId(), inicio, fin);
            }
        }
    }

    public AnoAcademico findById(String id) {
        return anoAcademicoModel.buscarPorId(id);
    }

    public AnoAcademico findById(Long id) {
        return anoAcademicoModel.buscarPorId(id);
    }
}