package libreriaarec.reabastecimiento;
import libreriaarec.PanelConFondo;
import libreriaarec.VentanaPrincipal;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Pantalla "Crear solicitud de abastecimiento":
 * se anotan los libros que faltan, la cantidad y para cuándo se necesitan.
 */
public class PanelReabastecimiento extends PanelConFondo {

    private final VentanaPrincipal ventana;
    private final List<Solicitud.Item> items = new ArrayList<>();

    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Libro", "Editorial", "Cantidad", "Precio unitario", "Subtotal"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final JLabel lblTotal = new JLabel();

    private JSpinner spTramite;
    private JSpinner spRequerida;

    public PanelReabastecimiento(VentanaPrincipal ventana) {
        super("/libreriaarec/imagenes/fondo2.jpg");
        this.ventana = ventana;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // ----- Título -----
        JLabel titulo = new JLabel("CREAR SOLICITUD DE ABASTECIMIENTO", SwingConstants.CENTER);
        titulo.setOpaque(true);
        titulo.setBackground(new Color(220, 220, 220));
        titulo.setFont(new Font("Serif", Font.BOLD, 22));
        titulo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(titulo, BorderLayout.NORTH);

        // ----- Fechas (izquierda) -----
        spTramite = Util.crearSpinnerFecha(LocalDate.now());
        spRequerida = Util.crearSpinnerFecha(LocalDate.now().plusDays(15));

        JPanel panelFechas = new JPanel(new GridLayout(0, 1, 5, 5));
        panelFechas.setBackground(Color.WHITE);
        panelFechas.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelFechas.add(new JLabel("Fecha del trámite *"));
        panelFechas.add(spTramite);
        panelFechas.add(new JLabel("Necesito los libros para el *"));
        panelFechas.add(spRequerida);

        JPanel izquierda = new JPanel(new BorderLayout());
        izquierda.setOpaque(false);
        izquierda.add(panelFechas, BorderLayout.NORTH);
        add(izquierda, BorderLayout.WEST);

        // ----- Tabla de libros (centro) -----
        tabla.setRowHeight(28);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);

        JButton btnAgregar = new JButton("+ Agregar libros");
        btnAgregar.setBackground(new Color(30, 90, 70));
        btnAgregar.setForeground(Color.WHITE);
        btnAgregar.addActionListener(e -> agregarLibro());

        JButton btnMas = new JButton("+ Cantidad");
        btnMas.addActionListener(e -> cambiarCantidad(+1));
        JButton btnMenos = new JButton("- Cantidad");
        btnMenos.addActionListener(e -> cambiarCantidad(-1));
        JButton btnQuitar = new JButton("Quitar");
        btnQuitar.addActionListener(e -> quitarLibro());

        JPanel botonesLibros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        botonesLibros.setOpaque(false);
        botonesLibros.add(btnAgregar);
        botonesLibros.add(btnMas);
        botonesLibros.add(btnMenos);
        botonesLibros.add(btnQuitar);

        lblTotal.setForeground(Color.WHITE);
        lblTotal.setFont(new Font("Serif", Font.BOLD, 16));

