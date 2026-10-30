package libreriaarec.venta;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.print.PrinterException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import libreriaarec.Iconos;
import libreriaarec.PanelConFondo;
import libreriaarec.VentanaPrincipal;

/**
 * Módulo de Venta.
 * Flujo: DNI -> código (autocompleta título y precio) -> cantidad -> AGREGAR (tabla, hasta 100)
 *        -> icono de boleta (vista previa) -> GENERAR COMPROBANTE (emite y descuenta stock).
 */
public class PanelVenta extends PanelConFondo {

    private static final int MAX_LINEAS = 100;
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SEPARADOR = "------------------------------------";

    private final VentanaPrincipal ventana;
    private final List<Venta.Linea> lineas = new ArrayList<>();

    private final JTextField txtDni = new JTextField();
    private final JTextField txtCodigo = new JTextField();
    private final JTextField txtTitulo = new JTextField();
    private final JTextField txtPrecio = new JTextField();
    private final JTextField txtCantidad = new JTextField();

    private final JLabel lblEstado = new JLabel(" ");
    private final JLabel lblTotal = new JLabel();
    private final JTextArea txtBoleta = new JTextArea();

    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"NRO", "CÓDIGO", "PRODUCTO", "CANT", "PRECIO", "TOTAL"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tabla = new JTable(modelo);

    private boolean autoRelleno = false;    // título y precio vinieron del catálogo
    private boolean previsualizada = false; // la boleta mostrada corresponde a la tabla actual

    public PanelVenta(VentanaPrincipal ventana) {
        super("/libreriaarec/imagenes/fondo2.jpg");
        this.ventana = ventana;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // ----- Título -----
        JLabel titulo = new JLabel("MÓDULO DE VENTA", SwingConstants.CENTER);
        titulo.setOpaque(true);
        titulo.setBackground(new Color(220, 220, 220));
        titulo.setFont(new Font("Serif", Font.BOLD, 22));
        titulo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(titulo, BorderLayout.NORTH);

        // ----- Centro: formulario + tabla -----
        JPanel izquierda = new JPanel(new BorderLayout(10, 10));
        izquierda.setOpaque(false);
        izquierda.add(crearFormulario(), BorderLayout.NORTH);
        izquierda.add(crearTabla(), BorderLayout.CENTER);
        add(izquierda, BorderLayout.CENTER);

        // ----- Derecha: boleta -----
        add(crearBoleta(), BorderLayout.EAST);

        // ----- Botones inferiores -----
        JButton btnSalir = new JButton("SALIR");
        btnSalir.setFont(new Font("Serif", Font.BOLD, 18));
        btnSalir.setBackground(new Color(220, 220, 220));
        btnSalir.setPreferredSize(new Dimension(200, 50));
        btnSalir.addActionListener(e -> salir());

        JButton btnEmitir = new JButton("GENERAR COMPROBANTE");
        btnEmitir.setFont(new Font("Serif", Font.BOLD, 18));
        btnEmitir.setBackground(new Color(220, 220, 220));
        btnEmitir.setPreferredSize(new Dimension(280, 50));
        btnEmitir.addActionListener(e -> emitir());

        JPanel sur = new JPanel(new BorderLayout());
        sur.setOpaque(false);
        sur.add(btnSalir, BorderLayout.WEST);
        sur.add(btnEmitir, BorderLayout.EAST);
        add(sur, BorderLayout.SOUTH);

        refrescarTabla();
    }

    // ------------------------------------------------------------------
    // Construcción de la interfaz
    // ------------------------------------------------------------------

