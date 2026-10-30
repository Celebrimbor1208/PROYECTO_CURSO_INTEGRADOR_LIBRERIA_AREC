package libreriaarec.venta;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Una venta emitida (boleta). El historial servirá luego para el módulo de crecimiento. */
public class Venta {

    private static int contador = 1;

    /** Todas las ventas emitidas desde que se abrió el programa. */
    public static final List<Venta> HISTORIAL = new ArrayList<>();

    /** Una línea de la boleta (un producto). */
    public static class Linea {
        public final String codigo;
        public final String titulo;
        public final double precio;
        public int cantidad;

        public Linea(String codigo, String titulo, int cantidad, double precio) {
            this.codigo = codigo;
            this.titulo = titulo;
            this.cantidad = cantidad;
            this.precio = precio;
        }

        public double total() { return cantidad * precio; }
    }

    public final String numero;
    public final LocalDateTime fecha;
    public final String dni;
    public final List<Linea> lineas;

    public Venta(String dni, List<Linea> lineas) {
        this.numero = String.format("B001-%06d", contador++);
        this.fecha = LocalDateTime.now();
        this.dni = dni;
        this.lineas = new ArrayList<>(lineas);
    }

    public double total() {
        double t = 0;
        for (Linea l : lineas) t += l.total();
        return t;
    }
}
