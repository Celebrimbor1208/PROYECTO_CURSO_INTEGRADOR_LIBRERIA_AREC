package libreriaarec.controlador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.util.List;
import libreriaarec.controlador.InventarioController.Criterio;
import libreriaarec.controlador.InventarioController.ResultadoRegistro;
import libreriaarec.dao.memoria.LibroDAOMemoria;
import libreriaarec.modelo.Libro;
import libreriaarec.modelo.RegistroLibro;
import org.junit.Before;
import org.junit.Test;

public class InventarioControllerTest {

    private LibroDAOMemoria libros;
    private InventarioController ctrl;
    private static final LocalDate HOY = LocalDate.of(2026, 10, 30);

    @Before
    public void preparar() {
        libros = new LibroDAOMemoria();
        ctrl = new InventarioController(libros);
    }

    private void agregar(String t, String a, String g, String e, String stock, String precio) throws Exception {
        ctrl.agregarPendiente(ctrl.crearRegistro(t, a, g, e, HOY, stock, precio));
    }

    @Test
    public void laTablaQuedaEnOrdenAlfabeticoSinImportarTildes() throws Exception {
        agregar("Zorro", "A", "Novela", "Planeta", "5", "10");
        agregar("Álbum", "B", "Novela", "Planeta", "5", "10");
        agregar("Mar", "C", "Novela", "Planeta", "5", "10");
        List<RegistroLibro> l = ctrl.pendientes();
        assertEquals("Álbum", l.get(0).getTitulo());
        assertEquals("Mar", l.get(1).getTitulo());
        assertEquals("Zorro", l.get(2).getTitulo());
    }

    @Test
    public void noPermiteDuplicadosEnLaTabla() throws Exception {
        agregar("Pedro Páramo", "Juan Rulfo", "Novela", "Alfaguara", "5", "10");
        try {
            agregar("pedro paramo", "JUAN RULFO", "Novela", "Alfaguara", "3", "10");
            fail();
        } catch (ValidacionException esperado) {
            assertEquals(1, ctrl.cantidadPendientes());
        }
    }

    @Test
    public void validaStockYPrecio() {
        for (String[] d : new String[][]{{"0", "10"}, {"-3", "10"}, {"x", "10"}, {"5", "0"}, {"5", "abc"}}) {
            try {
                ctrl.crearRegistro("T", "A", "G", "E", HOY, d[0], d[1]);
                fail("Debió rechazar " + d[0] + "/" + d[1]);
            } catch (ValidacionException esperado) {
                // correcto
            }
        }
    }

    @Test
    public void buscadorYFiltradorFuncionanJuntos() throws Exception {
        agregar("Cien años de soledad", "García Márquez", "Novela", "Penguin", "5", "10");
        agregar("Una habitación propia", "Woolf", "Ensayo", "Cátedra", "5", "10");
        agregar("Pedro Páramo", "Rulfo", "Novela", "Alfaguara", "5", "10");
        assertEquals(2, ctrl.buscar("", Criterio.GENERO, "novela").size());
        assertEquals(1, ctrl.buscar("PARAMO", Criterio.GENERO, "").size());
        assertEquals(1, ctrl.buscar("", Criterio.EDITORIAL, "catedra").size());
        assertEquals(1, ctrl.buscar("pedro", Criterio.AUTOR, "rulfo").size());
        assertEquals(0, ctrl.buscar("pedro", Criterio.AUTOR, "woolf").size());
    }

    @Test
    public void registrarCreaLibrosNuevosYSumaStockDeLosExistentes() throws Exception {
        libros.guardar(new Libro("L001", "Pedro Páramo", "Rulfo", "Novela", "Alfaguara", HOY, 15, 3));
        agregar("Pedro Páramo", "Rulfo", "Novela", "Alfaguara", "10", "18");
        agregar("Libro nuevo", "Autor", "Cuento", "Planeta", "7", "20");
        ResultadoRegistro r = ctrl.registrarPendientes();
        assertEquals(1, r.cantidadNuevos());
        assertEquals(1, r.cantidadExistentes());
        assertEquals(13, libros.buscarPorCodigo("L001").get().getStock());
        assertEquals(18.0, libros.buscarPorCodigo("L001").get().getPrecio(), 0.001);
        assertEquals(2, libros.listar().size());
        assertEquals(0, ctrl.cantidadPendientes());
    }

    @Test
    public void lasAlertasListanSoloLosLibrosConStockBajo() {
        libros.guardar(new Libro("L001", "A", "x", "g", "e", HOY, 10, 5));
        libros.guardar(new Libro("L002", "B", "x", "g", "e", HOY, 10, 6));
        libros.guardar(new Libro("L003", "C", "x", "g", "e", HOY, 10, 0));
        List<Libro> bajos = ctrl.alertasStockBajo();
        assertEquals(2, bajos.size());
        assertTrue(bajos.stream().noneMatch(l -> l.getCodigo().equals("L002")));
    }

    @Test
    public void registrarSinPendientesEsError() {
        try { ctrl.registrarPendientes(); fail(); } catch (ValidacionException esperado) { assertTrue(true); }
    }
}
