package libreriaarec.controlador;

import libreriaarec.util.Texto;
import org.apache.commons.lang3.StringUtils;

/**
 * Reglas de validación de datos de entrada, compartidas por los controladores.
 * Validar aquí (y no solo en la pantalla) evita guardar datos incorrectos,
 * sin importar desde dónde se llame.
 */
public final class Validador {

    private Validador() { }

    public static String dni(String valor, String campo) throws ValidacionException {
        String v = StringUtils.trimToEmpty(valor);
        if (!v.matches("\\d{8}")) {
            throw new ValidacionException("El DNI debe tener 8 dígitos.", campo);
        }
        return v;
    }

    public static String celular(String valor, String campo) throws ValidacionException {
        String v = StringUtils.trimToEmpty(valor);
        if (!v.matches("9\\d{8}")) {
            throw new ValidacionException("El celular debe tener 9 dígitos y empezar con 9.", campo);
        }
        return v;
    }

    public static String correo(String valor, String campo) throws ValidacionException {
        String v = StringUtils.trimToEmpty(valor);
        if (!v.matches("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+")) {
            throw new ValidacionException("Escribe un correo electrónico válido (ejemplo: nombre@correo.com).", campo);
        }
        return v;
    }

    /** Nombres o apellidos: solo letras, espacios y . ' - (mínimo 2 caracteres). */
    public static String nombre(String valor, String mensaje, String campo) throws ValidacionException {
        String v = Texto.limpiarEspacios(valor);
        if (!v.matches("[\\p{L} '.-]{2,}")) {
            throw new ValidacionException(mensaje, campo);
        }
        return v;
    }

    /** Texto obligatorio (no vacío). Devuelve el texto sin espacios sobrantes. */
    public static String obligatorio(String valor, String mensaje, String campo) throws ValidacionException {
        String v = Texto.limpiarEspacios(valor);
        if (v.isEmpty()) throw new ValidacionException(mensaje, campo);
        return v;
    }

    public static int enteroPositivo(String valor, String mensaje, String campo) throws ValidacionException {
        String v = StringUtils.trimToEmpty(valor);
        if (!StringUtils.isNumeric(v) || v.length() > 9) {
            throw new ValidacionException(mensaje, campo);
        }
        int n = Integer.parseInt(v);
        if (n <= 0) throw new ValidacionException(mensaje, campo);
        return n;
    }

    public static double montoPositivo(String valor, String mensaje, String campo) throws ValidacionException {
        String v = StringUtils.trimToEmpty(valor).replace(',', '.');
        try {
            double d = Double.parseDouble(v);
            if (d <= 0 || Double.isNaN(d) || Double.isInfinite(d)) throw new NumberFormatException();
            return d;
        } catch (NumberFormatException e) {
            throw new ValidacionException(mensaje, campo);
        }
    }
}
