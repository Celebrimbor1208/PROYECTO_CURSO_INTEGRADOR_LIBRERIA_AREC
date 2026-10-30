package libreriaarec.inventario;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import libreriaarec.PanelConFondo;
import libreriaarec.VentanaPrincipal;
import libreriaarec.controlador.InventarioController;
import libreriaarec.controlador.InventarioController.Criterio;
import libreriaarec.controlador.InventarioController.ResultadoRegistro;
import libreriaarec.controlador.ValidacionException;
import libreriaarec.modelo.Libro;
import libreriaarec.modelo.RegistroLibro;
import libreriaarec.util.Texto;

/**
 * Vista del módulo de Inventario y Alertas.
 * La lógica (tabla ordenada, buscador, filtro, registro y alertas) está en {@link InventarioController}.
 */
public class PanelInventario extends PanelConFondo {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VentanaPrincipal ventana;
    private final InventarioController controlador;
    private final List<RegistroLibro> vista = new ArrayList<>(); // lo que se ve (según buscador/filtro)

    // Formulario
    private final JTextField txtTitulo = new JTextField();
    private final JTextField txtAutor = new JTextField();
    private final JTextField txtStock = new JTextField();
    private final JTextField txtPrecio = new JTextField();
    private final JComboBox<String> cmbGenero = new JComboBox<>(new String[]{
        "Novela", "Cuento", "Poesía", "Teatro", "Ensayo", "Historia", "Biografía",
        "Infantil", "Juvenil", "Ciencia", "Autoayuda", "Otro"});
    private final JComboBox<String> cmbEditorial = new JComboBox<>(new String[]{
        "Planeta", "Penguin Random House", "Anagrama", "Siruela", "Lumen", "Alfaguara", "Cátedra"});
    private final JSpinner spFecha = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));

    // Buscador y filtrador
    private final JTextField txtBuscador = new JTextField(22);
    private final JComboBox<String> cmbCriterio = new JComboBox<>(new String[]{"Género", "Autor", "Editorial"});
    private final JTextField txtFiltro = new JTextField(14);

    // Tabla
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"N°", "Título", "Autor", "Género", "Editorial", "Fecha de registro", "Stock", "Precio"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final JLabel lblResumen = new JLabel(" ");
    private final JButton btnAlertas = new JButton();

    public PanelInventario(VentanaPrincipal ventana, InventarioController controlador) {
        super("/libreriaarec/imagenes/fondo2.jpg");
        this.ventana = ventana;
        this.controlador = controlador;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        cmbGenero.setEditable(true);
        cmbEditorial.setEditable(true);
        spFecha.setEditor(new JSpinner.DateEditor(spFecha, "dd/MM/yyyy"));
        limpiarFormulario();

        // ----- Título -----
        JLabel titulo = new JLabel("MÓDULO DE INVENTARIO Y ALERTAS", SwingConstants.CENTER);
        titulo.setOpaque(true);
        titulo.setBackground(new Color(220, 220, 220));
        titulo.setFont(new Font("Serif", Font.BOLD, 22));
        titulo.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        add(titulo, BorderLayout.NORTH);

        // ----- Formulario + botones + buscador -----
        JPanel superior = new JPanel(new BorderLayout(0, 4));
        superior.setOpaque(false);
        superior.add(crearFormulario(), BorderLayout.NORTH);
        superior.add(crearBuscador(), BorderLayout.SOUTH);

        // ----- Tabla -----
        tabla.setRowHeight(24);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        int[] anchos = {35, 230, 160, 90, 140, 110, 55, 70};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        // Al seleccionar un libro, sus datos pasan al formulario para corregirlos
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int fila = tabla.getSelectedRow();
            if (fila >= 0 && fila < vista.size()) cargarFormulario(vista.get(fila));
        });

        lblResumen.setForeground(Color.WHITE);
        lblResumen.setFont(new Font("Serif", Font.BOLD, 14));

        JPanel panelTabla = new JPanel(new BorderLayout(0, 4));
        panelTabla.setOpaque(false);
        panelTabla.add(lblResumen, BorderLayout.NORTH);
        panelTabla.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setOpaque(false);
        centro.add(superior, BorderLayout.NORTH);
        centro.add(panelTabla, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        // ----- Botones inferiores -----
        JButton btnSalir = new JButton("SALIR");
        btnSalir.setFont(new Font("Serif", Font.BOLD, 18));
        btnSalir.setBackground(new Color(220, 220, 220));
        btnSalir.setPreferredSize(new Dimension(200, 46));
        btnSalir.addActionListener(e -> salir());

        btnAlertas.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnAlertas.setPreferredSize(new Dimension(240, 46));
        btnAlertas.addActionListener(e -> verAlertas());

        JButton btnRegistrar = new JButton("REGISTRAR");
        btnRegistrar.setFont(new Font("Serif", Font.BOLD, 18));
        btnRegistrar.setBackground(new Color(220, 220, 220));
        btnRegistrar.setPreferredSize(new Dimension(240, 46));
        btnRegistrar.setToolTipText("Guarda todos los libros de la tabla en el inventario");
        btnRegistrar.addActionListener(e -> registrarEnInventario());

        JPanel centroSur = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centroSur.setOpaque(false);
        centroSur.add(btnAlertas);

        JPanel sur = new JPanel(new BorderLayout());
        sur.setOpaque(false);
        sur.add(btnSalir, BorderLayout.WEST);
        sur.add(centroSur, BorderLayout.CENTER);
        sur.add(btnRegistrar, BorderLayout.EAST);
        add(sur, BorderLayout.SOUTH);

        // Al mostrar el panel se actualiza el contador de alertas (la Venta pudo bajar el stock)
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) { actualizarAlertas(); }
        });

        refrescar();
        actualizarAlertas();
    }

    // ------------------------------------------------------------------
    // Construcción de la interfaz
    // ------------------------------------------------------------------

    private JPanel crearFormulario() {
        JPanel f = new JPanel(new GridBagLayout());
        f.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        soloDigitos(txtStock, 6);

        colocar(f, c, 0, 0, "NOMBRE DEL LIBRO", txtTitulo, 2.0);
        colocar(f, c, 0, 2, "STOCK", txtStock, 1.0);
        colocar(f, c, 1, 0, "GÉNERO", cmbGenero, 2.0);
        colocar(f, c, 1, 2, "PRECIO (S/)", txtPrecio, 1.0);
        colocar(f, c, 2, 0, "AUTOR", txtAutor, 2.0);
        colocar(f, c, 2, 2, "EDITORIAL", cmbEditorial, 1.0);
        colocar(f, c, 3, 0, "FECHA DE REGISTRO", spFecha, 2.0);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(boton("REGISTRAR EN TABLA", e -> registrarEnTabla()));
        botones.add(boton("ACTUALIZAR TABLA", e -> actualizarTabla()));
        botones.add(boton("QUITAR DE LA TABLA", e -> quitarDeTabla()));
        botones.add(boton("BORRAR DATOS", e -> borrarDatos()));
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 4;
        c.weightx = 1;
        f.add(botones, c);
        return f;
    }

    private void colocar(JPanel panel, GridBagConstraints c, int fila, int col,
                         String texto, Component campo, double peso) {
        JLabel l = new JLabel(texto);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Serif", Font.BOLD, 15));
        c.gridx = col;
        c.gridy = fila;
        c.gridwidth = 1;
        c.weightx = 0;
        panel.add(l, c);

        campo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        campo.setPreferredSize(new Dimension(200, 28));
        c.gridx = col + 1;
        c.weightx = peso;
        panel.add(campo, c);
    }

    private JButton boton(String texto, java.awt.event.ActionListener accion) {
        JButton b = new JButton(texto);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setBackground(new Color(220, 220, 220));
        b.setPreferredSize(new Dimension(170, 36));
        b.addActionListener(accion);
        return b;
    }

    private JPanel crearBuscador() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        p.setOpaque(false);

        JLabel l1 = new JLabel("BUSCADOR");
        l1.setForeground(Color.WHITE);
        l1.setFont(new Font("Serif", Font.BOLD, 15));
        txtBuscador.setToolTipText("Escribe parte del título del libro");

        JLabel l2 = new JLabel("FILTRAR POR");
        l2.setForeground(Color.WHITE);
        l2.setFont(new Font("Serif", Font.BOLD, 15));
        txtFiltro.setToolTipText("Escribe parte del género, autor o editorial elegido");

        p.add(l1);
        p.add(txtBuscador);
        p.add(l2);
        p.add(cmbCriterio);
        p.add(txtFiltro);

        // La tabla se filtra mientras se escribe
        DocumentListener alCambiar = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refrescar(); }
            @Override public void removeUpdate(DocumentEvent e) { refrescar(); }
            @Override public void changedUpdate(DocumentEvent e) { refrescar(); }
        };
        txtBuscador.getDocument().addDocumentListener(alCambiar);
        txtFiltro.getDocument().addDocumentListener(alCambiar);
        cmbCriterio.addActionListener(e -> refrescar());
        return p;
    }

    // ------------------------------------------------------------------
    // Acciones (delegan la lógica en el controlador)
    // ------------------------------------------------------------------

    /** REGISTRAR EN TABLA: pasa los datos del formulario a la tabla, en orden alfabético. */
    private void registrarEnTabla() {
        try {
            controlador.agregarPendiente(leerFormulario());
        } catch (ValidacionException ex) {
            avisar(ex);
            return;
        }
        limpiarFormulario();
        tabla.clearSelection();
        refrescar();
        txtTitulo.requestFocusInWindow();
    }

    /** ACTUALIZAR TABLA: aplica los cambios del formulario al libro seleccionado. */
    private void actualizarTabla() {
        RegistroLibro destino = seleccionado();
        if (destino == null) return;
        try {
            controlador.actualizarPendiente(destino, leerFormulario());
        } catch (ValidacionException ex) {
            avisar(ex);
            return;
        }
        limpiarFormulario();
        tabla.clearSelection();
        refrescar();
        JOptionPane.showMessageDialog(this, "Datos del libro actualizados.");
    }

    private void quitarDeTabla() {
        RegistroLibro r = seleccionado();
        if (r == null) return;
        controlador.quitarPendiente(r);
        limpiarFormulario();
        tabla.clearSelection();
        refrescar();
    }

    /** BORRAR DATOS: limpia el formulario antes de registrar en tabla. */
    private void borrarDatos() {
        limpiarFormulario();
        tabla.clearSelection();
        txtTitulo.requestFocusInWindow();
    }

    /** REGISTRAR: guarda los libros de la tabla en el inventario real. */
    private void registrarEnInventario() {
        if (controlador.cantidadPendientes() == 0) {
            aviso("No hay libros en la tabla para registrar.");
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
                "¿Registrar " + controlador.cantidadPendientes() + " libro(s) en el inventario?",
                "Confirmar registro", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;

        ResultadoRegistro resultado;
        try {
            resultado = controlador.registrarPendientes();
        } catch (ValidacionException ex) {
            avisar(ex);
            return;
        }

        limpiarFormulario();
        tabla.clearSelection();
        refrescar();
        actualizarAlertas();

        StringBuilder resumen = new StringBuilder();
        for (ResultadoRegistro.Item i : resultado.getItems()) {
            resumen.append(String.format("%-6s %s  (%s; stock%s: %d)\n", i.getCodigo(), i.getTitulo(),
                    i.isNuevo() ? "nuevo" : "ya existía", i.isNuevo() ? "" : " total", i.getStockFinal()));
        }
        JTextArea area = new JTextArea("Libros nuevos: " + resultado.cantidadNuevos()
                + "   |   Ya existentes (stock sumado): " + resultado.cantidadExistentes()
                + "\n\nCódigos para usar en el módulo de Venta:\n\n" + resumen);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(560, 220));
        JOptionPane.showMessageDialog(this, sp, "Inventario actualizado", JOptionPane.INFORMATION_MESSAGE);
    }

    private void salir() {
        if (controlador.cantidadPendientes() > 0) {
            int r = JOptionPane.showConfirmDialog(this,
                    "Hay " + controlador.cantidadPendientes() + " libro(s) en la tabla sin registrar. ¿Salir y descartarlos?",
                    "Registros pendientes", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) return;
        }
        for (RegistroLibro reg : controlador.pendientes()) controlador.quitarPendiente(reg);
        limpiarFormulario();
        txtBuscador.setText("");
        txtFiltro.setText("");
        refrescar();
        ventana.mostrar(VentanaPrincipal.MENU);
    }

    // ------------------------------------------------------------------
    // Alertas de stock
    // ------------------------------------------------------------------

    private void actualizarAlertas() {
        int n = controlador.alertasStockBajo().size();
        btnAlertas.setText("ALERTAS DE STOCK (" + n + ")");
        btnAlertas.setBackground(n > 0 ? new Color(255, 190, 190) : new Color(220, 220, 220));
    }

    private void verAlertas() {
        List<Libro> bajos = controlador.alertasStockBajo();
        if (bajos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay libros con stock bajo.",
                    "Alertas de stock", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Libro l : bajos) {
            String t = l.getTitulo().length() > 36 ? l.getTitulo().substring(0, 33) + "..." : l.getTitulo();
            sb.append(String.format("%-6s %-36s stock: %d\n", l.getCodigo(), t, l.getStock()));
        }
        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(520, 200));
        JOptionPane.showMessageDialog(this, sp,
                "Stock bajo (" + InventarioController.STOCK_MINIMO + " unidades o menos) - conviene reabastecer",
                JOptionPane.WARNING_MESSAGE);
    }

    // ------------------------------------------------------------------
    // Formulario y tabla
    // ------------------------------------------------------------------

    private RegistroLibro leerFormulario() throws ValidacionException {
        LocalDate fecha = ((Date) spFecha.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return controlador.crearRegistro(txtTitulo.getText(), txtAutor.getText(),
                String.valueOf(cmbGenero.getSelectedItem()), String.valueOf(cmbEditorial.getSelectedItem()),
                fecha, txtStock.getText(), txtPrecio.getText());
    }

    private void cargarFormulario(RegistroLibro r) {
        txtTitulo.setText(r.getTitulo());
        txtAutor.setText(r.getAutor());
        cmbGenero.setSelectedItem(r.getGenero());
        cmbEditorial.setSelectedItem(r.getEditorial());
        spFecha.setValue(Date.from(r.getFecha().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        txtStock.setText(String.valueOf(r.getStock()));
        txtPrecio.setText(Texto.monto(r.getPrecio()));
    }

    private void limpiarFormulario() {
        txtTitulo.setText("");
        txtAutor.setText("");
        txtStock.setText("");
        txtPrecio.setText("");
        cmbGenero.setSelectedItem("");
        cmbEditorial.setSelectedItem("");
        spFecha.setValue(new Date());
    }

    private RegistroLibro seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= vista.size()) {
            aviso("Selecciona en la tabla el libro que quieres actualizar o quitar.");
            return null;
        }
        return vista.get(fila);
    }

    /** Redibuja la tabla con el resultado del buscador (título) y el filtrador del controlador. */
    private void refrescar() {
        Criterio criterio = cmbCriterio.getSelectedIndex() == 0 ? Criterio.GENERO
                : cmbCriterio.getSelectedIndex() == 1 ? Criterio.AUTOR : Criterio.EDITORIAL;
        vista.clear();
        vista.addAll(controlador.buscar(txtBuscador.getText(), criterio, txtFiltro.getText()));
        modelo.setRowCount(0);
        int n = 1;
        for (RegistroLibro r : vista) {
            modelo.addRow(new Object[]{n++, r.getTitulo(), r.getAutor(), r.getGenero(), r.getEditorial(),
                r.getFecha().format(FMT), r.getStock(), Texto.monto(r.getPrecio())});
        }
        lblResumen.setText("  Libros por registrar (aún no guardados en el inventario): mostrando "
                + vista.size() + " de " + controlador.cantidadPendientes());
    }

    private void avisar(ValidacionException ex) {
        aviso(ex.getMessage());
        String campo = ex.getCampo();
        if ("titulo".equals(campo)) txtTitulo.requestFocusInWindow();
        else if ("autor".equals(campo)) txtAutor.requestFocusInWindow();
        else if ("stock".equals(campo)) txtStock.requestFocusInWindow();
        else if ("precio".equals(campo)) txtPrecio.requestFocusInWindow();
    }

    private void aviso(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    /** Limita un campo a solo dígitos y una longitud máxima. */
    private static void soloDigitos(JTextField campo, int max) {
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String texto, AttributeSet attr)
                    throws BadLocationException {
                replace(fb, offset, 0, texto, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int longitud, String texto, AttributeSet attr)
                    throws BadLocationException {
                if (texto == null) texto = "";
                String actual = fb.getDocument().getText(0, fb.getDocument().getLength());
                String nuevo = actual.substring(0, offset) + texto + actual.substring(offset + longitud);
                if (nuevo.matches("\\d{0," + max + "}")) {
                    super.replace(fb, offset, longitud, texto, attr);
                }
            }
        });
    }
}
