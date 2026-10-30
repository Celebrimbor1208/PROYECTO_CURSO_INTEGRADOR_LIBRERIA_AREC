package libreriaarec;

import libreriaarec.reabastecimiento.PanelReabastecimiento;
import libreriaarec.reabastecimiento.PanelSeguimiento;
import libreriaarec.reabastecimiento.Solicitud;
import libreriaarec.venta.PanelVenta;
import libreriaarec.inventario.PanelInventario;
import libreriaarec.usuarios.PanelUsuarios;
import libreriaarec.compra.PanelOrdenCompra;



import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Ventana principal. Contiene todos los paneles y cambia entre ellos con CardLayout.
 * Ejecuta esta clase (Run File) para probar el flujo completo.
 */
public class VentanaPrincipal extends JFrame {

    // Nombres de cada pantalla
    public static final String BIENVENIDA = "bienvenida";
    public static final String LOGIN = "login";
    public static final String MENU = "menu";
    public static final String VENTA = "venta";
    public static final String INVENTARIO = "inventario";
    public static final String USUARIOS = "usuarios";
    public static final String COMPRA = "compra";
    public static final String REABASTECIMIENTO = "reabastecimiento";
    public static final String CRECIMIENTO = "crecimiento";
    public static final String SEGUIMIENTO = "seguimiento";

    private final CardLayout cartas = new CardLayout();
    private final JPanel contenedor = new JPanel(cartas);

    // Solicitudes de abastecimiento (en memoria; luego se guardarán en la base de datos)
    private final List<Solicitud> solicitudes = new ArrayList<>();

    public VentanaPrincipal() {
        setTitle("Librerías AREC");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(900, 600));

        contenedor.add(new PanelBienvenida(this), BIENVENIDA);
        contenedor.add(new PanelLogin(this), LOGIN);
        contenedor.add(new PanelMenu(this), MENU);

        // Módulos de ejemplo (reemplázalos por tus paneles reales cuando los desarrolles)
        contenedor.add(crearPlaceholder("MÓDULO DE VENTA"), VENTA);
        contenedor.add(crearPlaceholder("MÓDULO DE INVENTARIO Y ALERTAS"), INVENTARIO);
        contenedor.add(crearPlaceholder("MÓDULO DE REGISTRO DE USUARIO/ADM"), USUARIOS);
        contenedor.add(crearPlaceholder("MÓDULO DE ORDEN DE COMPRA"), COMPRA);
        contenedor.add(new PanelReabastecimiento(this), REABASTECIMIENTO);
        contenedor.add(new PanelSeguimiento(this), SEGUIMIENTO);
        contenedor.add(new PanelVenta(this), VENTA);
        contenedor.add(new PanelInventario(this), INVENTARIO);
        contenedor.add(new PanelUsuarios(this), USUARIOS);
        contenedor.add(new PanelOrdenCompra(this), COMPRA);
        contenedor.add(crearPlaceholder("MÓDULO DE CRECIMIENTO"), CRECIMIENTO);

        add(contenedor);
        pack();
        setLocationRelativeTo(null);
        mostrar(BIENVENIDA);
    }

    public List<Solicitud> getSolicitudes() {
        return solicitudes;
    }

    public void mostrar(String nombre) {
        cartas.show(contenedor, nombre);
    }

    private JPanel crearPlaceholder(String titulo) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel l = new JLabel(titulo + " (en construcción)", SwingConstants.CENTER);
        l.setFont(new Font("Serif", Font.BOLD, 24));
        JButton volver = new JButton("VOLVER AL MENÚ");
        volver.addActionListener(e -> mostrar(MENU));
        p.add(l, BorderLayout.CENTER);
        p.add(volver, BorderLayout.SOUTH);
        return p;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }
}