        JPanel centro = new JPanel(new BorderLayout(5, 5));
        centro.setOpaque(false);
        centro.add(botonesLibros, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        centro.add(lblTotal, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        // ----- Botones inferiores -----
        JButton btnSalir = new JButton("SALIR");
        btnSalir.setFont(new Font("Serif", Font.BOLD, 18));
        btnSalir.setBackground(new Color(220, 220, 220));
        btnSalir.setPreferredSize(new Dimension(200, 50));
        btnSalir.addActionListener(e -> ventana.mostrar(VentanaPrincipal.MENU));

        JButton btnSeguimiento = new JButton("VER SEGUIMIENTO");
        btnSeguimiento.setPreferredSize(new Dimension(200, 50));
        btnSeguimiento.addActionListener(e -> ventana.mostrar(VentanaPrincipal.SEGUIMIENTO));

        JButton btnGenerar = new JButton("GENERAR SOLICITUD");
        btnGenerar.setFont(new Font("Serif", Font.BOLD, 18));
        btnGenerar.setBackground(new Color(220, 220, 220));
        btnGenerar.setPreferredSize(new Dimension(240, 50));
        btnGenerar.addActionListener(e -> generarSolicitud());

        JPanel sur = new JPanel(new BorderLayout());
        sur.setOpaque(false);
        sur.add(btnSalir, BorderLayout.WEST);
        JPanel centroSur = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centroSur.setOpaque(false);
        centroSur.add(btnSeguimiento);
        sur.add(centroSur, BorderLayout.CENTER);
        sur.add(btnGenerar, BorderLayout.EAST);
        add(sur, BorderLayout.SOUTH);

        refrescarTabla(-1);
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    private void agregarLibro() {
        JTextField txtTitulo = new JTextField(20);
        JTextField txtAutor = new JTextField(20);
        JComboBox<String> cmbEditorial = new JComboBox<>(new String[]{
            "Planeta", "Penguin Random House", "Anagrama", "Siruela", "Lumen", "Alfaguara"});
        cmbEditorial.setEditable(true); // se puede escribir otra editorial
        JSpinner spCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));
        JTextField txtPrecio = new JTextField("0");

        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Título:"));
        p.add(txtTitulo);
        p.add(new JLabel("Autor:"));
        p.add(txtAutor);
        p.add(new JLabel("Editorial:"));
        p.add(cmbEditorial);
        p.add(new JLabel("Cantidad:"));
        p.add(spCantidad);
        p.add(new JLabel("Precio referencial (0 = por cotizar):"));
        p.add(txtPrecio);

        int r = JOptionPane.showConfirmDialog(this, p, "Agregar libro",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        String titulo = txtTitulo.getText().trim();
        String autor = txtAutor.getText().trim();
        String editorial = String.valueOf(cmbEditorial.getSelectedItem()).trim();
        if (titulo.isEmpty() || editorial.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El título y la editorial son obligatorios.",
                    "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double precio;
        try {
            precio = Util.leerMonto(txtPrecio.getText());
            if (precio < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El precio no es válido.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        items.add(new Solicitud.Item(titulo, autor, editorial, (Integer) spCantidad.getValue(), precio));
        refrescarTabla(items.size() - 1);
    }

    private void cambiarCantidad(int delta) {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un libro de la tabla.");
            return;
        }
        Solicitud.Item it = items.get(fila);
        if (it.cantidad + delta < 1) return;
        it.cantidad += delta;
        refrescarTabla(fila);
    }

    private void quitarLibro() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un libro de la tabla.");
            return;
        }
        items.remove(fila);
        refrescarTabla(-1);
    }

    private void generarSolicitud() {
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Agrega al menos un libro a la solicitud.",
                    "Solicitud vacía", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LocalDate tramite = Util.aLocalDate(spTramite);
        LocalDate requerida = Util.aLocalDate(spRequerida);
        if (requerida.isBefore(tramite)) {
            JOptionPane.showMessageDialog(this,
                    "La fecha en que necesitas los libros no puede ser anterior a la del trámite.",
                    "Fechas inválidas", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Solicitud s = new Solicitud(tramite, requerida, new ArrayList<>(items));
        ventana.getSolicitudes().add(s);

        JOptionPane.showMessageDialog(this,
                "Solicitud N° " + s.id + " generada.\nAhora contacta a las editoriales desde el seguimiento.",
                "Solicitud creada", JOptionPane.INFORMATION_MESSAGE);

        reiniciar();
        ventana.mostrar(VentanaPrincipal.SEGUIMIENTO);
    }

    private void reiniciar() {
        items.clear();
        spTramite.setValue(Util.aDate(LocalDate.now()));
        spRequerida.setValue(Util.aDate(LocalDate.now().plusDays(15)));
        refrescarTabla(-1);
    }

    private void refrescarTabla(int seleccionar) {
        modelo.setRowCount(0);
        double total = 0;
        for (Solicitud.Item it : items) {
            boolean cotizado = it.precioReferencial > 0;
            modelo.addRow(new Object[]{
                it.titulo + (it.autor.isEmpty() ? "" : " - " + it.autor),
                it.editorial,
                it.cantidad,
                cotizado ? Util.soles(it.precioReferencial) : "Por cotizar",
                cotizado ? Util.soles(it.subtotal()) : "-"
            });
            total += it.subtotal();
        }
        lblTotal.setText("  Total referencial: " + Util.soles(total)
                + "   (el precio final lo acuerda la editorial)");
        if (seleccionar >= 0 && seleccionar < modelo.getRowCount()) {
            tabla.setRowSelectionInterval(seleccionar, seleccionar);
        }
    }
}
