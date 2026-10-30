package libreriaarec.controlador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.time.LocalDate;
import libreriaarec.dao.memoria.LibroDAOMemoria;
import libreriaarec.dao.memoria.VentaDAOMemoria;
import libreriaarec.modelo.Libro;
import libreriaarec.modelo.Venta;
import org.junit.Before;
import org.junit.Test;

public class VentaControllerTest {

    private LibroDAOMemoria libros;
    private VentaDAOMemoria ventas;
    private VentaController ctrl;

    @Before
    public void preparar() {
        libros = new LibroDAOMemoria();
        ventas = new VentaDAOMemoria();
        libros.guardar(new Libro("L001", "Pedro Páramo", "Juan Rulfo", "Novela", "Alfaguara",
                LocalDate.of(2026, 1, 1), 15.5, 10));
        ctrl = new VentaController(libros, ventas, new RelojFalso(Instant.parse("2026-10-30T15:00:00Z")));
    }

    @Test
    public void agregaLineasYCalculaElTotal() throws Exception {
        ctrl.agregarLinea("l001", "Pedro Páramo", "15.50", "2");
        assertEquals(1, ctrl.lineas().size());
        assertEquals(31.0, ctrl.total(), 0.001);
    }

    @Test
    public void elMismoLibroConElMismoPrecioSumaCantidad() throws Exception {
        ctrl.agregarLinea("L001", "Pedro Páramo", "15.50", "2");
        ctrl.agregarLinea("L001", "Pedro Páramo", "15.50", "3");
        assertEquals(1, ctrl.lineas().size());
        assertEquals(5, ctrl.lineas().get(0).getCantidad());
    }

    @Test
    public void noPermiteVenderMasQueElStock() throws Exception {
        ctrl.agregarLinea("L001", "Pedro Páramo", "15.50", "8");
        try {
            ctrl.agregarLinea("L001", "Pedro Páramo", "15.50", "3");
            fail("Debió rechazar por stock");
        } catch (ValidacionException ex) {
            assertEquals("cantidad", ex.getCampo());
        }
    }

    @Test
    public void rechazaCantidadYPrecioInvalidos() {
        for (String[] d : new String[][]{{"0", "15"}, {"-1", "15"}, {"abc", "15"}, {"1", "0"}, {"1", "x"}}) {
            try {
                ctrl.agregarLinea("L001", "Pedro Páramo", d[1], d[0]);
                fail("Debió rechazar " + d[0] + "/" + d[1]);
            } catch (ValidacionException esperado) {
                // correcto
            }
        }
    }

    @Test
    public void respetaElMaximoDe100Productos() throws Exception {
        for (int i = 0; i < VentaController.MAX_LINEAS; i++) {
            ctrl.agregarLinea("X" + i, "Libro " + i, "10", "1");
        }
        try {
            ctrl.agregarLinea("OTRO", "Libro extra", "10", "1");
            fail("Debió rechazar la línea 101");
        } catch (ValidacionException esperado) {
            assertEquals(100, ctrl.lineas().size());
        }
    }

    @Test
    public void emitirDescuentaStockGuardaLaVentaYLimpia() throws Exception {
        ctrl.agregarLinea("L001", "Pedro Páramo", "15.50", "4");
        Venta v = ctrl.emitir("12345678");
        assertEquals(6, libros.buscarPorCodigo("L001").get().getStock());
        assertEquals(1, ventas.listar().size());
        assertEquals(62.0, v.total(), 0.001);
        assertFalse(ctrl.hayVentaPendiente());
    }

    @Test
    public void noSeEmiteSinDniValidoOSinProductos() throws Exception {
        try { ctrl.emitir("12345678"); fail(); } catch (ValidacionException ok) { assertTrue(true); }
        ctrl.agregarLinea("L001", "Pedro Páramo", "15.50", "1");
        try { ctrl.emitir("123"); fail(); } catch (ValidacionException ex) { assertEquals("dni", ex.getCampo()); }
        assertEquals(10, libros.buscarPorCodigo("L001").get().getStock());
    }
}
