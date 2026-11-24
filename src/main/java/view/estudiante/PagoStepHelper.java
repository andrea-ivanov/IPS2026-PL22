package view.estudiante;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import view.comun.Estilo;

/**
 * Utilidades visuales compartidas exclusivamente por las vistas de matriculación
 * del estudiante (grado y microcredencial) en el paso de selección de pago.
 */
final class PagoStepHelper {

    private PagoStepHelper() {
    }

    /**
     * Envuelve las opciones de método de pago dentro de un recuadro centrado.
     */
    static JPanel crearCajaOpcionesPago(JRadioButton... opciones) {
        JPanel caja = new JPanel();
        caja.setLayout(new BoxLayout(caja, BoxLayout.Y_AXIS));
        caja.setBackground(Estilo.FONDO);
        caja.setBorder(new CompoundBorder(
                new LineBorder(Estilo.BORDE, 1, true),
                new EmptyBorder(12, 20, 12, 20)));

        for (int i = 0; i < opciones.length; i++) {
            opciones[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            caja.add(opciones[i]);
            if (i < opciones.length - 1) {
                caja.add(Box.createVerticalStrut(8));
            }
        }

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrapper.add(caja);
        return wrapper;
    }

    /**
     * Crea una sub-tarjeta compacta para mostrar las reglas de un método de pago.
     */
    static JPanel crearTarjetaReglaPago(String titulo, String detalleHtml) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Estilo.TARJETA);
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(206, 224, 250), 1, true),
                new EmptyBorder(8, 10, 8, 10)));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Tahoma", Font.BOLD, 12));
        lblTitulo.setForeground(Estilo.PRIMARIO);
        card.add(lblTitulo, BorderLayout.NORTH);

        JLabel lblDetalle = new JLabel("<html><div style='width:215px;'>" + detalleHtml + "</div></html>");
        lblDetalle.setFont(new Font("Tahoma", Font.PLAIN, 11));
        lblDetalle.setForeground(Estilo.TEXTO);
        lblDetalle.setVerticalAlignment(SwingConstants.TOP);
        card.add(lblDetalle, BorderLayout.CENTER);

        return card;
    }
}
