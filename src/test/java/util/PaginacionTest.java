package util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pruebas de la paginación de los listados (25 filas por página en Secretaría).
 */
class PaginacionTest {

	@Test
	void sinFilasHayUnaPaginaVacia() {
		Paginacion p = new Paginacion(25);
		assertEquals(1, p.getPagina());
		assertEquals(1, p.getTotalPaginas());
		assertEquals(0, p.getOffset());
		assertEquals(0, p.getDesde());
		assertEquals(0, p.getHasta());
		assertTrue(!p.hayAnterior());
		assertTrue(!p.haySiguiente());
	}

	@Test
	void calculaPaginasYFilasMostradas() {
		Paginacion p = new Paginacion(25);
		p.setTotalFilas(51);
		assertEquals(3, p.getTotalPaginas());
		assertEquals(1, p.getDesde());
		assertEquals(25, p.getHasta());

		p.siguiente();
		assertEquals(2, p.getPagina());
		assertEquals(25, p.getOffset());
		assertEquals(26, p.getDesde());
		assertEquals(50, p.getHasta());

		p.ultima();
		assertEquals(3, p.getPagina());
		assertEquals(51, p.getDesde());
		assertEquals(51, p.getHasta());
		assertTrue(!p.haySiguiente());

		p.primera();
		assertEquals(1, p.getPagina());
		assertTrue(!p.hayAnterior());
	}

	@Test
	void noSeSaleDeLosLimites() {
		Paginacion p = new Paginacion(25);
		p.setTotalFilas(50);
		p.anterior();
		assertEquals(1, p.getPagina());
		p.irA(99);
		assertEquals(2, p.getPagina());
		p.siguiente();
		assertEquals(2, p.getPagina());
	}

	@Test
	void alReducirseElTotalLaPaginaSeRecoloca() {
		Paginacion p = new Paginacion(25);
		p.setTotalFilas(100);
		p.ultima();
		assertEquals(4, p.getPagina());
		// Al filtrar quedan menos filas: se pasa a la última página que existe
		p.setTotalFilas(30);
		assertEquals(2, p.getPagina());
		p.setTotalFilas(0);
		assertEquals(1, p.getPagina());
	}

	@Test
	void lasFilasPorPaginaDebenSerPositivas() {
		assertThrows(IllegalArgumentException.class, () -> new Paginacion(0));
	}
}
