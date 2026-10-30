package libreriaarec.reabastecimiento;

import libreriaarec.PanelConFondo;
import libreriaarec.VentanaPrincipal;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Seguimiento de solicitudes:
 * contactar editoriales, registrar si respondieron (o no),
 * y anotar el precio y la fecha de entrega acordados.
 */
public class PanelSeguimiento extends PanelConFondo {

    private final VentanaPrincipal ventana;

    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"N°", "Fecha trámite", "Necesario para", "Libros", "Estado",
                "Editorial acordada", "Entrega acordada", "Total acordado"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final JTextArea txtDetalle = new JTextArea();

    public PanelSeguimiento(VentanaPrincipal ventana) {
        super("/libreriaarec/imagenes/fondo2.jpg");
        this.ventana = ventana;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel titulo = new JLabel("SEGUIMIENTO DE SOLICITUDES", SwingConstants.CENTER);
        titulo.setOpaque(true);
        titulo.setBackground(new Color(220, 220, 220));
        titulo.setFont(new Font("Serif", Font.BOLD, 22));
        titulo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(titulo, BorderLayout.NORTH);

        // Tabla de solicitudes (arriba) + detalle (abajo)
        tabla.setRowHeight(26);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) mostrarDetalle();
        });

        txtDetalle.setEditable(false);
        txtDetalle.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane spTabla = new JScrollPane(tabla);
        JScrollPane spDetalle = new JScrollPane(txtDetalle);
        spDetalle.setPreferredSize(new Dimension(100, 170));

        JPanel centro = new JPanel(new BorderLayout(10, 10));
        centro.setOpaque(false);
        centro.add(spTabla, BorderLayout.CENTER);
        centro.add(spDetalle, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        // Botones
        JButton btnVolver = new JButton("VOLVER");
        btnVolver.setPreferredSize(new Dimension(160, 45));
        btnVolver.addActionListener(e -> ventana.mostrar(VentanaPrincipal.REABASTECIMIENTO));

        JButton btnContactar = new JButton("CONTACTAR EDITORIAL");
        btnContactar.setPreferredSize(new Dimension(220, 45));
        btnContactar.addActionListener(e -> contactarEditorial());

        JButton btnRespuesta = new JButton("REGISTRAR RESPUESTA");
        btnRespuesta.setPreferredSize(new Dimension(220, 45));
        btnRespuesta.addActionListener(e -> registrarRespuesta());

        JButton btnMenu = new JButton("MENÚ PRINCIPAL");
        btnMenu.setPreferredSize(new Dimension(180, 45));
        btnMenu.addActionListener(e -> ventana.mostrar(VentanaPrincipal.MENU));

        JPanel sur = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        sur.setOpaque(false);
        sur.add(btnVolver);
        sur.add(btnContactar);
        sur.add(btnRespuesta);
        sur.add(btnMenu);
        add(sur, BorderLayout.SOUTH);

        // Cada vez que se muestra este panel, se actualiza la tabla
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) { refrescar(); }
        });
        refrescar();
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    private void contactarEditorial() {
        Solicitud s = seleccionada();
        if (s == null) return;
        if (s.estado == Solicitud.Estado.ACORDADA) {
            JOptionPane.showMessageDialog(this, "Esta solicitud ya tiene un acuerdo cerrado.");
            return;
        }

        // Sugiere las editoriales de los libros, pero se puede escribir otra (buscar otro proveedor)
        Set<String> sugeridas = new LinkedHashSet<>();
        for (Solicitud.Item it : s.items) sugeridas.add(it.editorial);
        JComboBox<String> cmb = new JComboBox<>(sugeridas.toArray(new String[0]));
        cmb.setEditable(true);

        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.add(new JLabel("Editorial a contactar (puedes escribir otra):"));
        p.add(cmb);

        int r = JOptionPane.showConfirmDialog(this, p, "Contactar editorial",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        String editorial = String.valueOf(cmb.getSelectedItem()).trim();
        if (editorial.isEmpty()) return;

        s.contactos.add(new Solicitud.Contacto(editorial));
        s.estado = Solicitud.Estado.EN_CONTACTO;
        refrescar();
        seleccionarPorId(s.id);
    }

    private void registrarRespuesta() {
        Solicitud s = seleccionada();
        if (s == null) return;
        if (s.estado == Solicitud.Estado.ACORDADA) {
            JOptionPane.showMessageDialog(this, "Esta solicitud ya tiene un acuerdo cerrado.");
            return;
        }

        List<Solicitud.Contacto> pendientes = new ArrayList<>();
        for (Solicitud.Contacto c : s.contactos) {
            if (c.estado == Solicitud.RespuestaContacto.ESPERANDO) pendientes.add(c);
        }
        if (pendientes.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay editoriales esperando respuesta.\nPrimero usa \"Contactar editorial\".");
            return;
        }

        JComboBox<String> cmb = new JComboBox<>();
        for (Solicitud.Contacto c : pendientes) cmb.addItem(c.editorial);

        JRadioButton rbRespondio = new JRadioButton("Respondió y hay acuerdo", true);
        JRadioButton rbNoRespondio = new JRadioButton("No respondió (buscar otra editorial)");
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(rbRespondio);
        grupo.add(rbNoRespondio);

        JTextField txtTotal = new JTextField();
        JSpinner spEntrega = Util.crearSpinnerFecha(s.fechaRequerida);

        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.add(new JLabel("Editorial:"));
        p.add(cmb);
        p.add(rbRespondio);
        p.add(rbNoRespondio);
        p.add(new JLabel("Precio total acordado (S/.):"));
        p.add(txtTotal);
        p.add(new JLabel("Fecha de entrega acordada:"));
        p.add(spEntrega);

        int r = JOptionPane.showConfirmDialog(this, p, "Registrar respuesta de editorial",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        Solicitud.Contacto contacto = pendientes.get(cmb.getSelectedIndex());

        if (rbNoRespondio.isSelected()) {
            contacto.estado = Solicitud.RespuestaContacto.SIN_RESPUESTA;
            JOptionPane.showMessageDialog(this,
                    contacto.editorial + " no respondió.\nUsa \"Contactar editorial\" para buscar a otro proveedor.");
        } else {
            double total;
            try {
                total = Util.leerMonto(txtTotal.getText());
                if (total <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "El precio acordado no es válido.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            LocalDate entrega = Util.aLocalDate(spEntrega);
            if (entrega.isBefore(s.fechaTramite)) {
                JOptionPane.showMessageDialog(this,
                        "La fecha de entrega no puede ser anterior a la del trámite.",
                        "Fecha inválida", JOptionPane.WARNING_MESSAGE);
                return;
            }
            contacto.estado = Solicitud.RespuestaContacto.RESPONDIO;
            s.estado = Solicitud.Estado.ACORDADA;
            s.editorialAcordada = contacto.editorial;
            s.totalAcordado = total;
            s.fechaEntrega = entrega;
        }
        refrescar();
        seleccionarPorId(s.id);
    }

    // ------------------------------------------------------------------
    // Utilidades de la tabla
    // ------------------------------------------------------------------

    private Solicitud seleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una solicitud de la tabla.");
            return null;
        }
        return ventana.getSolicitudes().get(fila);
    }

    private void seleccionarPorId(int id) {
        List<Solicitud> lista = ventana.getSolicitudes();
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).id == id) {
                tabla.setRowSelectionInterval(i, i);
                return;
            }
        }
    }

    public void refrescar() {
        modelo.setRowCount(0);
        for (Solicitud s : ventana.getSolicitudes()) {
            boolean acordada = s.estado == Solicitud.Estado.ACORDADA;
            modelo.addRow(new Object[]{
                s.id,
                s.fechaTramite.format(Util.FMT),
                s.fechaRequerida.format(Util.FMT),
                s.totalLibros(),
                s.estado.toString(),
                acordada ? s.editorialAcordada : "-",
                acordada ? s.fechaEntrega.format(Util.FMT) : "-",
                acordada ? Util.soles(s.totalAcordado) : "-"
            });
        }
        mostrarDetalle();
    }

    private void mostrarDetalle() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= ventana.getSolicitudes().size()) {
            txtDetalle.setText("Selecciona una solicitud para ver su detalle.");
            return;
        }
        Solicitud s = ventana.getSolicitudes().get(fila);
        StringBuilder sb = new StringBuilder();
        sb.append("SOLICITUD N° ").append(s.id).append("  -  ").append(s.estado).append("\n\n");
        sb.append("Libros:\n");
        for (Solicitud.Item it : s.items) {
            sb.append("  - ").append(it.titulo);
            if (!it.autor.isEmpty()) sb.append(" (").append(it.autor).append(")");
            sb.append(" | ").append(it.editorial).append(" | x").append(it.cantidad).append("\n");
        }
        sb.append("\nContactos con editoriales:\n");
        if (s.contactos.isEmpty()) {
            sb.append("  (aún no se ha contactado a ninguna)\n");
        }
        for (Solicitud.Contacto c : s.contactos) {
            sb.append("  - ").append(c.editorial).append(": ").append(c.estado).append("\n");
        }
        if (s.estado == Solicitud.Estado.ACORDADA) {
            sb.append("\nACUERDO: ").append(s.editorialAcordada)
              .append(" | ").append(Util.soles(s.totalAcordado))
              .append(" | entrega el ").append(s.fechaEntrega.format(Util.FMT)).append("\n");
        }
        txtDetalle.setText(sb.toString());
        txtDetalle.setCaretPosition(0);
    }
}
