package libreriaarec.usuarios;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Un trabajador de la librería. */
public class Empleado {

    public String nombres;
    public String apellidos;
    public String dni;
    public String celular;
    public String correo;
    public String cargo;
    public LocalDate fechaInscripcion;

    /** false = está en la tabla pero aún no se envió a la base de datos. */
    public boolean registrado;

    public Empleado(String nombres, String apellidos, String dni, String celular,
                    String correo, String cargo, LocalDate fechaInscripcion) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.dni = dni;
        this.celular = celular;
        this.correo = correo;
        this.cargo = cargo;
        this.fechaInscripcion = fechaInscripcion;
    }

    public void copiarDe(Empleado o) {
        nombres = o.nombres;
        apellidos = o.apellidos;
        dni = o.dni;
        celular = o.celular;
        correo = o.correo;
        cargo = o.cargo;
        fechaInscripcion = o.fechaInscripcion;
    }

    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }

    public boolean esAdministrador() {
        return Personal.ADMINISTRADOR.equalsIgnoreCase(cargo);
    }

    /** Meses completos desde la fecha de inscripción. */
    public long mesesAntiguedad() {
        return Math.max(0, ChronoUnit.MONTHS.between(fechaInscripcion, LocalDate.now()));
    }

    /** Ya cumplió el tiempo para ser administrador (y todavía no lo es). */
    public boolean cumpleTiempoParaAdmin() {
        return registrado && !esAdministrador() && mesesAntiguedad() >= Personal.MESES_PARA_ADMIN;
    }
}
