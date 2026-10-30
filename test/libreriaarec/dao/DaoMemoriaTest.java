package libreriaarec.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import libreriaarec.dao.memoria.EmpleadoDAOMemoria;
import libreriaarec.dao.memoria.LibroDAOMemoria;
import libreriaarec.dao.memoria.VentaDAOMemoria;
import libreriaarec.modelo.Empleado;
import libreriaarec.modelo.Libro;
import org.junit.Test;

public class DaoMemoriaTest {

    @Test
    public void librosSeBuscanPorCodigoYPorTituloYAutor() {
        LibroDAO dao = new LibroDAOMemoria();
        String cod = dao.siguienteCodigo();
        assertEquals("L001", cod);
        dao.guardar(new Libro(cod, "Pedro Páramo", "Juan Rulfo", "Novela", "Alfaguara", LocalDate.now(), 15, 5));
        assertTrue(dao.buscarPorCodigo("L001").isPresent());
        assertTrue(dao.buscarPorTituloYAutor("pedro paramo", "JUAN RULFO").isPresent());
        assertFalse(dao.buscarPorCodigo("L999").isPresent());
        assertEquals("L002", dao.siguienteCodigo());
    }

    @Test
    public void empleadosSePuedenActualizarCambiandoElDni() {
        EmpleadoDAO dao = new EmpleadoDAOMemoria();
        Empleado e = new Empleado("Ana", "Lopez", "11111111", "987654321", "a@b.com", "Cajero", LocalDate.now());
        dao.guardar(e);
        Empleado cambiado = new Empleado("Ana", "Lopez", "22222222", "987654321", "a@b.com", "Cajero", LocalDate.now());
        dao.actualizar("11111111", cambiado);
        assertFalse(dao.buscarPorDni("11111111").isPresent());
        assertTrue(dao.buscarPorDni("22222222").isPresent());
    }

    @Test
    public void numerosDeBoletaSonCorrelativos() {
        VentaDAO dao = new VentaDAOMemoria();
        String a = dao.siguienteNumero();
        assertTrue(a.endsWith("1"));
    }
}
