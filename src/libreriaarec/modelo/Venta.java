package libreriaarec.modelo;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.time.LocalDateTime;
import java.util.List;

/** Una venta emitida (boleta). Sus datos no cambian una vez creada. */
public class Venta {

    /** Una línea de la boleta (un producto). */
    public static class Linea {
        private final String codigo;
        private final String titulo;
        private final double precio;
        private int cantidad;

        public Linea(String codigo, String titulo, int cantidad, double precio) {
            this.codigo = checkNotNull(codigo);
            this.titulo = checkNotNull(titulo);
            this.cantidad = cantidad;
            this.precio = precio;
        }

        public String getCodigo() { return codigo; }
        public String getTitulo() { return titulo; }
        public double getPrecio() { return precio; }
        public int getCantidad() { return cantidad; }

        public void sumarCantidad(int unidades) { this.cantidad += unidades; }

        public double total() { return cantidad * precio; }
    }

    private final String numero;
    private final LocalDateTime fecha;
    private final String dni;
    private final ImmutableList<Linea> lineas;

    public Venta(String numero, LocalDateTime fecha, String dni, List<Linea> lineas) {
        this.numero = checkNotNull(numero);
        this.fecha = checkNotNull(fecha);
        this.dni = checkNotNull(dni);
        this.lineas = ImmutableList.copyOf(lineas);
    }

    public String getNumero() { return numero; }
    public LocalDateTime getFecha() { return fecha; }
    public String getDni() { return dni; }
    public List<Linea> getLineas() { return lineas; }

    public double total() {
        double t = 0;
        for (Linea l : lineas) t += l.total();
        return t;
    }
}
