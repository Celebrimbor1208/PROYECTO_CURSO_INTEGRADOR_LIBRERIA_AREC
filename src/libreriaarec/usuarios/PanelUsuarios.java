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

/**
 * Módulo de Registro de Usuario / Administrador.
 * 1) Se llenan los datos del trabajador y se pasan a la tabla (REGISTRAR EN TABLA).
 * 2) En la tabla se puede buscar (nombre, DNI, cargo), corregir o quitar.
 * 3) El botón REGISTRAR envía los pendientes a la base de datos (Personal).
 * 4) Quien cumple el tiempo mínimo en la librería puede ascender a Administrador.
 */
public class PanelUsuarios extends PanelConFondo {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Collator COLLATOR = crearCollator();
    private static final String TODOS = "Todos los cargos";

    private static final Color COLOR_PENDIENTE = new Color(255, 248, 200);
    private static final Color COLOR_ASCENSO = new Color(200, 235, 200);

    private final VentanaPrincipal ventana;
    private final List<Empleado> pendientes = new ArrayList<>(); // en tabla, aún no enviados a la BD
    private final List<Empleado> vista = new ArrayList<>();      // lo que se ve (según buscadores)

    // Formulario
    private final JTextField txtNombres = new JTextField();
    private final JTextField txtApellidos = new JTextField();
    private final JTextField txtDni = new JTextField();
    private final JTextField txtCelular = new JTextField();
    private final JTextField txtCorreo = new JTextField();
    private final JComboBox<String> cmbCargo = new JComboBox<>(Personal.CARGOS);
    private final JSpinner spFecha = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));

    // Buscadores
    private final JTextField txtBuscaNombre = new JTextField(18);
    private final JTextField txtBuscaDni = new JTextField(9);
    private final JComboBox<String> cmbFiltroCargo = new JComboBox<>();

    // Tabla
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"N°", "Nombres", "Apellidos", "DNI", "Celular", "Correo", "Cargo",
                "Inscripción", "Antigüedad", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final JLabel lblResumen = new JLabel(" ");
    private final JButton btnAscender = new JButton();

    public PanelUsuarios(VentanaPrincipal ventana) {
        super("/libreriaarec/imagenes/fondo2.jpg");
        this.ventana = ventana;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        spFecha.setEditor(new JSpinner.DateEditor(spFecha, "dd/MM/yyyy"));
        cmbFiltroCargo.addItem(TODOS);
        for (String c : Personal.CARGOS) cmbFiltroCargo.addItem(c);
        limpiarFormulario();

        // ----- Título -----
        JLabel titulo = new JLabel("MÓDULO DE REGISTRO DE USUARIO / ADMINISTRADOR", SwingConstants.CENTER);
        titulo.setOpaque(true);
        titulo.setBackground(new Color(220, 220, 220));
        titulo.setFont(new Font("Serif", Font.BOLD, 22));
        titulo.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        add(titulo, BorderLayout.NORTH);

        // ----- Formulario + botones + buscadores -----
        JPanel superior = new JPanel(new BorderLayout(0, 4));
        superior.setOpaque(false);
        superior.add(crearFormulario(), BorderLayout.NORTH);
        superior.add(crearBuscadores(), BorderLayout.SOUTH);

        // ----- Tabla -----
        tabla.setRowHeight(24);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        int[] anchos = {35, 110, 140, 80, 90, 170, 130, 90, 110, 85};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                                                           boolean foco, int fila, int columna) {
                Component c = super.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
                if (!seleccionada) {
                    Color fondo = Color.WHITE;
                    if (fila < vista.size()) {
                        Empleado e = vista.get(fila);
                        if (!e.registrado) fondo = COLOR_PENDIENTE;
                        else if (e.cumpleTiempoParaAdmin()) fondo = COLOR_ASCENSO;
                    }
                    c.setBackground(fondo);
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });
        // Al seleccionar a un trabajador, sus datos pasan al formulario para corregirlos
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int fila = tabla.getSelectedRow();
            if (fila >= 0 && fila < vista.size()) cargarFormulario(vista.get(fila));
        });

        lblResumen.setForeground(Color.WHITE);
        lblResumen.setFont(new Font("Serif", Font.BOLD, 14));

        JLabel leyenda = new JLabel("  Amarillo: pendiente de registrar   |   Verde: cumple el tiempo para ser administrador");
        leyenda.setForeground(new Color(230, 230, 230));
        leyenda.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JPanel encabezadoTabla = new JPanel(new BorderLayout());
        encabezadoTabla.setOpaque(false);
        encabezadoTabla.add(lblResumen, BorderLayout.NORTH);
        encabezadoTabla.add(leyenda, BorderLayout.SOUTH);

        JPanel panelTabla = new JPanel(new BorderLayout(0, 4));
        panelTabla.setOpaque(false);
        panelTabla.add(encabezadoTabla, BorderLayout.NORTH);
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

        btnAscender.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnAscender.setPreferredSize(new Dimension(300, 46));
        btnAscender.setToolTipText("Selecciona un trabajador en verde y asciéndelo a Administrador");
        btnAscender.addActionListener(e -> ascender());

        JButton btnRegistrar = new JButton("REGISTRAR");
        btnRegistrar.setFont(new Font("Serif", Font.BOLD, 18));
        btnRegistrar.setBackground(new Color(220, 220, 220));
        btnRegistrar.setPreferredSize(new Dimension(240, 46));
        btnRegistrar.setToolTipText("Envía a la base de datos a los trabajadores pendientes (amarillo)");
        btnRegistrar.addActionListener(e -> registrarEnBaseDeDatos());

        JPanel centroSur = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centroSur.setOpaque(false);
        centroSur.add(btnAscender);

        JPanel sur = new JPanel(new BorderLayout());
        sur.setOpaque(false);
        sur.add(btnSalir, BorderLayout.WEST);
        sur.add(centroSur, BorderLayout.CENTER);
        sur.add(btnRegistrar, BorderLayout.EAST);
        add(sur, BorderLayout.SOUTH);

        refrescar();
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

        soloDigitos(txtDni, 8);
        soloDigitos(txtCelular, 9);

        colocar(f, c, 0, 0, "NOMBRES COMPLETOS", txtNombres, 2.0);
        colocar(f, c, 0, 2, "DNI", txtDni, 1.0);
        colocar(f, c, 1, 0, "APELLIDOS COMPLETOS", txtApellidos, 2.0);
        colocar(f, c, 1, 2, "CELULAR", txtCelular, 1.0);
        colocar(f, c, 2, 0, "CARGO", cmbCargo, 2.0);
        colocar(f, c, 2, 2, "CORREO ELECTRÓNICO", txtCorreo, 1.0);
        colocar(f, c, 3, 0, "FECHA DE INSCRIPCIÓN", spFecha, 2.0);

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

    private JPanel crearBuscadores() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        p.setOpaque(false);

        p.add(etiqueta("BUSCAR POR NOMBRE"));
        txtBuscaNombre.setToolTipText("Escribe parte del nombre o del apellido");
        p.add(txtBuscaNombre);

        p.add(etiqueta("DNI"));
        soloDigitos(txtBuscaDni, 8);
        txtBuscaDni.setToolTipText("Escribe el DNI completo o parte de él");
        p.add(txtBuscaDni);

        p.add(etiqueta("CARGO"));
        p.add(cmbFiltroCargo);

        // La tabla se filtra mientras se escribe
        DocumentListener alCambiar = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refrescar(); }
            @Override public void removeUpdate(DocumentEvent e) { refrescar(); }
            @Override public void changedUpdate(DocumentEvent e) { refrescar(); }
        };
        txtBuscaNombre.getDocument().addDocumentListener(alCambiar);
        txtBuscaDni.getDocument().addDocumentListener(alCambiar);
        cmbFiltroCargo.addActionListener(e -> refrescar());
        return p;
    }

    private JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Serif", Font.BOLD, 15));
        return l;
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    /** REGISTRAR EN TABLA: pasa los datos del formulario a la tabla (aún no a la base de datos). */
    private void registrarEnTabla() {
        Empleado nuevo = leerFormulario();
        if (nuevo == null) return;
        if (existeDni(nuevo.dni, null)) {
            aviso("Ya existe un trabajador con el DNI " + nuevo.dni + ".\n"
                    + "Búscalo en la tabla y usa ACTUALIZAR TABLA si necesitas corregir sus datos.");
            return;
        }
        pendientes.add(nuevo);
        limpiarFormulario();
        tabla.clearSelection();
        refrescar();
        txtNombres.requestFocusInWindow();
    }

    /** ACTUALIZAR TABLA: aplica los cambios del formulario al trabajador seleccionado. */
    private void actualizarTabla() {
        Empleado destino = seleccionado();
        if (destino == null) return;
        Empleado editado = leerFormulario();
        if (editado == null) return;
        if (existeDni(editado.dni, destino)) {
            aviso("Ya existe otro trabajador con el DNI " + editado.dni + ".");
            return;
        }
        destino.copiarDe(editado);
        limpiarFormulario();
        tabla.clearSelection();
        refrescar();
        JOptionPane.showMessageDialog(this, "Datos del trabajador actualizados.");
    }

    /** QUITAR DE LA TABLA: solo para los pendientes (amarillo) anotados por error. */
    private void quitarDeTabla() {
        Empleado e = seleccionado();
        if (e == null) return;
        if (e.registrado) {
            aviso("Este trabajador ya está registrado en la base de datos y no se puede quitar desde aquí.");
            return;
        }
        pendientes.remove(e);
        limpiarFormulario();
        tabla.clearSelection();
        refrescar();
    }

    /** BORRAR DATOS: limpia el formulario. */
    private void borrarDatos() {
        limpiarFormulario();
        tabla.clearSelection();
        txtNombres.requestFocusInWindow();
    }

    /** REGISTRAR: envía los trabajadores pendientes a la base de datos. */
    private void registrarEnBaseDeDatos() {
        if (pendientes.isEmpty()) {
            aviso("No hay trabajadores pendientes (en amarillo) para registrar.");
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
                "¿Registrar " + pendientes.size() + " trabajador(es) en la base de datos?",
                "Confirmar registro", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;

        int n = pendientes.size();
        for (Empleado e : pendientes) Personal.registrar(e);
        pendientes.clear();
        limpiarFormulario();
        tabla.clearSelection();
        refrescar();
        JOptionPane.showMessageDialog(this, n + " trabajador(es) registrado(s) correctamente.",
                "Registro completado", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Asciende al trabajador seleccionado a Administrador si ya cumplió el tiempo requerido. */
    private void ascender() {
        Empleado e = seleccionado();
        if (e == null) return;
        if (!e.registrado) {
            aviso("Primero registra al trabajador en la base de datos con el botón REGISTRAR.");
            return;
        }
        if (e.esAdministrador()) {
            aviso(e.nombreCompleto() + " ya es Administrador.");
            return;
        }
        if (!e.cumpleTiempoParaAdmin()) {
            aviso(e.nombreCompleto() + " tiene " + e.mesesAntiguedad() + " mes(es) en la librería.\n"
                    + "Se necesitan " + Personal.MESES_PARA_ADMIN + " meses para ser Administrador.");
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
                "¿Ascender a " + e.nombreCompleto() + " de " + e.cargo + " a Administrador?\n"
                        + "Antigüedad: " + antiguedad(e) + ".",
                "Confirmar ascenso", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;

        e.cargo = Personal.ADMINISTRADOR;
        limpiarFormulario();
        tabla.clearSelection();
        refrescar();
    }

    private void salir() {
        if (!pendientes.isEmpty()) {
            int r = JOptionPane.showConfirmDialog(this,
                    "Hay " + pendientes.size() + " trabajador(es) sin registrar. ¿Salir y descartarlos?",
                    "Registros pendientes", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) return;
        }
        pendientes.clear();
        limpiarFormulario();
        txtBuscaNombre.setText("");
        txtBuscaDni.setText("");
        cmbFiltroCargo.setSelectedIndex(0);
        refrescar();
        ventana.mostrar(VentanaPrincipal.MENU);
    }

    private Empleado seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= vista.size()) {
            aviso("Selecciona un trabajador de la tabla.");
            return null;
        }
        return vista.get(fila);
    }

    // ------------------------------------------------------------------
    // Formulario
    // ------------------------------------------------------------------

    /** Lee y valida el formulario. Devuelve null (y avisa) si hay algún dato incorrecto. */
    private Empleado leerFormulario() {
        String nombres = limpiarEspacios(txtNombres.getText());
        String apellidos = limpiarEspacios(txtApellidos.getText());
        String dni = txtDni.getText().trim();
        String celular = txtCelular.getText().trim();
        String correo = txtCorreo.getText().trim();
        String cargo = String.valueOf(cmbCargo.getSelectedItem());

        if (!nombres.matches("[\\p{L} '.-]{2,}")) {
            aviso("Escribe los nombres completos (solo letras).");
            txtNombres.requestFocusInWindow();
            return null;
        }
        if (!apellidos.matches("[\\p{L} '.-]{2,}")) {
            aviso("Escribe los apellidos completos (solo letras).");
            txtApellidos.requestFocusInWindow();
            return null;
        }
        if (!dni.matches("\\d{8}")) {
            aviso("El DNI debe tener 8 dígitos.");
            txtDni.requestFocusInWindow();
            return null;
        }
        if (!celular.matches("9\\d{8}")) {
            aviso("El celular debe tener 9 dígitos y empezar con 9.");
            txtCelular.requestFocusInWindow();
            return null;
        }
        if (!correo.matches("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+")) {
            aviso("Escribe un correo electrónico válido (ejemplo: nombre@correo.com).");
            txtCorreo.requestFocusInWindow();
            return null;
        }
        LocalDate fecha = ((Date) spFecha.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (fecha.isAfter(LocalDate.now())) {
            aviso("La fecha de inscripción no puede ser futura.");
            return null;
        }
        return new Empleado(nombres, apellidos, dni, celular, correo, cargo, fecha);
    }

    private void cargarFormulario(Empleado e) {
        txtNombres.setText(e.nombres);
        txtApellidos.setText(e.apellidos);
        txtDni.setText(e.dni);
        txtCelular.setText(e.celular);
        txtCorreo.setText(e.correo);
        cmbCargo.setSelectedItem(e.cargo);
        spFecha.setValue(Date.from(e.fechaInscripcion.atStartOfDay(ZoneId.systemDefault()).toInstant()));
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

    // ------------------------------------------------------------------
    // Tabla: orden, filtros y utilidades
    // ------------------------------------------------------------------

    /** Redibuja la tabla aplicando los buscadores (nombre, DNI) y el filtro de cargo. */
    private void refrescar() {
        String q = norm(txtBuscaNombre.getText());
        String dni = txtBuscaDni.getText().trim();
        String cargo = String.valueOf(cmbFiltroCargo.getSelectedItem());
        boolean filtrarCargo = !TODOS.equals(cargo);

        List<Empleado> todos = new ArrayList<>(Personal.todos());
        todos.addAll(pendientes);
        todos.sort((a, b) -> {
            int r = COLLATOR.compare(a.apellidos, b.apellidos);
            return r != 0 ? r : COLLATOR.compare(a.nombres, b.nombres);
        });

        vista.clear();
        modelo.setRowCount(0);
        int n = 1;
        int aptos = 0;
        for (Empleado e : todos) {
            if (e.cumpleTiempoParaAdmin()) aptos++;
            if (!q.isEmpty() && !norm(e.nombreCompleto()).contains(q)) continue;
            if (!dni.isEmpty() && !e.dni.contains(dni)) continue;
            if (filtrarCargo && !e.cargo.equals(cargo)) continue;
            vista.add(e);
            modelo.addRow(new Object[]{String.valueOf(n++), e.nombres, e.apellidos, e.dni, e.celular,
                e.correo, e.cargo, e.fechaInscripcion.format(FMT), antiguedad(e),
                e.registrado ? "Registrado" : "Pendiente"});
        }
        lblResumen.setText("  Mostrando " + vista.size() + " de " + todos.size() + " trabajador(es)"
                + "   |   Pendientes de registrar: " + pendientes.size());
        btnAscender.setText("ASCENDER A ADMINISTRADOR (" + aptos + " apto" + (aptos == 1 ? "" : "s") + ")");
        btnAscender.setBackground(aptos > 0 ? new Color(200, 235, 200) : new Color(220, 220, 220));
    }

    private boolean existeDni(String dni, Empleado ignorar) {
        for (Empleado e : Personal.todos()) {
            if (e != ignorar && e.dni.equals(dni)) return true;
        }
        for (Empleado e : pendientes) {
            if (e != ignorar && e.dni.equals(dni)) return true;
        }
        return false;
    }

    /** Texto de antigüedad: "1 año 3 meses", "5 meses", "Menos de 1 mes". */
    private static String antiguedad(Empleado e) {
        long meses = e.mesesAntiguedad();
        long anios = meses / 12;
        long resto = meses % 12;
        if (anios == 0 && resto == 0) return "Menos de 1 mes";
        StringBuilder sb = new StringBuilder();
        if (anios > 0) sb.append(anios).append(anios == 1 ? " año" : " años");
        if (resto > 0) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(resto).append(resto == 1 ? " mes" : " meses");
        }
        return sb.toString();
    }

    private static String limpiarEspacios(String s) {
        return s.trim().replaceAll("\\s+", " ");
    }

    /** Minúsculas y sin tildes, para que "lucia" encuentre "Lucía". */
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
