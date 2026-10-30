package libreriaarec.dao;

import java.util.List;
import java.util.Optional;
import libreriaarec.modelo.Libro;

/**
 * Acceso a los datos de los libros (patrón DAO).
 * Los controladores dependen de esta interfaz, no de cómo se guardan los datos:
 * hoy hay una implementación en memoria y mañana puede haber una con JDBC/MySQL
 * sin tocar los controladores ni las pantallas.
 */
public interface LibroDAO {

    Optional<Libro> buscarPorCodigo(String codigo);

    Optional<Libro> buscarPorTituloYAutor(String titulo, String autor);

    List<Libro> listar();

    /** Inserta el libro, o lo actualiza si ya existe un libro con ese código. */
    void guardar(Libro libro);

    /** Siguiente código libre (L001, L002, ...). */
    String siguienteCodigo();
}
