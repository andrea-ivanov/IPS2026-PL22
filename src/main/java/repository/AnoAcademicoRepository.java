package repository;

import java.util.Date;

import model.anoacademico.AnoAcademico;
import model.anoacademico.AnoAcademicoModel;
import util.Database;
import util.Util;

/**
 * Repositorio para la gestión y persistencia del Año Académico.
 * Proporciona acceso a la base de datos y garantiza degradación elegante con datos
 * en memoria para entornos de pruebas o vistas desacopladas.
 */
public class AnoAcademicoRepository {
	private final AnoAcademicoModel anoAcademicoModel;
	private Database db;

	// Instancia en memoria para soportar ejecución mock/offline
	private AnoAcademico anoAcademicoActualCache;

	public AnoAcademicoRepository() {
		AnoAcademicoModel model = null;
		try {
			this.db = new Database();
			model = new AnoAcademicoModel(this.db);
		} catch (Exception ex) {
			model = new AnoAcademicoModel();
		}
		this.anoAcademicoModel = model;
		initDefaultCache();
	}

	public AnoAcademicoRepository(Database db) {
		this.db = db;
		this.anoAcademicoModel = new AnoAcademicoModel(db);
		initDefaultCache();
	}

	public AnoAcademicoRepository(AnoAcademicoModel model) {
		this.anoAcademicoModel = model;
		initDefaultCache();
	}

	private void initDefaultCache() {
		try {
			Date inicioAno = Util.isoStringToDate("2026-09-01");
			Date finAno = Util.isoStringToDate("2027-06-30");
			Date inicioMat = Util.isoStringToDate("2026-07-01");
			Date finMat = Util.isoStringToDate("2026-08-31");
			this.anoAcademicoActualCache = AnoAcademico.builder()
					.id(1L)
					.nombre("2026/2027")
					.inicioAnoAcademico(inicioAno)
					.finAnoAcademico(finAno)
					.inicioPeriodoMatriculacion(inicioMat)
					.finPeriodoMatriculacion(finMat)
					.actual(true)
					.build();
		} catch (Exception ignored) {
		}
	}

	/**
	 * Obtiene el año académico actual (desde BD o caché fallback).
	 */
	public AnoAcademico getAnoAcademicoActual() {
		try {
			AnoAcademico bdActual = anoAcademicoModel.buscarActual();
			if (bdActual != null) {
				this.anoAcademicoActualCache = bdActual;
				return bdActual;
			}
		} catch (Exception e) {
			// Si la base de datos no está disponible o la tabla no está creada, usar caché
		}
		return this.anoAcademicoActualCache;
	}

	/**
	 * Actualiza las fechas del año académico actual.
	 */
	public void actualizarFechasAnoAcademico(Date inicio, Date fin) {
		if (this.anoAcademicoActualCache != null) {
			this.anoAcademicoActualCache.setInicioAnoAcademico(inicio);
			this.anoAcademicoActualCache.setFinAnoAcademico(fin);
			try {
				if (this.anoAcademicoActualCache.getId() != null) {
					anoAcademicoModel.actualizarFechasAnoAcademico(this.anoAcademicoActualCache.getId(), inicio, fin);
				}
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * Actualiza las fechas del periodo de matriculación del año académico actual.
	 */
	public void actualizarFechasMatriculacion(Date inicio, Date fin) {
		if (this.anoAcademicoActualCache != null) {
			this.anoAcademicoActualCache.setInicioPeriodoMatriculacion(inicio);
			this.anoAcademicoActualCache.setFinPeriodoMatriculacion(fin);
			try {
				if (this.anoAcademicoActualCache.getId() != null) {
					anoAcademicoModel.actualizarFechasMatriculacion(this.anoAcademicoActualCache.getId(), inicio, fin);
				}
			} catch (Exception ignored) {
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

