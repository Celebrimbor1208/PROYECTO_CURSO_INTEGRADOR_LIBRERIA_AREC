package libreriaarec;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import libreriaarec.modelo.Empleado;

/**
 * Menú principal con los 6 módulos. El módulo de Registro de Usuario/Administrador
 * solo se puede abrir si el usuario con sesión iniciada es Administrador.
 */
public class PanelMenu extends PanelConFondo {

    private final VentanaPrincipal ventana;
    private final JLabel lblSesion = new JLabel(" ");

    public PanelMenu(VentanaPrincipal ventana) {
        super("/libreriaarec/imagenes/fondo2.jpg");
        this.ventana = ventana;
        setLayout(new GridBagLayout());

        JPanel rejilla = new JPanel(new GridLayout(2, 3, 15, 15));
        rejilla.setOpaque(false);

        rejilla.add(crearModulo("MÓDULO DE VENTA", "venta.png", VentanaPrincipal.VENTA, false));
        rejilla.add(crearModulo("MÓDULO DE INVENTARIO Y ALERTAS", "inventario.png", VentanaPrincipal.INVENTARIO, false));
        rejilla.add(crearModulo("MÓDULO DE REGISTRO DE USUARIO/ADM", "usuario.png", VentanaPrincipal.USUARIOS, true));
        rejilla.add(crearModulo("MÓDULO DE ORDEN DE COMPRA", "compra.png", VentanaPrincipal.COMPRA, false));
        rejilla.add(crearModulo("MÓDULO DE REABASTECIMIENTO", "reabastecimiento.png", VentanaPrincipal.REABASTECIMIENTO, false));
        rejilla.add(crearModulo("MÓDULO DE CRECIMIENTO", "crecimiento.png", VentanaPrincipal.CRECIMIENTO, false));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(10, 10, 10, 10);
        add(rejilla, c);

        lblSesion.setForeground(Color.WHITE);
        lblSesion.setFont(new Font("Serif", Font.BOLD, 14));
        c.gridy = 1;
        add(lblSesion, c);

        JButton btnSalir = new JButton("CERRAR SESIÓN");
        btnSalir.addActionListener(e -> ventana.cerrarSesion());
        c.gridy = 2;
        add(btnSalir, c);

        // Cada vez que se muestra el menú se actualiza quién tiene la sesión iniciada
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) { actualizarSesion(); }
        });
    }

    private void actualizarSesion() {
        Empleado actual = ventana.getSesion().actual();
        lblSesion.setText(actual == null ? " " : "Sesión: " + actual.nombreCompleto() + " (" + actual.getCargo() + ")");
    }

    private JButton crearModulo(String texto, String icono, String destino, boolean soloAdministrador) {
        JButton b = new JButton("<html><center>" + texto + "</center></html>");
        b.setIcon(Iconos.cargar(icono, 90, 90));
        b.setFont(new Font("Serif", Font.BOLD, 13));
        b.setBackground(new Color(220, 220, 220));
        b.setFocusPainted(false);
        b.setVerticalTextPosition(JButton.BOTTOM);
        b.setHorizontalTextPosition(JButton.CENTER);
        b.addActionListener(e -> {
            if (soloAdministrador && !ventana.getSesion().esAdministrador()) {
                JOptionPane.showMessageDialog(this,
                        "Acceso restringido: este módulo es solo para Administradores.",
                        "Sin permiso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            ventana.mostrar(destino);
        });
        return b;
    }
}
