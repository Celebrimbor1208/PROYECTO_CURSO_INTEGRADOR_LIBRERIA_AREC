package libreriaarec;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/** Panel 2: inicio de sesión (código de usuario y contraseña). */
public class PanelLogin extends PanelConFondo {

    private final JTextField txtUsuario = new JTextField(18);
    private final JPasswordField txtClave = new JPasswordField(18);

    public PanelLogin(VentanaPrincipal ventana) {
        super("/libreriaarec/imagenes/fondo2.jpg");
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.insets = new Insets(8, 8, 8, 8);

        Font fuente = new Font("Serif", Font.BOLD, 20);

        JLabel lblUsuario = new JLabel("CÓDIGO DE USUARIO");
        lblUsuario.setFont(fuente);
        lblUsuario.setForeground(Color.WHITE);
        c.gridy = 0;
        add(lblUsuario, c);

        txtUsuario.setFont(fuente);
        c.gridy = 1;
        add(txtUsuario, c);

        JLabel lblClave = new JLabel("CONTRASEÑA");
        lblClave.setFont(fuente);
        lblClave.setForeground(Color.WHITE);
        c.gridy = 2;
        add(lblClave, c);

        txtClave.setFont(fuente);
        c.gridy = 3;
        add(txtClave, c);

        JButton btnEntrar = new JButton("ENTRAR");
        btnEntrar.setBackground(new Color(255, 165, 0));
        btnEntrar.addActionListener(e -> validar(ventana));
        c.gridy = 4;
        add(btnEntrar, c);

        JButton btnVolver = new JButton("VOLVER");
        btnVolver.addActionListener(e -> ventana.mostrar(VentanaPrincipal.BIENVENIDA));
        c.gridy = 5;
        add(btnVolver, c);

        // Presionar Enter en la contraseña también entra
        txtClave.addActionListener(e -> validar(ventana));
    }

    private void validar(VentanaPrincipal ventana) {
        String usuario = txtUsuario.getText().trim();
        String clave = new String(txtClave.getPassword());

        // TODO: reemplazar por consulta a tu base de datos (JDBC).
        if (usuario.equals("admin") && clave.equals("1234")) {
            limpiar();
            ventana.mostrar(VentanaPrincipal.MENU);
        } else {
            JOptionPane.showMessageDialog(this, "Código de usuario o contraseña incorrectos",
                    "Error de acceso", JOptionPane.ERROR_MESSAGE);
            txtClave.setText("");
        }
    }

    public void limpiar() {
        txtUsuario.setText("");
        txtClave.setText("");
    }
}