    private JPanel crearFormulario() {
        JPanel f = new JPanel(new GridBagLayout());
        f.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        soloDigitos(txtDni, 8);
        soloDigitos(txtCantidad, 4);

        agregarFila(f, c, 0, "DNI DEL CLIENTE", txtDni);
        agregarFila(f, c, 1, "CÓDIGO DEL LIBRO", txtCodigo);
        agregarFila(f, c, 2, "TÍTULO DEL LIBRO", txtTitulo);
        agregarFila(f, c, 3, "PRECIO (S/)", txtPrecio);
        agregarFila(f, c, 4, "CANTIDAD", txtCantidad);

        lblEstado.setForeground(new Color(255, 230, 120));
        lblEstado.setFont(new Font("Serif", Font.BOLD, 14));
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        f.add(lblEstado, c);

        JButton btnAgregar = new JButton("AGREGAR");
        btnAgregar.setPreferredSize(new Dimension(120, 56));
        btnAgregar.setFont(new Font("Serif", Font.BOLD, 15));
        btnAgregar.setBackground(new Color(220, 220, 220));
        btnAgregar.addActionListener(e -> agregar());

        JButton btnQuitar = botonIcono("basura.png", "QUITAR",
                "Quitar el producto seleccionado (o limpiar los campos si no hay selección)");
        btnQuitar.addActionListener(e -> quitar());

        JButton btnBoleta = botonIcono("boleta.png", "BOLETA",
                "Registrar y mostrar la boleta");
        btnBoleta.addActionListener(e -> verBoleta());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        botones.setOpaque(false);
        botones.add(btnAgregar);
        botones.add(btnQuitar);
        botones.add(btnBoleta);
        c.gridy = 6;
        f.add(botones, c);

        // Comportamiento de los campos
        txtCodigo.addActionListener(e -> {
            boolean encontrado = buscarLibro();
            (encontrado ? txtCantidad : txtTitulo).requestFocusInWindow();
        });
        txtCodigo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) { buscarLibro(); }
        });
        txtTitulo.addActionListener(e -> txtPrecio.requestFocusInWindow());
        txtPrecio.addActionListener(e -> txtCantidad.requestFocusInWindow());
        txtCantidad.addActionListener(e -> agregar());

        return f;
    }

    private void agregarFila(JPanel panel, GridBagConstraints c, int fila, String texto, JTextField campo) {
        JLabel l = new JLabel(texto);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Serif", Font.BOLD, 16));
        c.gridx = 0;
        c.gridy = fila;
        c.gridwidth = 1;
        c.weightx = 0;
        panel.add(l, c);

        campo.setFont(new Font("SansSerif", Font.PLAIN, 15));
        campo.setBackground(new Color(225, 225, 225));
        campo.setPreferredSize(new Dimension(260, 30));
        c.gridx = 1;
        c.weightx = 1;
        panel.add(campo, c);
    }

    private JButton botonIcono(String archivo, String textoRespaldo, String ayuda) {
        JButton b = new JButton();
        ImageIcon icono = Iconos.cargar(archivo, 40, 40);
        if (icono != null) {
            b.setIcon(icono);
        } else {
            b.setText(textoRespaldo); // si falta la imagen, el botón igual funciona
        }
        b.setToolTipText(ayuda);
        b.setBackground(new Color(240, 240, 240));
        b.setPreferredSize(new Dimension(100, 56));
        return b;
    }

    private JPanel crearTabla() {
        tabla.setRowHeight(26);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        int[] anchos = {45, 70, 300, 50, 80, 90};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        lblTotal.setForeground(Color.WHITE);
        lblTotal.setFont(new Font("Serif", Font.BOLD, 16));

        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setOpaque(false);
        p.add(new JScrollPane(tabla), BorderLayout.CENTER);
        p.add(lblTotal, BorderLayout.SOUTH);
        return p;
    }

    private JPanel crearBoleta() {
        JLabel titulo = new JLabel("BOLETA", SwingConstants.CENTER);
        titulo.setOpaque(true);
        titulo.setBackground(new Color(220, 220, 220));
        titulo.setFont(new Font("Serif", Font.BOLD, 18));
        titulo.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        txtBoleta.setEditable(false);
        txtBoleta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtBoleta.setMargin(new Insets(8, 8, 8, 8));

        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(340, 100));
        p.add(titulo, BorderLayout.NORTH);
        p.add(new JScrollPane(txtBoleta), BorderLayout.CENTER);
        return p;
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    /** Busca el código en el catálogo y rellena título y precio. Devuelve true si lo encontró. */
    private boolean buscarLibro() {
        String codigo = txtCodigo.getText().trim();
        if (codigo.isEmpty()) {
            lblEstado.setText(" ");
            return false;
        }
        Catalogo.Libro libro = Catalogo.buscar(codigo);
        if (libro != null) {
            txtTitulo.setText(libro.titulo);
            txtPrecio.setText(monto(libro.precio));
            autoRelleno = true;
            lblEstado.setText("Libro encontrado. Stock disponible: " + libro.stock);
            return true;
        }
        if (autoRelleno) { // quedaron datos de otro código: se limpian
            txtTitulo.setText("");
            txtPrecio.setText("");
            autoRelleno = false;
        }
        lblEstado.setText("Código no registrado: escribe el título y el precio manualmente.");
        return false;
    }

    private void agregar() {
        String codigo = txtCodigo.getText().trim().toUpperCase();
        String titulo = txtTitulo.getText().trim();

        if (codigo.isEmpty()) {
            aviso("Ingresa el código del libro.");
            txtCodigo.requestFocusInWindow();
            return;
        }
        if (titulo.isEmpty()) {
            aviso("Escribe el título del libro.");
            txtTitulo.requestFocusInWindow();
            return;
        }
        double precio;
        try {
            precio = leerMonto(txtPrecio.getText());
            if (precio <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            aviso("El precio no es válido.");
            txtPrecio.requestFocusInWindow();
            return;
        }
        int cantidad;
        try {
            cantidad = Integer.parseInt(txtCantidad.getText().trim());
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            aviso("Ingresa una cantidad válida (mayor que 0).");
            txtCantidad.requestFocusInWindow();
            return;
        }

        // Control de stock (solo para libros del catálogo)
        Catalogo.Libro libro = Catalogo.buscar(codigo);
        if (libro != null && cantidadEnTabla(codigo) + cantidad > libro.stock) {
            aviso("Stock insuficiente. Quedan " + libro.stock + " unidad(es) de \"" + libro.titulo + "\".");
            return;
        }

        // Si ya está el mismo libro con el mismo precio, se suma la cantidad
        Venta.Linea existente = null;
        for (Venta.Linea l : lineas) {
            if (l.codigo.equals(codigo) && l.precio == precio) {
                existente = l;
                break;
            }
        }
        if (existente != null) {
            existente.cantidad += cantidad;
        } else {
            if (lineas.size() >= MAX_LINEAS) {
                aviso("Se alcanzó el máximo de " + MAX_LINEAS + " productos por boleta.");
                return;
            }
            lineas.add(new Venta.Linea(codigo, titulo, cantidad, precio));
        }

        refrescarTabla();
        invalidarBoleta();
        limpiarCamposProducto();
        txtCodigo.requestFocusInWindow();
    }

    /** Icono de basura: quita la fila seleccionada; sin selección, limpia los campos. */
    private void quitar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            limpiarCamposProducto();
            return;
        }
        lineas.remove(fila);
        refrescarTabla();
        invalidarBoleta();
    }

    /** Icono de la hoja roja: muestra en el cuadro BOLETA los datos registrados. */
    private boolean verBoleta() {
        if (!validarVenta()) return false;
        txtBoleta.setText(construirBoleta("(se asigna al emitir)", LocalDateTime.now(),
                txtDni.getText().trim(), lineas, true));
        txtBoleta.setCaretPosition(0);
        previsualizada = true;
        return true;
    }

    /** Botón GENERAR COMPROBANTE: emite la boleta, descuenta stock y deja todo listo para otra venta. */
    private void emitir() {
        if (!previsualizada) {
            if (verBoleta()) {
                JOptionPane.showMessageDialog(this,
                        "Revisa los datos en el cuadro BOLETA y vuelve a pulsar GENERAR COMPROBANTE para emitirla.");
            }
            return;
        }
        if (!validarVenta()) return;

        int r = JOptionPane.showConfirmDialog(this,
                "¿Emitir la boleta por S/ " + monto(totalActual()) + "?",
                "Confirmar venta", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;

        Venta venta = new Venta(txtDni.getText().trim(), lineas);
        for (Venta.Linea l : venta.lineas) {
            Catalogo.Libro libro = Catalogo.buscar(l.codigo);
            if (libro != null) libro.stock -= l.cantidad;
        }
        Venta.HISTORIAL.add(venta);

        String texto = construirBoleta(venta.numero, venta.fecha, venta.dni, venta.lineas, false);

        // Deja el formulario limpio para la siguiente venta; la boleta emitida queda a la vista
        lineas.clear();
        refrescarTabla();
        limpiarCamposProducto();
        txtDni.setText("");
        txtBoleta.setText(texto);
        txtBoleta.setCaretPosition(0);
        previsualizada = false;

        int imprimir = JOptionPane.showConfirmDialog(this,
                "Boleta " + venta.numero + " emitida correctamente.\n¿Deseas imprimirla?",
                "Venta registrada", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
        if (imprimir == JOptionPane.YES_OPTION) {
            try {
                txtBoleta.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "No se pudo imprimir: " + ex.getMessage(),
                        "Error de impresión", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void salir() {
        if (!lineas.isEmpty()) {
            int r = JOptionPane.showConfirmDialog(this,
                    "Hay una venta sin emitir. ¿Salir y descartarla?",
                    "Venta pendiente", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) return;
        }
        lineas.clear();
        refrescarTabla();
        limpiarCamposProducto();
        txtDni.setText("");
        invalidarBoleta();
        ventana.mostrar(VentanaPrincipal.MENU);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private boolean validarVenta() {
        if (!txtDni.getText().trim().matches("\\d{8}")) {
            aviso("Ingresa el DNI del cliente (8 dígitos).");
            txtDni.requestFocusInWindow();
            return false;
        }
        if (lineas.isEmpty()) {
            aviso("Agrega al menos un producto a la tabla.");
            return false;
        }
        return true;
    }

    private int cantidadEnTabla(String codigo) {
        int t = 0;
        for (Venta.Linea l : lineas) {
            if (l.codigo.equals(codigo)) t += l.cantidad;
        }
        return t;
    }

    private double totalActual() {
        double t = 0;
        for (Venta.Linea l : lineas) t += l.total();
        return t;
    }

    private void refrescarTabla() {
        modelo.setRowCount(0);
        int nro = 1;
        for (Venta.Linea l : lineas) {
            modelo.addRow(new Object[]{nro++, l.codigo, l.titulo, l.cantidad, monto(l.precio), monto(l.total())});
        }
        lblTotal.setText("  TOTAL: S/ " + monto(totalActual()) + "     (" + lineas.size() + " / " + MAX_LINEAS + " productos)");
    }

    private void limpiarCamposProducto() {
        txtCodigo.setText("");
        txtTitulo.setText("");
        txtPrecio.setText("");
        txtCantidad.setText("");
        lblEstado.setText(" ");
        autoRelleno = false;
    }

    /** Si la tabla cambia, la boleta mostrada ya no es válida. */
    private void invalidarBoleta() {
        previsualizada = false;
        txtBoleta.setText("");
    }

    private String construirBoleta(String numero, LocalDateTime fecha, String dni,
                                   List<Venta.Linea> lista, boolean preliminar) {
        StringBuilder sb = new StringBuilder();
        sb.append("        LIBRERÍAS AREC\n");
        sb.append("        BOLETA DE VENTA\n");
        if (preliminar) sb.append("      *** VISTA PREVIA ***\n");
        sb.append(SEPARADOR).append("\n");
        sb.append("N°:    ").append(numero).append("\n");
        sb.append("Fecha: ").append(fecha.format(FECHA_HORA)).append("\n");
        sb.append("DNI:   ").append(dni).append("\n");
        sb.append(SEPARADOR).append("\n");

        double total = 0;
        int n = 1;
        for (Venta.Linea l : lista) {
            sb.append(n++).append(". ").append(recortar(l.titulo, 30)).append("\n");
            sb.append("   [").append(l.codigo).append("] ")
              .append(l.cantidad).append(" x ").append(monto(l.precio))
              .append(" = ").append(monto(l.total())).append("\n");
            total += l.total();
        }
        sb.append(SEPARADOR).append("\n");
        sb.append("TOTAL A PAGAR: S/ ").append(monto(total)).append("\n\n");
        sb.append("     ¡Gracias por su compra!\n");
        return sb.toString();
    }

    private static String recortar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max - 3) + "...";
    }

    private static String monto(double valor) {
        return String.format(Locale.US, "%.2f", valor);
    }

    private static double leerMonto(String texto) {
        return Double.parseDouble(texto.trim().replace(',', '.'));
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
