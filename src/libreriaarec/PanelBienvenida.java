package libreriaarec;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/** Panel 1: pantalla de bienvenida con el botón INGRESAR. */
public class PanelBienvenida extends PanelConFondo {

    public PanelBienvenida(VentanaPrincipal ventana) {
        super("/libreriaarec/imagenes/fondo1.jpg");
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.insets = new Insets(10, 10, 10, 10);

        JLabel titulo = new JLabel("LIBRERÍAS AREC", SwingConstants.CENTER);
        titulo.setFont(new Font("Serif", Font.BOLD, 36));
        titulo.setForeground(Color.WHITE);
        c.gridy = 0;
        add(titulo, c);

        JLabel logo = new JLabel(Iconos.cargar("logo.png", 150, 150));
        c.gridy = 1;
        add(logo, c);

        JButton btnIngresar = new JButton("INGRESAR");
        btnIngresar.setFont(new Font("Serif", Font.BOLD, 20));
        btnIngresar.setBackground(new Color(255, 165, 0));
        btnIngresar.setFocusPainted(false);
        btnIngresar.addActionListener(e -> ventana.mostrar(VentanaPrincipal.LOGIN));
        c.gridy = 2;
        add(btnIngresar, c);
    }
}
