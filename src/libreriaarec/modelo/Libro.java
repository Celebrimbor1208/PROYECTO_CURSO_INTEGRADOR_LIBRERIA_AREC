package libreriaarec.modelo;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDate;

/**
 * Libro del inventario de la librería (capa Modelo).
 * El código, el título y los datos de registro no cambian; el precio y el stock sí.
 */
public class Libro {

    private final String codigo;
    private final String titulo;
    private final String autor;
    private final String genero;
    private final String editorial;
    private final LocalDate fechaRegistro;
    private double precio;
    private int stock;

    public Libro(String codigo, String titulo, String autor, String genero, String editorial,
                 LocalDate fechaRegistro, double precio, int stock) {
        this.codigo = checkNotNull(codigo, "codigo");
        this.titulo = checkNotNull(titulo, "titulo");
        this.autor = checkNotNull(autor, "autor");
        this.genero = checkNotNull(genero, "genero");
        this.editorial = checkNotNull(editorial, "editorial");
        this.fechaRegistro = checkNotNull(fechaRegistro, "fechaRegistro");
        checkArgument(precio > 0, "El precio debe ser mayor que 0");
        this.precio = precio;
        checkArgument(stock >= 0, "El stock no puede ser negativo");
        this.stock = stock;
    }

    public String getCodigo() { return codigo; }
    public String getTitulo() { return titulo; }
    public String getAutor() { return autor; }
    public String getGenero() { return genero; }
    public String getEditorial() { return editorial; }
    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public double getPrecio() { return precio; }
    public int getStock() { return stock; }

    public void setPrecio(double precio) {
        checkArgument(precio > 0, "El precio debe ser mayor que 0");
        this.precio = precio;
    }

    /** Suma unidades al stock (por ejemplo, cuando llega mercadería). */
    public void sumarStock(int unidades) {
        checkArgument(unidades > 0, "Las unidades a sumar deben ser mayores que 0");
        this.stock += unidades;
    }

    /** Descuenta unidades del stock (por ejemplo, tras una venta). */
    public void descontarStock(int unidades) {
        checkArgument(unidades > 0, "Las unidades a descontar deben ser mayores que 0");
        checkArgument(unidades <= stock, "Stock insuficiente");
        this.stock -= unidades;
    }

    /** @return true si el stock es igual o menor al mínimo indicado (genera alerta). */
    public boolean tieneStockBajo(int minimo) {
        return stock <= minimo;
    }
}
