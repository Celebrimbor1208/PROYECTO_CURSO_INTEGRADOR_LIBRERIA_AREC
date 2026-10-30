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
import java.util.Optional;
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
import libreriaarec.controlador.ValidacionException;
import libreriaarec.controlador.VentaController;
import libreriaarec.modelo.Libro;
import libreriaarec.modelo.Venta;
import libreriaarec.util.Texto;

/**
 * Vista del módulo de Venta.
 * Solo muestra la pantalla y pasa las acciones al {@link VentaController}, que contiene la lógica.
 * Flujo: DNI -> código (autocompleta título y precio) -> cantidad -> AGREGAR (tabla, hasta 100)
 *        -> icono de boleta (vista previa) -> GENERAR COMPROBANTE (emite y descuenta stock).
 */
public class PanelVenta extends PanelConFondo {

    private final VentanaPrincipal ventana;
    private final VentaController controlador;

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

    private boolean autoRelleno = false;    // título y precio vinieron del inventario
    private boolean previsualizada = false; // la boleta mostrada corresponde a la tabla actual

    public PanelVenta(VentanaPrincipal ventana, VentaController controlador) {
        super("/libreriaarec/imagenes/fondo2.jpg");
        this.ventana = ventana;
        this.controlador = controlador;
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
    // Acciones (cada una delega la lógica en el controlador)
    // ------------------------------------------------------------------

    /** Busca el código en el inventario y rellena título y precio. Devuelve true si lo encontró. */
    private boolean buscarLibro() {
        String codigo = txtCodigo.getText().trim();
        if (codigo.isEmpty()) {
            lblEstado.setText(" ");
            return false;
        }
        Optional<Libro> libro = controlador.consultarLibro(codigo);
        if (libro.isPresent()) {
            txtTitulo.setText(libro.get().getTitulo());
            txtPrecio.setText(Texto.monto(libro.get().getPrecio()));
            autoRelleno = true;
            lblEstado.setText("Libro encontrado. Stock disponible: " + libro.get().getStock());
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
        try {
            controlador.agregarLinea(txtCodigo.getText(), txtTitulo.getText(),
                    txtPrecio.getText(), txtCantidad.getText());
        } catch (ValidacionException ex) {
            avisar(ex);
            return;
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
        controlador.quitarLinea(fila);
        refrescarTabla();
        invalidarBoleta();
    }

    /** Icono de la hoja roja: muestra en el cuadro BOLETA los datos registrados. */
    private boolean verBoleta() {
        try {
            controlador.validarVenta(txtDni.getText());
        } catch (ValidacionException ex) {
            avisar(ex);
            return false;
        }
        txtBoleta.setText(BoletaTexto.construir("(se asigna al emitir)", LocalDateTime.now(),
                txtDni.getText().trim(), controlador.lineas(), true));
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
        int r = JOptionPane.showConfirmDialog(this,
                "¿Emitir la boleta por S/ " + Texto.monto(controlador.total()) + "?",
                "Confirmar venta", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;

        Venta venta;
        try {
            venta = controlador.emitir(txtDni.getText());
        } catch (ValidacionException ex) {
            avisar(ex);
            return;
        }
        String texto = BoletaTexto.construir(venta.getNumero(), venta.getFecha(), venta.getDni(),
                venta.getLineas(), false);

        // Deja el formulario limpio para la siguiente venta; la boleta emitida queda a la vista
        refrescarTabla();
        limpiarCamposProducto();
        txtDni.setText("");
        txtBoleta.setText(texto);
        txtBoleta.setCaretPosition(0);
        previsualizada = false;

        int imprimir = JOptionPane.showConfirmDialog(this,
                "Boleta " + venta.getNumero() + " emitida correctamente.\n¿Deseas imprimirla?",
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
        if (controlador.hayVentaPendiente()) {
            int r = JOptionPane.showConfirmDialog(this,
                    "Hay una venta sin emitir. ¿Salir y descartarla?",
                    "Venta pendiente", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) return;
        }
        controlador.cancelar();
        refrescarTabla();
        limpiarCamposProducto();
        txtDni.setText("");
        invalidarBoleta();
        ventana.mostrar(VentanaPrincipal.MENU);
    }

    // ------------------------------------------------------------------
    // Utilidades de la vista
    // ------------------------------------------------------------------

    private void refrescarTabla() {
        modelo.setRowCount(0);
        int nro = 1;
        for (Venta.Linea l : controlador.lineas()) {
            modelo.addRow(new Object[]{nro++, l.getCodigo(), l.getTitulo(), l.getCantidad(),
                Texto.monto(l.getPrecio()), Texto.monto(l.total())});
        }
        lblTotal.setText("  TOTAL: S/ " + Texto.monto(controlador.total()) + "     ("
                + controlador.lineas().size() + " / " + VentaController.MAX_LINEAS + " productos)");
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

    /** Muestra el mensaje de validación y lleva el cursor a la casilla con el error. */
    private void avisar(ValidacionException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
        String campo = ex.getCampo();
        if ("dni".equals(campo)) txtDni.requestFocusInWindow();
        else if ("codigo".equals(campo)) txtCodigo.requestFocusInWindow();
        else if ("titulo".equals(campo)) txtTitulo.requestFocusInWindow();
        else if ("precio".equals(campo)) txtPrecio.requestFocusInWindow();
        else if ("cantidad".equals(campo)) txtCantidad.requestFocusInWindow();
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
