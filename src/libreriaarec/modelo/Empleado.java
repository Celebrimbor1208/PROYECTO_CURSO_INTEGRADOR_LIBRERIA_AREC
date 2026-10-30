package libreriaarec.modelo;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Trabajador de la librería (capa Modelo).
 * La contraseña nunca se guarda en texto plano: solo su hash (ver {@code PasswordHasher}).
 */
public class Empleado {

    private String nombres;
    private String apellidos;
    private String dni;
    private String celular;
    private String correo;
    private String cargo;
    private LocalDate fechaInscripcion;

    /** false = está en la tabla pero aún no se envió a la base de datos. */
    private boolean registrado;

    /** Hash PBKDF2 de la contraseña; null si todavía no tiene acceso al sistema. */
    private String claveHash;

    public Empleado(String nombres, String apellidos, String dni, String celular,
                    String correo, String cargo, LocalDate fechaInscripcion) {
        this.nombres = checkNotNull(nombres);
        this.apellidos = checkNotNull(apellidos);
        this.dni = checkNotNull(dni);
        this.celular = checkNotNull(celular);
        this.correo = checkNotNull(correo);
        this.cargo = checkNotNull(cargo);
        this.fechaInscripcion = checkNotNull(fechaInscripcion);
    }

    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getDni() { return dni; }
    public String getCelular() { return celular; }
    public String getCorreo() { return correo; }
    public String getCargo() { return cargo; }
    public LocalDate getFechaInscripcion() { return fechaInscripcion; }
    public boolean isRegistrado() { return registrado; }
    public String getClaveHash() { return claveHash; }

    public void setCargo(String cargo) { this.cargo = checkNotNull(cargo); }
    public void setRegistrado(boolean registrado) { this.registrado = registrado; }
    public void setClaveHash(String claveHash) { this.claveHash = claveHash; }

    /** Copia los datos personales de otro empleado (no toca la contraseña ni el estado de registro). */
    public void actualizarDatos(Empleado otro) {
        this.nombres = otro.nombres;
        this.apellidos = otro.apellidos;
        this.dni = otro.dni;
        this.celular = otro.celular;
        this.correo = otro.correo;
        this.cargo = otro.cargo;
        this.fechaInscripcion = otro.fechaInscripcion;
    }

    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }

    public boolean esAdministrador() {
        return Cargos.ADMINISTRADOR.equalsIgnoreCase(cargo);
    }

    /** Meses completos que lleva en la librería hasta la fecha indicada. */
    public long mesesAntiguedad(LocalDate hoy) {
        return Math.max(0, ChronoUnit.MONTHS.between(fechaInscripcion, hoy));
    }
}
