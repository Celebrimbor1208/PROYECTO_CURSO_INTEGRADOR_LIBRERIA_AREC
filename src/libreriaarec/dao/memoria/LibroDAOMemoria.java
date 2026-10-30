package libreriaarec.dao.memoria;

import com.google.common.collect.ImmutableList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import libreriaarec.dao.LibroDAO;
import libreriaarec.modelo.Libro;
import libreriaarec.util.Texto;

/** Implementación de {@link LibroDAO} que guarda los libros en memoria (se pierden al cerrar el programa). */
public class LibroDAOMemoria implements LibroDAO {

    private final Map<String, Libro> libros = new LinkedHashMap<>();

    @Override
    public Optional<Libro> buscarPorCodigo(String codigo) {
        if (codigo == null) return Optional.empty();
        return Optional.ofNullable(libros.get(codigo.trim().toUpperCase()));
    }

    @Override
    public Optional<Libro> buscarPorTituloYAutor(String titulo, String autor) {
        for (Libro l : libros.values()) {
            if (Texto.normalizar(l.getTitulo()).equals(Texto.normalizar(titulo))
                    && Texto.normalizar(l.getAutor()).equals(Texto.normalizar(autor))) {
                return Optional.of(l);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Libro> listar() {
        return ImmutableList.copyOf(libros.values());
    }

    @Override
    public void guardar(Libro libro) {
        libros.put(libro.getCodigo().toUpperCase(), libro);
    }

    @Override
    public String siguienteCodigo() {
        return String.format("L%03d", libros.size() + 1);
    }
}
