package libreriaarec.controlador;

import libreriaarec.modelo.Empleado;

/** Sesión actual: quién inició sesión. Sirve para restringir módulos según el cargo. */
public class Sesion {

    private Empleado actual;

    public void iniciar(Empleado empleado) {
        this.actual = empleado;
    }

    public void cerrar() {
        this.actual = null;
    }

    public boolean estaAutenticado() {
        return actual != null;
    }

    public Empleado actual() {
        return actual;
    }

    public boolean esAdministrador() {
        return actual != null && actual.esAdministrador();
    }
}
