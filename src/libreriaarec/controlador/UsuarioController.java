package libreriaarec.controlador;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.text.Collator;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import libreriaarec.dao.EmpleadoDAO;
import libreriaarec.modelo.Cargos;
import libreriaarec.modelo.Empleado;
import libreriaarec.util.PasswordHasher;
import libreriaarec.util.Texto;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador del módulo de Registro de Usuario / Administrador.
 * Maneja la tabla de trabajadores pendientes de registrar, los envía a la base de datos (DAO)
 * con una contraseña temporal segura, y aplica la regla de ascenso a Administrador.
 */
public class UsuarioController {

    /** Meses de antigüedad necesarios para poder ascender a administrador. */
    public static final int MESES_PARA_ADMIN = 12;

    /** Contraseña temporal entregada al registrar un trabajador (se muestra una sola vez). */
    public static final class CredencialTemporal {
        private final String dni;
        private final String nombre;
        private final String clave;

        CredencialTemporal(String dni, String nombre, String clave) {
            this.dni = dni;
            this.nombre = nombre;
            this.clave = clave;
        }

        public String getDni() { return dni; }
        public String getNombre() { return nombre; }
        public String getClave() { return clave; }
    }

    private static final Logger LOG = LoggerFactory.getLogger(UsuarioController.class);

    private final EmpleadoDAO empleados;
    private final Clock reloj;
    private final Collator collator = crearCollator();
    private final List<Empleado> pendientes = new ArrayList<>();

    public UsuarioController(EmpleadoDAO empleados, Clock reloj) {
        this.empleados = checkNotNull(empleados);
        this.reloj = checkNotNull(reloj);
    }

    /** Valida los datos escritos y arma un trabajador (todavía no lo agrega a la tabla). */
    public Empleado crearEmpleado(String nombres, String apellidos, String dni, String celular,
                                  String correo, String cargo, LocalDate fecha) throws ValidacionException {
        String n = Validador.nombre(nombres, "Escribe los nombres completos (solo letras).", "nombres");
        String a = Validador.nombre(apellidos, "Escribe los apellidos completos (solo letras).", "apellidos");
        String d = Validador.dni(dni, "dni");
        String c = Validador.celular(celular, "celular");
        String m = Validador.correo(correo, "correo");
        if (!Cargos.TODOS.contains(cargo)) {
            throw new ValidacionException("Selecciona un cargo válido.", "cargo");
        }
        if (fecha == null) throw new ValidacionException("Indica la fecha de inscripción.", "fecha");
        if (fecha.isAfter(hoy())) {
            throw new ValidacionException("La fecha de inscripción no puede ser futura.", "fecha");
        }
        return new Empleado(n, a, d, c, m, cargo, fecha);
    }

    /** REGISTRAR EN TABLA: agrega al trabajador como pendiente (aún no está en la base de datos). */
    public void agregarPendiente(Empleado nuevo) throws ValidacionException {
        verificarDniLibre(nuevo.getDni(), null);
        pendientes.add(nuevo);
    }

    /** ACTUALIZAR TABLA: corrige los datos de un trabajador (pendiente o ya registrado). */
    public void actualizar(Empleado destino, Empleado datos) throws ValidacionException {
        verificarDniLibre(datos.getDni(), destino);
        String dniOriginal = destino.getDni();
        destino.actualizarDatos(datos);
        if (destino.isRegistrado()) empleados.actualizar(dniOriginal, destino);
    }

    /** QUITAR DE LA TABLA: solo para pendientes anotados por error. */
    public void quitarPendiente(Empleado e) throws ValidacionException {
        if (e.isRegistrado()) {
            throw new ValidacionException(
                    "Este trabajador ya está registrado en la base de datos y no se puede quitar desde aquí.");
        }
        pendientes.remove(e);
    }

    public int cantidadPendientes() {
        return pendientes.size();
    }

    /** Todos los trabajadores (registrados y pendientes), ordenados por apellido y nombre. */
    public List<Empleado> todos() {
        List<Empleado> r = new ArrayList<>(empleados.listar());
        r.addAll(pendientes);
        r.sort((a, b) -> {
            int c = collator.compare(a.getApellidos(), b.getApellidos());
            return c != 0 ? c : collator.compare(a.getNombres(), b.getNombres());
        });
        return r;
    }

