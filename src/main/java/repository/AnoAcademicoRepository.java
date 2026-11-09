package repository;

import java.time.LocalDate;
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

    public AnoAcademicoRepository() {
        this(new Database());
    }

    public AnoAcademicoRepository(Database db) {
        this.anoAcademicoModel = new AnoAcademicoModel(db);
    }

    public AnoAcademicoRepository(AnoAcademicoModel model) {
        this.anoAcademicoModel = model;
    }

    /**
     * Obtiene el año académico actual (el último configurado) desde la base de datos.
     */
    public AnoAcademico getAnoAcademicoActual() {
        return anoAcademicoModel.buscarActual();
    }

    public void validarAnoAnteriorTerminado() {
        anoAcademicoModel.validarAnoAnteriorTerminado(LocalDate.now().toString());
    }

    public String validarNuevoAnoAcademico(Date inicioAnio, Date finAnio, Date inicioMatriculacion, Date finMatriculacion) {
        return anoAcademicoModel.validarNuevoAnoAcademico(LocalDate.now().toString(), inicioAnio, finAnio,
                inicioMatriculacion, finMatriculacion);
    }

    public String iniciarNuevoAnoAcademico(Date inicioAnio, Date finAnio, Date inicioMatriculacion, Date finMatriculacion) {
        return anoAcademicoModel.iniciarNuevoAnoAcademico(LocalDate.now().toString(), inicioAnio, finAnio,
                inicioMatriculacion, finMatriculacion);
    }

}
