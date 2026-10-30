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
import java.text.Collator;
import java.text.Normalizer;
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
import libreriaarec.venta.Catalogo;

/**
 * Módulo de Inventario y Alertas.
 * 1) Se llenan los datos del libro y se pasan a la tabla (queda en orden alfabético).
 * 2) En la tabla se puede buscar, filtrar, corregir (actualizar) o quitar libros.
 * 3) El botón REGISTRAR guarda todo en el inventario (Catalogo) y asigna los códigos.
 */
public class PanelInventario extends PanelConFondo {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Collator COLLATOR = crearCollator();

    /** Libro que aún no se ha guardado en el inventario. */
    private static class Registro {
        String titulo, autor, genero, editorial;
        LocalDate fecha;
        int stock;
        double precio;

        Registro(String titulo, String autor, String genero, String editorial,
                 LocalDate fecha, int stock, double precio) {
            this.titulo = titulo;
            this.autor = autor;
            this.genero = genero;
            this.editorial = editorial;
            this.fecha = fecha;
            this.stock = stock;
            this.precio = precio;
        }

        void copiarDe(Registro o) {
            titulo = o.titulo;
            autor = o.autor;
            genero = o.genero;
            editorial = o.editorial;
            fecha = o.fecha;
            stock = o.stock;
            precio = o.precio;
        }
    }

    private final VentanaPrincipal ventana;
    private final List<Registro> registros = new ArrayList<>(); // todos los pendientes
    private final List<Registro> vista = new ArrayList<>();     // los que se ven (según buscador/filtro)

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

