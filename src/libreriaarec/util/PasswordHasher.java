package libreriaarec.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Seguridad de contraseñas: nunca se guardan en texto plano.
 * Se usa PBKDF2 con HMAC-SHA256, una sal aleatoria distinta por contraseña y
 * muchas iteraciones, para que adivinar una contraseña sea lento.
 * Formato guardado: {@code pbkdf2$iteraciones$sal(base64)$hash(base64)}.
 */
public final class PasswordHasher {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 120_000;
    private static final int BITS_CLAVE = 256;
    private static final int BYTES_SAL = 16;
    private static final String ALFABETO_TEMPORAL = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private PasswordHasher() { }

    /** Calcula el hash de una contraseña. */
    public static String hash(char[] clave) {
        byte[] sal = new byte[BYTES_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] derivada = derivar(clave, sal, ITERACIONES);
        return "pbkdf2$" + ITERACIONES + "$" + Base64.getEncoder().encodeToString(sal)
                + "$" + Base64.getEncoder().encodeToString(derivada);
    }

    /** Comprueba una contraseña contra el hash guardado (comparación de tiempo constante). */
    public static boolean verificar(char[] clave, String almacenado) {
        if (clave == null || almacenado == null) return false;
        String[] partes = almacenado.split("\\$");
        if (partes.length != 4 || !"pbkdf2".equals(partes[0])) return false;
        try {
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] calculado = derivar(clave, sal, iteraciones);
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException e) {
            return false; // hash mal formado
        }
    }

    /** Genera una contraseña temporal aleatoria (sin caracteres que se confundan, como 0/O o 1/l). */
    public static String claveTemporal() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(ALFABETO_TEMPORAL.charAt(ALEATORIO.nextInt(ALFABETO_TEMPORAL.length())));
        }
        return sb.toString();
    }

    private static byte[] derivar(char[] clave, byte[] sal, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(clave, sal, iteraciones, BITS_CLAVE);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("El algoritmo " + ALGORITMO + " no está disponible", e);
        } finally {
            spec.clearPassword();
        }
    }
}
