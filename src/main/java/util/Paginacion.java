package util;

/**
 * Estado de la paginación de un listado: página actual (empezando en 1), filas por página y total de filas.
 * La página actual se ajusta sola para estar siempre entre 1 y el número de páginas.
 */
public class Paginacion {
	private final int filasPorPagina;
	private int pagina = 1;
	private int totalFilas = 0;

	public Paginacion(int filasPorPagina) {
		if (filasPorPagina <= 0) {
			throw new IllegalArgumentException("Las filas por página deben ser positivas");
		}
		this.filasPorPagina = filasPorPagina;
	}

	public int getFilasPorPagina() { return filasPorPagina; }
	public int getPagina() { return pagina; }
	public int getTotalFilas() { return totalFilas; }

	/** Número de páginas; al menos 1 aunque no haya filas. */
	public int getTotalPaginas() {
		return Math.max(1, (totalFilas + filasPorPagina - 1) / filasPorPagina);
	}

	/** Fija el total de filas (p. ej. tras filtrar) y recoloca la página actual si se ha quedado fuera. */
	public void setTotalFilas(int totalFilas) {
		this.totalFilas = Math.max(0, totalFilas);
		irA(pagina);
	}

	public void irA(int pagina) {
		this.pagina = Math.min(Math.max(1, pagina), getTotalPaginas());
	}

	public void primera() { irA(1); }
	public void anterior() { irA(pagina - 1); }
	public void siguiente() { irA(pagina + 1); }
	public void ultima() { irA(getTotalPaginas()); }

	public boolean hayAnterior() { return pagina > 1; }
	public boolean haySiguiente() { return pagina < getTotalPaginas(); }

	/** Primera fila de la página actual (desde 0), para el OFFSET de la consulta. */
	public int getOffset() {
		return (pagina - 1) * filasPorPagina;
	}

	/** Posición (desde 1) de la primera fila mostrada, o 0 si no hay filas. */
	public int getDesde() {
		return totalFilas == 0 ? 0 : getOffset() + 1;
	}

	/** Posición (desde 1) de la última fila mostrada. */
	public int getHasta() {
		return Math.min(getOffset() + filasPorPagina, totalFilas);
	}
}
