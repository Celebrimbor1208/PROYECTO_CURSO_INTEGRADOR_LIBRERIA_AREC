package libreriaarec.modelo;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDate;

/**
 * Libro que llegó con la mercadería y todavía no se guardó en el inventario
 * (el código se asigna recién al confirmar el registro).
 */
public class RegistroLibro {

    private String titulo;
    private String autor;
    private String genero;
    private String editorial;
    private LocalDate fecha;
    private int stock;
    private double precio;

    public RegistroLibro(String titulo, String autor, String genero, String editorial,
                         LocalDate fecha, int stock, double precio) {
        this.titulo = checkNotNull(titulo);
        this.autor = checkNotNull(autor);
        this.genero = checkNotNull(genero);
        this.editorial = checkNotNull(editorial);
        this.fecha = checkNotNull(fecha);
        this.stock = stock;
        this.precio = precio;
    }

    public String getTitulo() { return titulo; }
    public String getAutor() { return autor; }
    public String getGenero() { return genero; }
    public String getEditorial() { return editorial; }
    public LocalDate getFecha() { return fecha; }
    public int getStock() { return stock; }
    public double getPrecio() { return precio; }

    /** Copia todos los datos de otro registro (se usa al corregir un libro de la tabla). */
    public void copiarDe(RegistroLibro otro) {
        this.titulo = otro.titulo;
        this.autor = otro.autor;
        this.genero = otro.genero;
        this.editorial = otro.editorial;
        this.fecha = otro.fecha;
        this.stock = otro.stock;
        this.precio = otro.precio;
    }
}
