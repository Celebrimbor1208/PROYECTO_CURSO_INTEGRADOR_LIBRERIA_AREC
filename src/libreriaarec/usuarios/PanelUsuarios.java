package libreriaarec.usuarios;

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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import libreriaarec.PanelConFondo;
import libreriaarec.VentanaPrincipal;
import libreriaarec.controlador.UsuarioController;
import libreriaarec.controlador.UsuarioController.CredencialTemporal;
import libreriaarec.controlador.ValidacionException;
import libreriaarec.modelo.Cargos;
import libreriaarec.modelo.Empleado;

/**
 * Vista del módulo de Registro de Usuario / Administrador (solo accesible para Administradores).
 * Filas amarillas = pendientes de registrar; verdes = ya pueden ascender a Administrador.
 */
public class PanelUsuarios extends PanelConFondo {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color AMARILLO = new Color(255, 245, 157);
    private static final Color VERDE = new Color(200, 235, 200);

    private final VentanaPrincipal ventana;
    private final UsuarioController controlador;
    private final List<Empleado> vista = new ArrayList<>();

    private final JTextField txtNombres = new JTextField();
    private final JTextField txtApellidos = new JTextField();
    private final JTextField txtDni = new JTextField();
    private final JTextField txtCelular = new JTextField();
    private final JTextField txtCorreo = new JTextField();
    private final JComboBox<String> cmbCargo = new JComboBox<>(Cargos.TODOS.toArray(new String[0]));
    private final JSpinner spFecha = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));

    private final JTextField txtBuscaNombre = new JTextField(14);
    private final JTextField txtBuscaDni = new JTextField(9);
    private final JComboBox<String> cmbBuscaCargo = new JComboBox<>();

    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Nombres y apellidos", "DNI", "Celular", "Correo", "Cargo", "Inscripción", "Antigüedad", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int f, int c) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final JLabel lblResumen = new JLabel(" ");

    public PanelUsuarios(VentanaPrincipal ventana, UsuarioController controlador) {
        super("/libreriaarec/imagenes/fondo2.jpg");
        this.ventana = ventana;
        this.controlador = controlador;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        spFecha.setEditor(new JSpinner.DateEditor(spFecha, "dd/MM/yyyy"));
        soloDigitos(txtDni, 8);
        soloDigitos(txtBuscaDni, 8);
        soloDigitos(txtCelular, 9);
        cmbBuscaCargo.addItem("Todos los cargos");
        for (String c : Cargos.TODOS) cmbBuscaCargo.addItem(c);

        JLabel titulo = new JLabel("MÓDULO DE REGISTRO DE USUARIO / ADMINISTRADOR", SwingConstants.CENTER);
        titulo.setOpaque(true);
        titulo.setBackground(new Color(220, 220, 220));
        titulo.setFont(new Font("Serif", Font.BOLD, 22));
        titulo.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        add(titulo, BorderLayout.NORTH);

        JPanel superior = new JPanel(new BorderLayout(0, 4));
        superior.setOpaque(false);
        superior.add(crearFormulario(), BorderLayout.NORTH);
        superior.add(crearBuscador(), BorderLayout.SOUTH);

        tabla.setRowHeight(24);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        int[] anchos = {200, 80, 85, 190, 150, 90, 110, 100};
        for (int i = 0; i < anchos.length; i++) tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco,
                                                           int fila, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foco, fila, col);
                if (!sel && fila < vista.size()) {
                    Empleado e = vista.get(fila);
                    c.setBackground(!e.isRegistrado() ? AMARILLO
                            : controlador.puedeAscender(e) ? VERDE : Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int f = tabla.getSelectedRow();
            if (f >= 0 && f < vista.size()) cargarFormulario(vista.get(f));
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

        JButton btnSalir = grande("SALIR", 200);
        btnSalir.addActionListener(e -> salir());
        JButton btnAscender = grande("ASCENDER A ADMIN.", 260);
        btnAscender.addActionListener(e -> ascender());
        JButton btnRegistrar = grande("REGISTRAR", 240);
        btnRegistrar.setToolTipText("Envía a la base de datos a los trabajadores en amarillo");
        btnRegistrar.addActionListener(e -> registrar());

        JPanel centroSur = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centroSur.setOpaque(false);
        centroSur.add(btnAscender);
        JPanel sur = new JPanel(new BorderLayout());
        sur.setOpaque(false);
        sur.add(btnSalir, BorderLayout.WEST);
        sur.add(centroSur, BorderLayout.CENTER);
        sur.add(btnRegistrar, BorderLayout.EAST);
        add(sur, BorderLayout.SOUTH);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) { refrescar(); }
        });

        limpiarFormulario();
        refrescar();
    }

    // ------------------------------------------------------------------ interfaz

    private JPanel crearFormulario() {
        JPanel f = new JPanel(new GridBagLayout());
        f.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        colocar(f, c, 0, 0, "NOMBRES", txtNombres);
        colocar(f, c, 0, 2, "APELLIDOS", txtApellidos);
        colocar(f, c, 1, 0, "DNI", txtDni);
        colocar(f, c, 1, 2, "CELULAR", txtCelular);
        colocar(f, c, 2, 0, "CORREO", txtCorreo);
        colocar(f, c, 2, 2, "CARGO", cmbCargo);
        colocar(f, c, 3, 0, "FECHA DE INSCRIPCIÓN", spFecha);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(boton("REGISTRAR EN TABLA", e -> registrarEnTabla()));
        botones.add(boton("ACTUALIZAR TABLA", e -> actualizarTabla()));
        botones.add(boton("QUITAR DE LA TABLA", e -> quitarDeTabla()));
        botones.add(boton("BORRAR DATOS", e -> { limpiarFormulario(); tabla.clearSelection(); }));
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 4;
        f.add(botones, c);
        return f;
    }

    private void colocar(JPanel p, GridBagConstraints c, int fila, int col, String texto, Component campo) {
        JLabel l = new JLabel(texto);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Serif", Font.BOLD, 15));
        c.gridx = col;
        c.gridy = fila;
        c.gridwidth = 1;
        c.weightx = 0;
        p.add(l, c);
        campo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        campo.setPreferredSize(new Dimension(200, 28));
        c.gridx = col + 1;
        c.weightx = 1;
        p.add(campo, c);
    }

    private JButton boton(String texto, java.awt.event.ActionListener accion) {
        JButton b = new JButton(texto);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setBackground(new Color(220, 220, 220));
        b.setPreferredSize(new Dimension(170, 36));
        b.addActionListener(accion);
        return b;
    }

    private JButton grande(String texto, int ancho) {
        JButton b = new JButton(texto);
        b.setFont(new Font("Serif", Font.BOLD, 18));
        b.setBackground(new Color(220, 220, 220));
        b.setPreferredSize(new Dimension(ancho, 46));
        return b;
    }

    private JPanel crearBuscador() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        p.setOpaque(false);
        p.add(etiqueta("BUSCAR POR NOMBRE"));
        p.add(txtBuscaNombre);
        p.add(etiqueta("DNI"));
        p.add(txtBuscaDni);
        p.add(etiqueta("CARGO"));
        p.add(cmbBuscaCargo);

        DocumentListener alCambiar = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refrescar(); }
            @Override public void removeUpdate(DocumentEvent e) { refrescar(); }
            @Override public void changedUpdate(DocumentEvent e) { refrescar(); }
        };
        txtBuscaNombre.getDocument().addDocumentListener(alCambiar);
        txtBuscaDni.getDocument().addDocumentListener(alCambiar);
        cmbBuscaCargo.addActionListener(e -> refrescar());
        return p;
    }

    private JLabel etiqueta(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Serif", Font.BOLD, 15));
        return l;
    }

    // ------------------------------------------------------------------ acciones

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
        txtNombres.requestFocusInWindow();
    }

    private void actualizarTabla() {
        Empleado destino = seleccionado();
        if (destino == null) return;
        try {
            controlador.actualizar(destino, leerFormulario());
        } catch (ValidacionException ex) {
            avisar(ex);
            return;
        }
        limpiarFormulario();
        tabla.clearSelection();
        refrescar();
        JOptionPane.showMessageDialog(this, "Datos del trabajador actualizados.");
    }

    private void quitarDeTabla() {
        Empleado e = seleccionado();
        if (e == null) return;
        try {
            controlador.quitarPendiente(e);
        } catch (ValidacionException ex) {
            avisar(ex);
            return;
        }
        limpiarFormulario();
        tabla.clearSelection();
        refrescar();
    }

    private void registrar() {
        if (controlador.cantidadPendientes() == 0) {
            aviso("No hay trabajadores en amarillo para registrar.");
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
                "¿Registrar " + controlador.cantidadPendientes() + " trabajador(es) en la base de datos?",
                "Confirmar registro", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;

        List<CredencialTemporal> credenciales;
        try {
            credenciales = controlador.registrarPendientes();
        } catch (ValidacionException ex) {
            avisar(ex);
            return;
        }
        limpiarFormulario();
        refrescar();

        // La contraseña temporal solo existe en este momento: en la base de datos queda únicamente su hash.
        StringBuilder sb = new StringBuilder();
        sb.append("Entrega estas credenciales a cada trabajador.\n");
        sb.append("Usuario = DNI. Esta pantalla se muestra UNA sola vez.\n\n");
        for (CredencialTemporal c : credenciales) {
            sb.append(String.format("%-28s DNI: %s   Clave: %s%n", c.getNombre(), c.getDni(), c.getClave()));
        }
        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(620, 200));
        JOptionPane.showMessageDialog(this, sp, "Credenciales temporales", JOptionPane.INFORMATION_MESSAGE);
    }

    private void ascender() {
        Empleado e = seleccionado();
        if (e == null) return;
        try {
            controlador.ascender(e); // valida antigüedad y estado
        } catch (ValidacionException ex) {
            aviso(ex.getMessage());
            return;
        }
        refrescar();
        JOptionPane.showMessageDialog(this, e.nombreCompleto() + " ahora es Administrador.");
    }

    private void salir() {
        if (controlador.cantidadPendientes() > 0) {
            int r = JOptionPane.showConfirmDialog(this,
                    "Hay " + controlador.cantidadPendientes() + " trabajador(es) sin registrar. Se mantendrán en la tabla.\n¿Salir?",
                    "Registros pendientes", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) return;
        }
        ventana.mostrar(VentanaPrincipal.MENU);
    }

    // ------------------------------------------------------------------ formulario y tabla

    private Empleado leerFormulario() throws ValidacionException {
        LocalDate fecha = ((Date) spFecha.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return controlador.crearEmpleado(txtNombres.getText(), txtApellidos.getText(), txtDni.getText(),
                txtCelular.getText(), txtCorreo.getText(), String.valueOf(cmbCargo.getSelectedItem()), fecha);
    }

    private void cargarFormulario(Empleado e) {
        txtNombres.setText(e.getNombres());
        txtApellidos.setText(e.getApellidos());
        txtDni.setText(e.getDni());
        txtCelular.setText(e.getCelular());
        txtCorreo.setText(e.getCorreo());
        cmbCargo.setSelectedItem(e.getCargo());
        spFecha.setValue(Date.from(e.getFechaInscripcion().atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    private void limpiarFormulario() {
        txtNombres.setText("");
        txtApellidos.setText("");
        txtDni.setText("");
        txtCelular.setText("");
        txtCorreo.setText("");
        cmbCargo.setSelectedIndex(0);
        spFecha.setValue(new Date());
    }

    private Empleado seleccionado() {
        int f = tabla.getSelectedRow();
        if (f < 0 || f >= vista.size()) {
            aviso("Selecciona primero un trabajador de la tabla.");
            return null;
        }
        return vista.get(f);
    }

    private void refrescar() {
        String cargo = cmbBuscaCargo.getSelectedIndex() <= 0 ? null : (String) cmbBuscaCargo.getSelectedItem();
        vista.clear();
        vista.addAll(controlador.buscar(txtBuscaNombre.getText(), txtBuscaDni.getText(), cargo));
        modelo.setRowCount(0);
        for (Empleado e : vista) {
            String estado = !e.isRegistrado() ? "Pendiente"
                    : controlador.puedeAscender(e) ? "Apto ascenso" : "Registrado";
            modelo.addRow(new Object[]{e.nombreCompleto(), e.getDni(), e.getCelular(), e.getCorreo(),
                e.getCargo(), e.getFechaInscripcion().format(FMT), controlador.antiguedadTexto(e), estado});
        }
        lblResumen.setText("  Mostrando " + vista.size() + " trabajador(es)  |  Pendientes (amarillo): "
                + controlador.cantidadPendientes() + "  |  Aptos para ascenso (verde): "
                + controlador.cantidadAptosParaAscenso());
    }

    private void avisar(ValidacionException ex) {
        aviso(ex.getMessage());
        String campo = ex.getCampo();
        if ("nombres".equals(campo)) txtNombres.requestFocusInWindow();
        else if ("apellidos".equals(campo)) txtApellidos.requestFocusInWindow();
        else if ("dni".equals(campo)) txtDni.requestFocusInWindow();
        else if ("celular".equals(campo)) txtCelular.requestFocusInWindow();
        else if ("correo".equals(campo)) txtCorreo.requestFocusInWindow();
    }

    private void aviso(String m) {
        JOptionPane.showMessageDialog(this, m, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

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
                if (nuevo.matches("\\d{0," + max + "}")) super.replace(fb, offset, longitud, texto, attr);
            }
        });
    }
}
