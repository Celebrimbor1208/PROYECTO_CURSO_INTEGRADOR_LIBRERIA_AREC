package libreriaarec.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordHasherTest {

    @Test
    public void laClaveCorrectaSeVerifica() {
        String h = PasswordHasher.hash("Admin2026".toCharArray());
        assertTrue(PasswordHasher.verificar("Admin2026".toCharArray(), h));
    }

    @Test
    public void unaClaveIncorrectaNoSeVerifica() {
        String h = PasswordHasher.hash("Admin2026".toCharArray());
        assertFalse(PasswordHasher.verificar("admin2026".toCharArray(), h));
    }

    @Test
    public void elHashNoContieneLaClaveYUsaSalDistintaCadaVez() {
        String a = PasswordHasher.hash("Clave123".toCharArray());
        String b = PasswordHasher.hash("Clave123".toCharArray());
        assertFalse(a.contains("Clave123"));
        assertNotEquals(a, b);
        assertTrue(a.startsWith("pbkdf2$"));
    }

    @Test
    public void unHashMalFormadoOVacioNuncaVerifica() {
        assertFalse(PasswordHasher.verificar("x".toCharArray(), null));
        assertFalse(PasswordHasher.verificar("x".toCharArray(), "basura"));
    }

    @Test
    public void laClaveTemporalTieneLongitudSuficienteYCambia() {
        String a = PasswordHasher.claveTemporal();
        assertTrue(a.length() >= 10);
        assertNotEquals(a, PasswordHasher.claveTemporal());
    }

    @Test
    public void textoUtilitarios() {
        assertTrue(Texto.enmascararDni("12345678").endsWith("5678"));
        assertFalse(Texto.enmascararDni("12345678").contains("1234"));
        assertTrue(Texto.contiene("Cien años de soledad", "ANOS"));
        assertEquals("22.00", Texto.monto(22));
    }
}
