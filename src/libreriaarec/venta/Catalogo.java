package libreriaarec.venta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo / inventario de libros en memoria.
 * Lo usan el módulo de Venta (consulta y descuenta stock) y el de Inventario (registra libros).
 * Más adelante se reemplazará por la tabla de libros de la base de datos.
 */
public class Catalogo {

    /** Stock igual o menor a este valor genera una alerta. */
    public static final int STOCK_MINIMO = 5;

    public static class Libro {
        public final String codigo;
        public final String titulo;
        public final String autor;
        public final String genero;
        public final String editorial;
        public final LocalDate fechaRegistro;
        public double precio;
        public int stock;

        public Libro(String codigo, String titulo, String autor, String genero, String editorial,
                     LocalDate fechaRegistro, double precio, int stock) {
            this.codigo = codigo;
            this.titulo = titulo;
            this.autor = autor;
            this.genero = genero;
            this.editorial = editorial;
            this.fechaRegistro = fechaRegistro;
            this.precio = precio;
            this.stock = stock;
        }
    }

    private static final Map<String, Libro> LIBROS = new LinkedHashMap<>();

    static {
        ejemplo("L001", "El infinito en un junco", "Irene Vallejo", "Ensayo", "Siruela", 24.90, 15);
        ejemplo("L002", "Una habitación propia", "Virginia Woolf", "Ensayo", "Cátedra", 19.50, 4);
        ejemplo("L003", "Cien años de soledad", "Gabriel García Márquez", "Novela", "Penguin Random House", 22.00, 20);
        ejemplo("L004", "La ridícula idea de no volver a verte", "Rosa Montero", "Novela", "Lumen", 28.00, 3);
        ejemplo("L005", "Los detectives salvajes", "Roberto Bolaño", "Novela", "Anagrama", 18.90, 12);
        ejemplo("L006", "Pedro Páramo", "Juan Rulfo", "Novela", "", 15.50, 25);
    }

    private static void ejemplo(String codigo, String titulo, String autor, String genero,
                                String editorial, double precio, int stock) {
        LIBROS.put(codigo, new Libro(codigo, titulo, autor, genero, editorial, LocalDate.now(), precio, stock));
    }

    /** Busca un libro por código (sin distinguir mayúsculas). Devuelve null si no existe. */
    public static Libro buscar(String codigo) {
        if (codigo == null) return null;
        return LIBROS.get(codigo.trim().toUpperCase());
    }

    /** Busca un libro ya registrado por título y autor (sin distinguir mayúsculas). */
    public static Libro buscarPorTitulo(String titulo, String autor) {
        for (Libro l : LIBROS.values()) {
            if (l.titulo.equalsIgnoreCase(titulo.trim()) && l.autor.equalsIgnoreCase(autor.trim())) {
                return l;
            }
        }
        return null;
    }

    /** Registra un libro nuevo y le asigna el siguiente código disponible (L007, L008...). */
    public static Libro registrar(String titulo, String autor, String genero, String editorial,
                                  LocalDate fecha, double precio, int stock) {
        String codigo = String.format("L%03d", LIBROS.size() + 1);
        Libro libro = new Libro(codigo, titulo, autor, genero, editorial, fecha, precio, stock);
        LIBROS.put(codigo, libro);
        return libro;
    }

    public static List<Libro> todos() {
        return new ArrayList<>(LIBROS.values());
    }

    /** Libros cuyo stock es igual o menor al mínimo indicado. */
    public static List<Libro> stockBajo(int minimo) {
        List<Libro> r = new ArrayList<>();
        for (Libro l : LIBROS.values()) {
            if (l.stock <= minimo) r.add(l);
        }
        return r;
    }
}