    /**
     * Buscador por nombre/apellido, por DNI (completo o parcial) y por cargo.
     * Los criterios vacíos no filtran; un cargo null o vacío significa "todos".
     */
    public List<Empleado> buscar(String nombre, String dni, String cargo) {
        String dniBuscado = StringUtils.trimToEmpty(dni);
        List<Empleado> r = new ArrayList<>();
        for (Empleado e : todos()) {
            if (!Texto.normalizar(nombre).isEmpty() && !Texto.contiene(e.nombreCompleto(), nombre)) continue;
            if (!dniBuscado.isEmpty() && !e.getDni().contains(dniBuscado)) continue;
            if (StringUtils.isNotBlank(cargo) && !e.getCargo().equals(cargo)) continue;
            r.add(e);
        }
        return r;
    }

    /**
     * REGISTRAR: envía a la base de datos a todos los pendientes. A cada uno se le genera una
     * contraseña temporal aleatoria (solo se guarda su hash) que se devuelve para entregársela.
     */
    public List<CredencialTemporal> registrarPendientes() throws ValidacionException {
        if (pendientes.isEmpty()) {
            throw new ValidacionException("No hay trabajadores pendientes (en amarillo) para registrar.");
        }
        List<CredencialTemporal> credenciales = new ArrayList<>();
        for (Empleado e : pendientes) {
            String clave = PasswordHasher.claveTemporal();
            e.setClaveHash(PasswordHasher.hash(clave.toCharArray()));
            e.setRegistrado(true);
            empleados.guardar(e);
            credenciales.add(new CredencialTemporal(e.getDni(), e.nombreCompleto(), clave));
            LOG.info("Trabajador registrado: {} ({})", Texto.enmascararDni(e.getDni()), e.getCargo());
        }
        pendientes.clear();
        return ImmutableList.copyOf(credenciales);
    }

    // ------------------------------------------------------------------
    // Ascenso a administrador
    // ------------------------------------------------------------------

    /** true si ya está registrado, todavía no es administrador y cumplió el tiempo requerido. */
    public boolean puedeAscender(Empleado e) {
        return e.isRegistrado() && !e.esAdministrador() && e.mesesAntiguedad(hoy()) >= MESES_PARA_ADMIN;
    }

    public int cantidadAptosParaAscenso() {
        int n = 0;
        for (Empleado e : todos()) if (puedeAscender(e)) n++;
        return n;
    }

    /** Asciende al trabajador a Administrador si cumple las condiciones; si no, explica por qué. */
    public void ascender(Empleado e) throws ValidacionException {
        if (!e.isRegistrado()) {
            throw new ValidacionException("Primero registra al trabajador en la base de datos con el botón REGISTRAR.");
        }
        if (e.esAdministrador()) {
            throw new ValidacionException(e.nombreCompleto() + " ya es Administrador.");
        }
        if (!puedeAscender(e)) {
            throw new ValidacionException(e.nombreCompleto() + " tiene " + e.mesesAntiguedad(hoy())
                    + " mes(es) en la librería.\nSe necesitan " + MESES_PARA_ADMIN + " meses para ser Administrador.");
        }
        String anterior = e.getCargo();
        e.setCargo(Cargos.ADMINISTRADOR);
        empleados.actualizar(e.getDni(), e);
        LOG.info("Ascenso a Administrador: {} (antes: {})", Texto.enmascararDni(e.getDni()), anterior);
    }

    /** Texto de antigüedad: "1 año 3 meses", "5 meses", "Menos de 1 mes". */
    public String antiguedadTexto(Empleado e) {
        long meses = e.mesesAntiguedad(hoy());
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

    // ------------------------------------------------------------------

    private void verificarDniLibre(String dni, Empleado ignorar) throws ValidacionException {
        boolean repetido = empleados.buscarPorDni(dni).filter(e -> e != ignorar).isPresent();
        for (Empleado e : pendientes) {
            if (e != ignorar && e.getDni().equals(dni)) repetido = true;
        }
        if (repetido) {
            throw new ValidacionException("Ya existe un trabajador con el DNI " + dni + ".\n"
                    + "Búscalo en la tabla y usa ACTUALIZAR TABLA si necesitas corregir sus datos.", "dni");
        }
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    private static Collator crearCollator() {
        Collator c = Collator.getInstance(Locale.forLanguageTag("es-PE"));
        c.setStrength(Collator.PRIMARY);
        return c;
    }
}
