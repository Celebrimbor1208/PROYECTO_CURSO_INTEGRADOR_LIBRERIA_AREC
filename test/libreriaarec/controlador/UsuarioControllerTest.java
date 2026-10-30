package libreriaarec.controlador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import libreriaarec.controlador.UsuarioController.CredencialTemporal;
import libreriaarec.dao.memoria.EmpleadoDAOMemoria;
import libreriaarec.modelo.Empleado;
import libreriaarec.util.PasswordHasher;
import org.junit.Before;
import org.junit.Test;

public class UsuarioControllerTest {

    // "Hoy" fijo: 2026-10-30
    private static final LocalDate HOY = LocalDate.of(2026, 10, 30);
    private EmpleadoDAOMemoria dao;
    private UsuarioController ctrl;

    @Before
    public void preparar() {
        dao = new EmpleadoDAOMemoria();
        ctrl = new UsuarioController(dao, new RelojFalso(Instant.parse("2026-10-30T15:00:00Z")));
    }

    private Empleado nuevo(String dni, String cargo, LocalDate fecha) throws Exception {
        return ctrl.crearEmpleado("Ana María", "Torres Díaz", dni, "987654321", "ana@correo.com", cargo, fecha);
    }

    @Test
    public void validaLosDatosPersonales() {
        String[][] malos = {
            {"Ana1", "Torres", "12345678", "987654321", "a@b.com"},
            {"Ana", "Torres", "1234567", "987654321", "a@b.com"},
            {"Ana", "Torres", "12345678", "887654321", "a@b.com"},
            {"Ana", "Torres", "12345678", "987654321", "sin-arroba"},
        };
        for (String[] m : malos) {
            try {
                ctrl.crearEmpleado(m[0], m[1], m[2], m[3], m[4], "Cajero", HOY);
                fail("Debió rechazar " + String.join(",", m));
            } catch (ValidacionException esperado) {
                assertTrue(esperado.getCampo() != null);
            }
        }
    }

    @Test
    public void noAceptaFechaFuturaNiCargoInvalido() {
        try { nuevo("12345678", "Cajero", HOY.plusDays(1)); fail(); } catch (Exception e) { assertTrue(e instanceof ValidacionException); }
        try { nuevo("12345678", "Gerente", HOY); fail(); } catch (Exception e) { assertTrue(e instanceof ValidacionException); }
    }

    @Test
    public void noPermiteDniRepetido() throws Exception {
        ctrl.agregarPendiente(nuevo("12345678", "Cajero", HOY));
        try { ctrl.agregarPendiente(nuevo("12345678", "Vendedor", HOY)); fail(); }
        catch (ValidacionException ex) { assertEquals("dni", ex.getCampo()); }
    }

    @Test
    public void registrarGuardaConHashYEntregaClaveTemporalQueFunciona() throws Exception {
        ctrl.agregarPendiente(nuevo("12345678", "Cajero", HOY));
        List<CredencialTemporal> cred = ctrl.registrarPendientes();
        assertEquals(1, cred.size());
        Empleado guardado = dao.buscarPorDni("12345678").get();
        assertTrue(guardado.isRegistrado());
        assertFalse(guardado.getClaveHash().contains(cred.get(0).getClave()));
        assertTrue(PasswordHasher.verificar(cred.get(0).getClave().toCharArray(), guardado.getClaveHash()));
        assertEquals(0, ctrl.cantidadPendientes());
    }

    @Test
    public void ascensoSoloConDoceMesesYRegistrado() throws Exception {
        Empleado nuevoRegistrado = nuevo("11111111", "Cajero", HOY.minusMonths(5));
        ctrl.agregarPendiente(nuevoRegistrado);
        try { ctrl.ascender(nuevoRegistrado); fail("Pendiente no asciende"); } catch (ValidacionException ok) { assertTrue(true); }
        ctrl.registrarPendientes();
        try { ctrl.ascender(nuevoRegistrado); fail("5 meses no alcanzan"); } catch (ValidacionException ok) { assertFalse(ctrl.puedeAscender(nuevoRegistrado)); }

        Empleado veterano = nuevo("22222222", "Vendedor", HOY.minusMonths(12));
        ctrl.agregarPendiente(veterano);
        ctrl.registrarPendientes();
        assertTrue(ctrl.puedeAscender(veterano));
        assertEquals(1, ctrl.cantidadAptosParaAscenso());
        ctrl.ascender(veterano);
        assertEquals("Administrador", dao.buscarPorDni("22222222").get().getCargo());
        try { ctrl.ascender(veterano); fail("Ya es administrador"); } catch (ValidacionException ok) { assertTrue(true); }
    }

    @Test
    public void buscadorPorNombreDniYCargo() throws Exception {
        ctrl.agregarPendiente(nuevo("11111111", "Cajero", HOY));
        ctrl.agregarPendiente(ctrl.crearEmpleado("Luis", "Rojas", "22222222", "912345678", "l@x.com", "Vendedor", HOY));
        assertEquals(1, ctrl.buscar("luis", "", null).size());
        assertEquals(1, ctrl.buscar("", "2222", null).size());
        assertEquals(1, ctrl.buscar("", "", "Cajero").size());
        assertEquals(0, ctrl.buscar("luis", "", "Cajero").size());
        assertEquals(2, ctrl.buscar("", "", "").size());
    }

    @Test
    public void textoDeAntiguedad() throws Exception {
        assertEquals("1 año 3 meses", ctrl.antiguedadTexto(nuevo("11111111", "Cajero", HOY.minusMonths(15))));
        assertEquals("Menos de 1 mes", ctrl.antiguedadTexto(nuevo("22222222", "Cajero", HOY)));
    }
}
