package libreriaarec;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JPanel;

/** Panel 3: menú principal con los 6 módulos y el botón SALIR. */
public class PanelMenu extends PanelConFondo {

    public PanelMenu(VentanaPrincipal ventana) {
        super("/libreriaarec/imagenes/fondo2.jpg");
        setLayout(new GridBagLayout());

        JPanel rejilla = new JPanel(new GridLayout(2, 3, 15, 15));
        rejilla.setOpaque(false);

        rejilla.add(crearModulo("MÓDULO DE VENTA", "venta.png", VentanaPrincipal.VENTA, ventana));
        rejilla.add(crearModulo("MÓDULO DE INVENTARIO Y ALERTAS", "inventario.png", VentanaPrincipal.INVENTARIO, ventana));
        rejilla.add(crearModulo("MÓDULO DE REGISTRO DE USUARIO/ADM", "usuario.png", VentanaPrincipal.USUARIOS, ventana));
        rejilla.add(crearModulo("MÓDULO DE ORDEN DE COMPRA", "compra.png", VentanaPrincipal.COMPRA, ventana));
        rejilla.add(crearModulo("MÓDULO DE REABASTECIMIENTO", "reabastecimiento.png", VentanaPrincipal.REABASTECIMIENTO, ventana));
        rejilla.add(crearModulo("MÓDULO DE CRECIMIENTO", "crecimiento.png", VentanaPrincipal.CRECIMIENTO, ventana));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(10, 10, 10, 10);
        add(rejilla, c);

        JButton btnSalir = new JButton("SALIR");
        btnSalir.addActionListener(e -> ventana.mostrar(VentanaPrincipal.LOGIN));
        c.gridy = 1;
        add(btnSalir, c);
    }

    private JButton crearModulo(String texto, String icono, String destino, VentanaPrincipal ventana) {
        JButton b = new JButton("<html><center>" + texto + "</center></html>");
        b.setIcon(Iconos.cargar(icono, 90, 90));
        b.setFont(new Font("Serif", Font.BOLD, 13));
        b.setBackground(new Color(220, 220, 220));
        b.setFocusPainted(false);
        b.setVerticalTextPosition(JButton.BOTTOM);
        b.setHorizontalTextPosition(JButton.CENTER);
        b.addActionListener(e -> ventana.mostrar(destino));
        return b;
    }
}
