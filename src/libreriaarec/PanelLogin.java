package libreriaarec;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import libreriaarec.controlador.AutenticacionController;
import libreriaarec.controlador.AutenticacionController.Resultado;

/**
 * Vista de inicio de sesión (código de usuario = DNI, y contraseña).
 * La validación la hace {@link AutenticacionController}; esta clase solo muestra la pantalla.
 */
public class PanelLogin extends PanelConFondo {

    private final JTextField txtUsuario = new JTextField(18);
    private final JPasswordField txtClave = new JPasswordField(18);
    private final VentanaPrincipal ventana;
    private final AutenticacionController autenticacion;

    public PanelLogin(VentanaPrincipal ventana, AutenticacionController autenticacion) {
        super("/libreriaarec/imagenes/fondo2.jpg");
        this.ventana = ventana;
        this.autenticacion = autenticacion;
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.insets = new Insets(8, 8, 8, 8);

        Font fuente = new Font("Serif", Font.BOLD, 20);

        JLabel lblUsuario = new JLabel("CÓDIGO DE USUARIO (DNI)");
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
        btnEntrar.addActionListener(e -> validar());
        c.gridy = 4;
        add(btnEntrar, c);

        JButton btnVolver = new JButton("VOLVER");
        btnVolver.addActionListener(e -> {
            limpiar();
            ventana.mostrar(VentanaPrincipal.BIENVENIDA);
        });
        c.gridy = 5;
        add(btnVolver, c);

        // Presionar Enter en la contraseña también entra
        txtClave.addActionListener(e -> validar());
    }

    private void validar() {
        char[] clave = txtClave.getPassword();
        Resultado r;
        try {
            r = autenticacion.iniciarSesion(txtUsuario.getText(), clave);
        } finally {
            Arrays.fill(clave, '\0'); // no dejar la contraseña en memoria
        }

        switch (r.getEstado()) {
            case CORRECTO:
                limpiar();
                ventana.iniciarSesion(r.getEmpleado());
                break;
            case BLOQUEADO:
                JOptionPane.showMessageDialog(this,
                        "Demasiados intentos fallidos.\nEspera " + r.getSegundosRestantes()
                        + " segundo(s) antes de volver a intentar.",
                        "Acceso bloqueado temporalmente", JOptionPane.WARNING_MESSAGE);
                txtClave.setText("");
                break;
            default:
                JOptionPane.showMessageDialog(this, "Código de usuario o contraseña incorrectos",
                        "Error de acceso", JOptionPane.ERROR_MESSAGE);
                txtClave.setText("");
                break;
        }
    }

    public void limpiar() {
        txtUsuario.setText("");
        txtClave.setText("");
    }
}
