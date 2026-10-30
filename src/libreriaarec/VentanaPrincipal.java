package libreriaarec;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import libreriaarec.ordencompra.PanelOrdenCompra;
import libreriaarec.controlador.AutenticacionController;
import libreriaarec.controlador.InventarioController;
import libreriaarec.controlador.Sesion;
import libreriaarec.controlador.UsuarioController;
import libreriaarec.controlador.VentaController;
import libreriaarec.dao.EmpleadoDAO;
import libreriaarec.dao.LibroDAO;
import libreriaarec.dao.VentaDAO;
import libreriaarec.dao.memoria.DatosDemo;
import libreriaarec.dao.memoria.EmpleadoDAOMemoria;
import libreriaarec.dao.memoria.LibroDAOMemoria;
import libreriaarec.dao.memoria.VentaDAOMemoria;
import libreriaarec.modelo.Empleado;
import libreriaarec.reabastecimiento.PanelReabastecimiento;
import libreriaarec.reabastecimiento.PanelSeguimiento;
import libreriaarec.reabastecimiento.Solicitud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ventana principal y raíz de composición: aquí se crean los DAO y los controladores
 * y se inyectan en las vistas (inversión de dependencias). Ejecuta esta clase para probar.
 */
public class VentanaPrincipal extends JFrame {

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

    private static final Logger LOG = LoggerFactory.getLogger(VentanaPrincipal.class);

    private final CardLayout cartas = new CardLayout();
    private final JPanel contenedor = new JPanel(cartas);
    private final Sesion sesion = new Sesion();
    private final PanelLogin panelLogin;

    // Solicitudes de abastecimiento (en memoria)
    private final List<Solicitud> solicitudes = new ArrayList<>();

    public VentanaPrincipal() {
        setTitle("Librerías AREC");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(900, 600));

        // --- Capa de datos (DAO en memoria; se puede cambiar por JDBC sin tocar los controladores) ---
        LibroDAO libros = new LibroDAOMemoria();
        EmpleadoDAO empleados = new EmpleadoDAOMemoria();
        VentaDAO ventas = new VentaDAOMemoria();
        DatosDemo.cargar(libros, empleados);

        // --- Controladores ---
        Clock reloj = Clock.systemDefaultZone();
        AutenticacionController autenticacion = new AutenticacionController(empleados, reloj);
        VentaController ventaCtrl = new VentaController(libros, ventas, reloj);
        InventarioController inventarioCtrl = new InventarioController(libros);
        UsuarioController usuarioCtrl = new UsuarioController(empleados, reloj);

        // --- Vistas ---
        panelLogin = new PanelLogin(this, autenticacion);
        contenedor.add(new PanelBienvenida(this), BIENVENIDA);
        contenedor.add(panelLogin, LOGIN);
        contenedor.add(new PanelMenu(this), MENU);
        contenedor.add(new libreriaarec.venta.PanelVenta(this, ventaCtrl), VENTA);
        contenedor.add(new libreriaarec.inventario.PanelInventario(this, inventarioCtrl), INVENTARIO);
        contenedor.add(new libreriaarec.usuarios.PanelUsuarios(this, usuarioCtrl), USUARIOS);
        contenedor.add(new PanelOrdenCompra(this), COMPRA);
        contenedor.add(new PanelReabastecimiento(this), REABASTECIMIENTO);
        contenedor.add(new PanelSeguimiento(this), SEGUIMIENTO);
        contenedor.add(crearPlaceholder("MÓDULO DE CRECIMIENTO"), CRECIMIENTO);

        add(contenedor);
        pack();
        setLocationRelativeTo(null);
        mostrar(BIENVENIDA);
        LOG.info("Sistema Librerías AREC iniciado");
    }

    public Sesion getSesion() {
        return sesion;
    }

    /** Llamado por el login cuando las credenciales son correctas. */
    public void iniciarSesion(Empleado empleado) {
        sesion.iniciar(empleado);
        mostrar(MENU);
    }

    public void cerrarSesion() {
        sesion.cerrar();
        panelLogin.limpiar();
        mostrar(LOGIN);
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