    public PanelInventario(VentanaPrincipal ventana) {
        super("/libreriaarec/imagenes/fondo2.jpg");
        this.ventana = ventana;
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
    // Acciones sobre la tabla (libros pendientes de registrar)
    // ------------------------------------------------------------------

    /** REGISTRAR EN TABLA: pasa los datos del formulario a la tabla, en orden alfabético. */
    private void registrarEnTabla() {
        Registro nuevo = leerFormulario();
        if (nuevo == null) return;
        if (existeDuplicado(nuevo, null)) {
            aviso("Ese libro (mismo título y autor) ya está en la tabla.\n"
                    + "Selecciónalo en la tabla y usa ACTUALIZAR TABLA para cambiar su stock o precio.");
            return;
        }
        registros.add(nuevo);
        ordenar();
        limpiarFormulario();
        tabla.clearSelection();
        refrescar();
        txtTitulo.requestFocusInWindow();
    }

    /** ACTUALIZAR TABLA: aplica los cambios del formulario al libro seleccionado (stock, precio, nombre...). */
    private void actualizarTabla() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= vista.size()) {
            aviso("Selecciona en la tabla el libro que quieres actualizar.");
            return;
        }
        Registro editado = leerFormulario();
        if (editado == null) return;
        Registro destino = vista.get(fila);
        if (existeDuplicado(editado, destino)) {
            aviso("Ya existe otro libro con ese título y autor en la tabla.");
            return;
        }
        destino.copiarDe(editado);
        ordenar();
        limpiarFormulario();
        tabla.clearSelection();
        refrescar();
        JOptionPane.showMessageDialog(this, "Datos del libro actualizados.");
    }

    private void quitarDeTabla() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= vista.size()) {
            aviso("Selecciona en la tabla el libro que quieres quitar.");
            return;
        }
        registros.remove(vista.get(fila));
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
        if (registros.isEmpty()) {
            aviso("No hay libros en la tabla para registrar.");
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
                "¿Registrar " + registros.size() + " libro(s) en el inventario?",
                "Confirmar registro", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;

        StringBuilder resumen = new StringBuilder();
        int nuevos = 0;
        int actualizados = 0;
        for (Registro reg : registros) {
            Catalogo.Libro existente = Catalogo.buscarPorTitulo(reg.titulo, reg.autor);
            if (existente != null) {
                // El libro ya estaba en el inventario: se suma el stock y se actualiza el precio
                existente.stock += reg.stock;
                existente.precio = reg.precio;
                actualizados++;
                resumen.append(String.format("%-6s %s  (ya existía; stock total: %d)\n",
                        existente.codigo, reg.titulo, existente.stock));
            } else {
                Catalogo.Libro nuevo = Catalogo.registrar(reg.titulo, reg.autor, reg.genero,
                        reg.editorial, reg.fecha, reg.precio, reg.stock);
                nuevos++;
                resumen.append(String.format("%-6s %s  (nuevo; stock: %d)\n",
                        nuevo.codigo, reg.titulo, nuevo.stock));
            }
        }

        registros.clear();
        limpiarFormulario();
        tabla.clearSelection();
        refrescar();
        actualizarAlertas();

        JTextArea area = new JTextArea("Libros nuevos: " + nuevos + "   |   Ya existentes (stock sumado): "
                + actualizados + "\n\nCódigos para usar en el módulo de Venta:\n\n" + resumen);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(560, 220));
        JOptionPane.showMessageDialog(this, sp, "Inventario actualizado", JOptionPane.INFORMATION_MESSAGE);
    }

    private void salir() {
        if (!registros.isEmpty()) {
            int r = JOptionPane.showConfirmDialog(this,
                    "Hay " + registros.size() + " libro(s) en la tabla sin registrar. ¿Salir y descartarlos?",
                    "Registros pendientes", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) return;
        }
        registros.clear();
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
        int n = Catalogo.stockBajo(Catalogo.STOCK_MINIMO).size();
        btnAlertas.setText("ALERTAS DE STOCK (" + n + ")");
        btnAlertas.setBackground(n > 0 ? new Color(255, 190, 190) : new Color(220, 220, 220));
    }

    private void verAlertas() {
        List<Catalogo.Libro> bajos = Catalogo.stockBajo(Catalogo.STOCK_MINIMO);
        if (bajos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay libros con stock bajo.",
                    "Alertas de stock", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Catalogo.Libro l : bajos) {
            String t = l.titulo.length() > 36 ? l.titulo.substring(0, 33) + "..." : l.titulo;
            sb.append(String.format("%-6s %-36s stock: %d\n", l.codigo, t, l.stock));
        }
        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(520, 200));
        JOptionPane.showMessageDialog(this, sp,
                "Stock bajo (" + Catalogo.STOCK_MINIMO + " unidades o menos) - conviene reabastecer",
                JOptionPane.WARNING_MESSAGE);
    }

    // ------------------------------------------------------------------
    // Formulario
    // ------------------------------------------------------------------

    /** Lee y valida el formulario. Devuelve null (y avisa) si falta algo. */
    private Registro leerFormulario() {
        String titulo = txtTitulo.getText().trim();
        String autor = txtAutor.getText().trim();
        String genero = String.valueOf(cmbGenero.getSelectedItem()).trim();
        String editorial = String.valueOf(cmbEditorial.getSelectedItem()).trim();

        if (titulo.isEmpty()) { aviso("Escribe el nombre del libro."); txtTitulo.requestFocusInWindow(); return null; }
        if (autor.isEmpty()) { aviso("Escribe el nombre del autor."); txtAutor.requestFocusInWindow(); return null; }
        if (genero.isEmpty() || genero.equals("null")) { aviso("Indica el género del libro."); return null; }
        if (editorial.isEmpty() || editorial.equals("null")) { aviso("Indica la editorial que provee el libro."); return null; }

        int stock;
        try {
            stock = Integer.parseInt(txtStock.getText().trim());
            if (stock <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            aviso("Ingresa un stock válido (mayor que 0).");
            txtStock.requestFocusInWindow();
            return null;
        }
        double precio;
        try {
            precio = Double.parseDouble(txtPrecio.getText().trim().replace(',', '.'));
            if (precio <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            aviso("Ingresa un precio válido (mayor que 0).");
            txtPrecio.requestFocusInWindow();
            return null;
        }
        LocalDate fecha = ((Date) spFecha.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return new Registro(titulo, autor, genero, editorial, fecha, stock, precio);
    }

    private void cargarFormulario(Registro r) {
        txtTitulo.setText(r.titulo);
        txtAutor.setText(r.autor);
        cmbGenero.setSelectedItem(r.genero);
        cmbEditorial.setSelectedItem(r.editorial);
        spFecha.setValue(Date.from(r.fecha.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        txtStock.setText(String.valueOf(r.stock));
        txtPrecio.setText(String.format(Locale.US, "%.2f", r.precio));
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

    // ------------------------------------------------------------------
    // Tabla: orden, filtros y utilidades
    // ------------------------------------------------------------------

    private void ordenar() {
        registros.sort((a, b) -> COLLATOR.compare(a.titulo, b.titulo));
    }

    private boolean existeDuplicado(Registro candidato, Registro ignorar) {
        for (Registro r : registros) {
            if (r == ignorar) continue;
            if (norm(r.titulo).equals(norm(candidato.titulo)) && norm(r.autor).equals(norm(candidato.autor))) {
                return true;
            }
        }
        return false;
    }

    /** Redibuja la tabla aplicando el buscador (título) y el filtrador (género/autor/editorial). */
    private void refrescar() {
        String q = norm(txtBuscador.getText());
        String f = norm(txtFiltro.getText());
        int criterio = cmbCriterio.getSelectedIndex();

        vista.clear();
        modelo.setRowCount(0);
        int n = 1;
        for (Registro r : registros) {
            if (!q.isEmpty() && !norm(r.titulo).contains(q)) continue;
            if (!f.isEmpty()) {
                String campo = criterio == 0 ? r.genero : (criterio == 1 ? r.autor : r.editorial);
                if (!norm(campo).contains(f)) continue;
            }
            vista.add(r);
            modelo.addRow(new Object[]{n++, r.titulo, r.autor, r.genero, r.editorial,
                r.fecha.format(FMT), r.stock, String.format(Locale.US, "%.2f", r.precio)});
        }
        lblResumen.setText("  Libros por registrar (aún no guardados en el inventario): mostrando "
                + vista.size() + " de " + registros.size());
    }

    /** Minúsculas y sin tildes, para que "habitacion" encuentre "Habitación". */
    private static String norm(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    private static Collator crearCollator() {
        Collator c = Collator.getInstance(Locale.forLanguageTag("es-PE"));
        c.setStrength(Collator.PRIMARY); // ignora mayúsculas y tildes al ordenar
        return c;
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
