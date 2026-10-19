package view.secretario;

import util.Paginacion;
import view.comun.Estilo;

import javax.swing.*;
import java.awt.*;

/**
 * Controles de paginación de un listado: primera, anterior, "Página [n] de N", siguiente y última.
 * En el campo de la página se puede escribir un número y pulsar Intro para ir a esa página.
 */
public class PanelPaginacion extends JPanel {
    private final JButton btnPrimera = Estilo.botonSecundario("«");
    private final JButton btnAnterior = Estilo.botonSecundario("‹ Anterior");
    private final JButton btnSiguiente = Estilo.botonSecundario("Siguiente ›");
    private final JButton btnUltima = Estilo.botonSecundario("»");
    private final JTextField txtPagina = new JTextField("1", 3);
    private final JLabel lblTotalPaginas = new JLabel("de 1");

    public PanelPaginacion() {
        super(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        setOpaque(false);

        btnPrimera.setToolTipText("Primera página");
        btnUltima.setToolTipText("Última página");
        txtPagina.setHorizontalAlignment(SwingConstants.CENTER);
        txtPagina.setToolTipText("Escriba un número de página y pulse Intro");
        Estilo.estilizarCampo(txtPagina);
        lblTotalPaginas.setFont(Estilo.NORMAL);
        lblTotalPaginas.setForeground(Estilo.TEXTO_SECUNDARIO);
        JLabel lblPagina = new JLabel("Página");
        lblPagina.setFont(Estilo.NORMAL);
        lblPagina.setForeground(Estilo.TEXTO_SECUNDARIO);

        add(btnPrimera);
        add(btnAnterior);
        add(Box.createHorizontalStrut(6));
        add(lblPagina);
        add(txtPagina);
        add(lblTotalPaginas);
        add(Box.createHorizontalStrut(6));
        add(btnSiguiente);
        add(btnUltima);
    }

    /**
     * Mueve la paginación con los botones y el campo de página, y llama a recargar después de cada cambio.
     */
    public void conectar(Paginacion paginacion, Runnable recargar) {
        btnPrimera.addActionListener(e -> { paginacion.primera(); recargar.run(); });
        btnAnterior.addActionListener(e -> { paginacion.anterior(); recargar.run(); });
        btnSiguiente.addActionListener(e -> { paginacion.siguiente(); recargar.run(); });
        btnUltima.addActionListener(e -> { paginacion.ultima(); recargar.run(); });
        txtPagina.addActionListener(e -> {
            try {
                paginacion.irA(Integer.parseInt(txtPagina.getText().trim()));
            } catch (NumberFormatException ex) {
                // Si no es un número se queda en la página actual
            }
            recargar.run();
        });
    }

    /** Muestra la página actual y habilita solo los botones que llevan a otra página. */
    public void actualizar(Paginacion paginacion) {
        txtPagina.setText(String.valueOf(paginacion.getPagina()));
        lblTotalPaginas.setText("de " + paginacion.getTotalPaginas());
        btnPrimera.setEnabled(paginacion.hayAnterior());
        btnAnterior.setEnabled(paginacion.hayAnterior());
        btnSiguiente.setEnabled(paginacion.haySiguiente());
        btnUltima.setEnabled(paginacion.haySiguiente());
        txtPagina.setEnabled(paginacion.getTotalPaginas() > 1);
    }
}
