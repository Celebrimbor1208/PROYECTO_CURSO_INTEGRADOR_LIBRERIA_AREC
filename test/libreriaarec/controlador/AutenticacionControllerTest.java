package libreriaarec.controlador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import libreriaarec.controlador.AutenticacionController.Resultado;
import libreriaarec.controlador.AutenticacionController.Resultado.Estado;
import libreriaarec.dao.memoria.EmpleadoDAOMemoria;
import libreriaarec.modelo.Empleado;
import libreriaarec.util.PasswordHasher;
import org.junit.Before;
import org.junit.Test;

public class AutenticacionControllerTest {

    private RelojFalso reloj;
    private EmpleadoDAOMemoria dao;
    private AutenticacionController auth;

    @Before
    public void preparar() {
        reloj = new RelojFalso(Instant.parse("2026-10-30T10:00:00Z"));
        dao = new EmpleadoDAOMemoria();
        Empleado e = new Empleado("Marta", "Salazar", "70000003", "955443322", "m@x.com",
                "Administrador", LocalDate.of(2023, 1, 1));
        e.setRegistrado(true);
        e.setClaveHash(PasswordHasher.hash("Admin2026".toCharArray()));
        dao.guardar(e);
        Empleado pendiente = new Empleado("Pedro", "Pérez", "70000009", "999999999", "p@x.com",
                "Cajero", LocalDate.of(2026, 1, 1));
        dao.guardar(pendiente); // sin registrar, sin clave
        auth = new AutenticacionController(dao, reloj);
    }

    private Resultado entrar(String dni, String clave) {
        return auth.iniciarSesion(dni, clave.toCharArray());
    }

    @Test
    public void credencialesCorrectasInicianSesion() {
        Resultado r = entrar("70000003", "Admin2026");
        assertEquals(Estado.CORRECTO, r.getEstado());
        assertEquals("Marta", r.getEmpleado().getNombres());
    }

    @Test
    public void claveIncorrectaEsRechazada() {
        assertEquals(Estado.CREDENCIALES_INVALIDAS, entrar("70000003", "mala").getEstado());
    }

    @Test
    public void usuarioInexistenteOSinRegistrarEsRechazado() {
        assertEquals(Estado.CREDENCIALES_INVALIDAS, entrar("11111111", "Admin2026").getEstado());
        assertEquals(Estado.CREDENCIALES_INVALIDAS, entrar("70000009", "lo-que-sea").getEstado());
    }

    @Test
    public void tresFallosBloqueanAunConLaClaveCorrecta() {
        entrar("70000003", "a");
        entrar("70000003", "b");
        assertEquals(Estado.BLOQUEADO, entrar("70000003", "c").getEstado());
        Resultado r = entrar("70000003", "Admin2026");
        assertEquals(Estado.BLOQUEADO, r.getEstado());
        assertTrue(r.getSegundosRestantes() > 0);
        assertNull(r.getEmpleado());
    }

    @Test
    public void despuesDelBloqueoSePuedeEntrarDeNuevo() {
        entrar("70000003", "a");
        entrar("70000003", "b");
        entrar("70000003", "c");
        reloj.avanzar(Duration.ofSeconds(30));
        assertEquals(Estado.BLOQUEADO, entrar("70000003", "Admin2026").getEstado());
        reloj.avanzar(Duration.ofSeconds(31));
        assertEquals(Estado.CORRECTO, entrar("70000003", "Admin2026").getEstado());
    }

    @Test
    public void unAccesoCorrectoReiniciaElContadorDeFallos() {
        entrar("70000003", "a");
        entrar("70000003", "b");
        assertEquals(Estado.CORRECTO, entrar("70000003", "Admin2026").getEstado());
        assertEquals(Estado.CREDENCIALES_INVALIDAS, entrar("70000003", "a").getEstado());
        assertEquals(Estado.CREDENCIALES_INVALIDAS, entrar("70000003", "b").getEstado());
    }
}
