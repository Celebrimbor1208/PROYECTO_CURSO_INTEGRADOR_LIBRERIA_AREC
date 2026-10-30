package libreriaarec.compra;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import libreriaarec.PanelConFondo;
import libreriaarec.VentanaPrincipal;

/**
 * Módulo de Orden de Compra: un buscador de órdenes.
 * Se escribe UNO o más datos (editorial, N° de orden, cantidad, proveedor) y BUSCAR muestra
 * las órdenes que coinciden. Al elegir una, se ven sus libros y se puede DESCARGAR EL PDF.
 * Las órdenes salen de las solicitudes que ya cerraron acuerdo en Reabastecimiento.
 */
public class PanelOrdenCompra extends PanelConFondo {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VentanaPrincipal ventana;
    private final List<OrdenCompra> resultados = new ArrayList<>();
    private boolean busquedaHecha = false;

    private final JTextField txtEditorial = new JTextField();
    private final JTextField txtProveedor = new JTextField();
    private final JTextField txtNumero = new JTextField();
    private final JTextField txtCantidad = new JTextField();

    private final DefaultTableModel modeloOrdenes = new DefaultTableModel(
            new Object[]{"N° ORDEN", "FECHA", "PROVEEDOR", "EDITORIAL(ES)", "LIBROS", "TOTAL ACORDADO", "ENTREGA"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tablaOrdenes = new JTable(modeloOrdenes);

    private final DefaultTableModel modeloDetalle = new DefaultTableModel(
            new Object[]{"ITEM", "TÍTULO", "AUTOR", "EDITORIAL", "CANT.", "PRECIO REF.", "VALOR REF."}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tablaDetalle = new JTable(modeloDetalle);

    private final JLabel lblResumen = new JLabel(" ");
    private final JLabel lblDetalle = new JLabel(" ");

    public PanelOrdenCompra(VentanaPrincipal ventana) {
        super("/libreriaarec/imagenes/fondo2.jpg");
        this.ventana = ventana;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        // ----- Título -----
        JLabel titulo = new JLabel("MÓDULO DE ORDEN DE COMPRA", SwingConstants.CENTER);
        titulo.setOpaque(true);
        titulo.setBackground(new Color(220, 220, 220));
        titulo.setFont(new Font("Serif", Font.BOLD, 22));
        titulo.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        add(titulo, BorderLayout.NORTH);

        // ----- Buscador + resultados -----
        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setOpaque(false);
        centro.add(crearBuscador(), BorderLayout.NORTH);
        centro.add(crearResultados(), BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        // ----- Botones inferiores -----
        JButton btnSalir = new JButton("SALIR");
        btnSalir.setFont(new Font("Serif", Font.BOLD, 18));
        btnSalir.setBackground(new Color(220, 220, 220));
        btnSalir.setPreferredSize(new Dimension(200, 46));
        btnSalir.addActionListener(e -> salir());

        JButton btnPdf = new JButton("DESCARGAR PDF");
        btnPdf.setFont(new Font("Serif", Font.BOLD, 18));
        btnPdf.setBackground(new Color(220, 220, 220));
        btnPdf.setPreferredSize(new Dimension(260, 46));
        btnPdf.setToolTipText("Guarda la orden seleccionada como archivo PDF");
        btnPdf.addActionListener(e -> descargarPdf());

        JPanel sur = new JPanel(new BorderLayout());
        sur.setOpaque(false);
        sur.add(btnSalir, BorderLayout.WEST);
        sur.add(btnPdf, BorderLayout.EAST);
        add(sur, BorderLayout.SOUTH);

        // Si se vuelve a esta pantalla, la búsqueda se actualiza (pudo cerrarse un acuerdo nuevo)
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (busquedaHecha) buscar(false);
            }
        });

        mostrarResultados();
    }

    // ------------------------------------------------------------------
    // Construcción de la interfaz
    // ------------------------------------------------------------------

    private JPanel crearBuscador() {
        JPanel f = new JPanel(new GridBagLayout());
        f.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        soloDigitos(txtCantidad, 6);

        colocar(f, c, 0, 0, "EDITORIAL", txtEditorial);
        colocar(f, c, 0, 2, "PROVEEDOR / DISTRIBUIDOR", txtProveedor);
        colocar(f, c, 1, 0, "N° ORDEN DE COMPRA", txtNumero);
        colocar(f, c, 1, 2, "CANTIDAD", txtCantidad);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        botones.setOpaque(false);
        JButton btnBuscar = new JButton("BUSCAR");
        btnBuscar.setFont(new Font("Serif", Font.BOLD, 16));
        btnBuscar.setBackground(new Color(220, 220, 220));
        btnBuscar.setPreferredSize(new Dimension(200, 38));
        btnBuscar.addActionListener(e -> buscar(true));
        JButton btnLimpiar = new JButton("LIMPIAR");
        btnLimpiar.setPreferredSize(new Dimension(120, 38));
        btnLimpiar.addActionListener(e -> limpiar());
        botones.add(btnBuscar);
        botones.add(btnLimpiar);

        JLabel ayuda = new JLabel("Basta con llenar UN dato. Si dejas todo vacío, se muestran todas las órdenes.");
        ayuda.setForeground(new Color(230, 230, 230));
        ayuda.setFont(new Font("SansSerif", Font.ITALIC, 12));
        botones.add(ayuda);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 4;
        c.weightx = 1;
        f.add(botones, c);

        // Enter en cualquier casilla también busca
        for (JTextField t : new JTextField[]{txtEditorial, txtProveedor, txtNumero, txtCantidad}) {
            t.addActionListener(e -> buscar(true));
        }
        return f;
    }

    private void colocar(JPanel panel, GridBagConstraints c, int fila, int col, String texto, JTextField campo) {
        JLabel l = new JLabel(texto);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Serif", Font.BOLD, 15));
        c.gridx = col;
        c.gridy = fila;
        c.gridwidth = 1;
        c.weightx = 0;
        panel.add(l, c);

        campo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        campo.setPreferredSize(new Dimension(220, 28));
        c.gridx = col + 1;
        c.weightx = 1;
        panel.add(campo, c);
    }

    private JPanel crearResultados() {
        lblResumen.setForeground(Color.WHITE);
        lblResumen.setFont(new Font("Serif", Font.BOLD, 14));
        lblDetalle.setForeground(Color.WHITE);
        lblDetalle.setFont(new Font("Serif", Font.BOLD, 14));

        configurarTabla(tablaOrdenes, new int[]{150, 80, 150, 180, 55, 110, 85});
        configurarTabla(tablaDetalle, new int[]{40, 250, 160, 130, 50, 85, 85});
        tablaOrdenes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) mostrarDetalle();
        });

        JPanel arriba = new JPanel(new BorderLayout(0, 3));
        arriba.setOpaque(false);
        arriba.add(lblResumen, BorderLayout.NORTH);
        arriba.add(new JScrollPane(tablaOrdenes), BorderLayout.CENTER);

        JPanel abajo = new JPanel(new BorderLayout(0, 3));
        abajo.setOpaque(false);
        abajo.add(lblDetalle, BorderLayout.NORTH);
        abajo.add(new JScrollPane(tablaDetalle), BorderLayout.CENTER);

        JPanel p = new JPanel(new GridLayout(2, 1, 0, 8));
        p.setOpaque(false);
        p.add(arriba);
        p.add(abajo);
        return p;
    }

    private void configurarTabla(JTable t, int[] anchos) {
        t.setRowHeight(24);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setFillsViewportHeight(true);
        t.getTableHeader().setReorderingAllowed(false);
        for (int i = 0; i < anchos.length; i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
    }

    // ------------------------------------------------------------------
    // Búsqueda
    // ------------------------------------------------------------------

    /** BUSCAR: filtra las órdenes con los datos escritos (los que estén llenos se combinan). */
    private void buscar(boolean avisar) {
        String editorial = norm(txtEditorial.getText());
        String proveedor = norm(txtProveedor.getText());
        String numero = norm(txtNumero.getText());
        String cantTexto = txtCantidad.getText().trim();
        Integer cantidad = cantTexto.isEmpty() ? null : Integer.valueOf(cantTexto);

        resultados.clear();
        for (OrdenCompra o : OrdenCompra.desde(ventana.getSolicitudes())) {
            if (!editorial.isEmpty() && !norm(o.editoriales()).contains(editorial)) continue;
            if (!proveedor.isEmpty() && !norm(o.proveedor).contains(proveedor)) continue;
            if (!numero.isEmpty() && !norm(o.numero).contains(numero)) continue;
            if (cantidad != null && !coincideCantidad(o, cantidad)) continue;
            resultados.add(o);
        }
        busquedaHecha = true;
        mostrarResultados();
        if (!resultados.isEmpty()) tablaOrdenes.setRowSelectionInterval(0, 0);

        if (resultados.isEmpty() && avisar) avisarSinResultados();
    }

    /** La cantidad coincide con el total de libros de la orden o con la de alguno de sus libros. */
    private boolean coincideCantidad(OrdenCompra o, int cantidad) {
        if (o.totalLibros() == cantidad) return true;
        for (OrdenCompra.Linea l : o.lineas) {
            if (l.cantidad == cantidad) return true;
        }
        return false;
    }

    private void avisarSinResultados() {
        int sinAcuerdo = 0;
        for (libreriaarec.reabastecimiento.Solicitud s : ventana.getSolicitudes()) {
            if (s.estado != libreriaarec.reabastecimiento.Solicitud.Estado.ACORDADA) sinAcuerdo++;
        }
        String msg = "No se encontró ninguna orden de compra con esos datos.\n"
                + "Revisa lo escrito o confirma que la orden se haya registrado.";
        if (sinAcuerdo > 0) {
            msg += "\n\nHay " + sinAcuerdo + " solicitud(es) en seguimiento que todavía no cerraron acuerdo "
                    + "con un proveedor; la orden de compra se genera al cerrarlo.";
        }
        Object[] opciones = {"Ir al seguimiento de solicitudes", "Cerrar"};
        int r = JOptionPane.showOptionDialog(this, msg, "Sin resultados", JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, opciones, opciones[1]);
        if (r == 0) ventana.mostrar(VentanaPrincipal.SEGUIMIENTO);
    }

    private void limpiar() {
        txtEditorial.setText("");
        txtProveedor.setText("");
        txtNumero.setText("");
        txtCantidad.setText("");
        resultados.clear();
        busquedaHecha = false;
        mostrarResultados();
        txtEditorial.requestFocusInWindow();
    }

    // ------------------------------------------------------------------
    // Resultados
    // ------------------------------------------------------------------

    private void mostrarResultados() {
        modeloOrdenes.setRowCount(0);
        for (OrdenCompra o : resultados) {
            modeloOrdenes.addRow(new Object[]{o.numero, o.fecha.format(FMT), o.proveedor, o.editoriales(),
                String.valueOf(o.totalLibros()), "S/ " + monto(o.totalAcordado), o.fechaEntrega.format(FMT)});
        }
        if (!busquedaHecha) {
            lblResumen.setText("  Escribe uno o más datos y presiona BUSCAR.");
        } else {
            lblResumen.setText("  " + resultados.size() + " orden(es) de compra encontrada(s)");
        }
        mostrarDetalle();
    }

    private void mostrarDetalle() {
        modeloDetalle.setRowCount(0);
        OrdenCompra o = seleccionada();
        if (o == null) {
            lblDetalle.setText("  Selecciona una orden para ver sus libros.");
            return;
        }
        int n = 1;
        for (OrdenCompra.Linea l : o.lineas) {
            modeloDetalle.addRow(new Object[]{String.valueOf(n++), l.titulo, l.autor, l.editorial,
                String.valueOf(l.cantidad), l.precioRef > 0 ? monto(l.precioRef) : "Por cotizar",
                l.precioRef > 0 ? monto(l.valorRef()) : "-"});
        }
        lblDetalle.setText("  Orden " + o.numero + "  |  " + o.proveedor
                + "  |  Total: S/ " + monto(o.totalAcordado) + "  |  Entrega: " + o.fechaEntrega.format(FMT));
    }

    private OrdenCompra seleccionada() {
        int fila = tablaOrdenes.getSelectedRow();
        return (fila >= 0 && fila < resultados.size()) ? resultados.get(fila) : null;
    }

    // ------------------------------------------------------------------
    // PDF
    // ------------------------------------------------------------------

    /** DESCARGAR PDF: guarda la orden seleccionada como archivo PDF. */
    private void descargarPdf() {
        OrdenCompra o = seleccionada();
        if (o == null && resultados.size() == 1) o = resultados.get(0);
        if (o == null) {
            aviso("Busca una orden de compra y selecciónala en la tabla para descargar su PDF.");
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar orden de compra como PDF");
        fc.setFileFilter(new FileNameExtensionFilter("Archivo PDF (*.pdf)", "pdf"));
        fc.setSelectedFile(new File(fc.getCurrentDirectory(), "OrdenCompra_" + o.numero + ".pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File destino = fc.getSelectedFile();
        if (!destino.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".pdf");
        }
        if (destino.exists()) {
            int r = JOptionPane.showConfirmDialog(this, "El archivo ya existe. ¿Deseas reemplazarlo?",
                    "Archivo existente", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) return;
        }

        try {
            OrdenPdf.generar(o, destino);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el PDF:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int abrir = JOptionPane.showConfirmDialog(this,
                "PDF guardado en:\n" + destino.getAbsolutePath() + "\n\n¿Deseas abrirlo ahora?",
                "PDF generado", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
        if (abrir == JOptionPane.YES_OPTION) {
            try {
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(destino);
            } catch (IOException ex) {
                aviso("No se pudo abrir el PDF automáticamente. Búscalo en:\n" + destino.getAbsolutePath());
            }
        }
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private void salir() {
        limpiar();
        ventana.mostrar(VentanaPrincipal.MENU);
    }

    private static String monto(double valor) {
        return String.format(Locale.US, "%.2f", valor);
    }

    /** Minúsculas y sin tildes, para que "libreria" encuentre "Librería". */
    private static String norm(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
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
