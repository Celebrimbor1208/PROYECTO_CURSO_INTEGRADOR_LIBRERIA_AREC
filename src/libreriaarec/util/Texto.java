package libreriaarec.util;

import com.google.common.base.Strings;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

/** Utilidades de texto (usa Apache Commons Lang y Google Guava). */
public final class Texto {

    private Texto() { }

    /** Minúsculas y sin tildes, para que "habitacion" encuentre "Habitación". */
    public static String normalizar(String s) {
        return StringUtils.stripAccents(Strings.nullToEmpty(s)).toLowerCase(Locale.ROOT).trim();
    }

    /** Quita espacios al inicio y final, y deja un solo espacio entre palabras. */
    public static String limpiarEspacios(String s) {
        return StringUtils.normalizeSpace(Strings.nullToEmpty(s));
    }

    /** true si {@code texto} contiene a {@code buscado}, sin importar mayúsculas ni tildes. */
    public static boolean contiene(String texto, String buscado) {
        return normalizar(texto).contains(normalizar(buscado));
    }

    /** Oculta los primeros dígitos de un DNI para no mostrarlo completo en los registros (logs). */
    public static String enmascararDni(String dni) {
        if (dni == null || dni.length() < 4) return "****";
        return "****" + dni.substring(dni.length() - 4);
    }

    public static String monto(double valor) {
        return String.format(Locale.US, "%.2f", valor);
    }
}
