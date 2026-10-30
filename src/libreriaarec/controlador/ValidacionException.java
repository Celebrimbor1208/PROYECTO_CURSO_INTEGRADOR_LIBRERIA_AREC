package libreriaarec.controlador;

/**
 * Error de validación de datos ingresados por el usuario (DNI incorrecto, stock insuficiente, etc.).
 * El mensaje se puede mostrar tal cual en pantalla; {@link #getCampo()} indica qué casilla corregir.
 */
public class ValidacionException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String campo;

    public ValidacionException(String mensaje, String campo) {
        super(mensaje);
        this.campo = campo;
    }

    public ValidacionException(String mensaje) {
        this(mensaje, null);
    }

    /** @return nombre de la casilla con el error, o null si no corresponde a una en particular. */
    public String getCampo() {
        return campo;
    }
}
