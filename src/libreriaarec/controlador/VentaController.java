package libreriaarec.controlador;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import libreriaarec.dao.LibroDAO;
import libreriaarec.dao.VentaDAO;
import libreriaarec.modelo.Libro;
import libreriaarec.modelo.Venta;
import libreriaarec.util.Texto;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador del módulo de Venta: arma la boleta (hasta 100 productos), controla el stock
 * y emite la venta. No sabe nada de pantallas (eso es la Vista) ni de cómo se guardan los
 * datos (eso es el DAO): solo coordina ambos.
 */
public class VentaController {

    public static final int MAX_LINEAS = 100;

    private static final Logger LOG = LoggerFactory.getLogger(VentaController.class);

    private final LibroDAO libros;
    private final VentaDAO ventas;
    private final Clock reloj;
    private final List<Venta.Linea> lineas = new ArrayList<>();

    public VentaController(LibroDAO libros, VentaDAO ventas, Clock reloj) {
        this.libros = checkNotNull(libros);
        this.ventas = checkNotNull(ventas);
        this.reloj = checkNotNull(reloj);
    }

    /** Busca un libro por su código para autocompletar el título y el precio. */
    public Optional<Libro> consultarLibro(String codigo) {
        return libros.buscarPorCodigo(codigo);
    }

    /**
     * Agrega un producto a la boleta. Si el libro existe en el inventario se controla el stock;
     * si el mismo libro ya está con el mismo precio, se suma la cantidad.
     */
    public void agregarLinea(String codigo, String titulo, String precioTxt, String cantidadTxt)
            throws ValidacionException {
        String cod = StringUtils.trimToEmpty(codigo).toUpperCase();
        if (cod.isEmpty()) throw new ValidacionException("Ingresa el código del libro.", "codigo");
        String tit = Validador.obligatorio(titulo, "Escribe el título del libro.", "titulo");
        double precio = Validador.montoPositivo(precioTxt, "El precio no es válido.", "precio");
        int cantidad = Validador.enteroPositivo(cantidadTxt, "Ingresa una cantidad válida (mayor que 0).", "cantidad");

        Optional<Libro> libro = libros.buscarPorCodigo(cod);
        if (libro.isPresent() && cantidadEnBoleta(cod) + cantidad > libro.get().getStock()) {
            throw new ValidacionException("Stock insuficiente. Quedan " + libro.get().getStock()
                    + " unidad(es) de \"" + libro.get().getTitulo() + "\".", "cantidad");
        }

        for (Venta.Linea l : lineas) {
            if (l.getCodigo().equals(cod) && l.getPrecio() == precio) {
                l.sumarCantidad(cantidad);
                return;
            }
        }
        if (lineas.size() >= MAX_LINEAS) {
            throw new ValidacionException("Se alcanzó el máximo de " + MAX_LINEAS + " productos por boleta.");
        }
        lineas.add(new Venta.Linea(cod, tit, cantidad, precio));
    }

    public void quitarLinea(int indice) {
        if (indice >= 0 && indice < lineas.size()) lineas.remove(indice);
    }

    public List<Venta.Linea> lineas() {
        return ImmutableList.copyOf(lineas);
    }

    public double total() {
        double t = 0;
        for (Venta.Linea l : lineas) t += l.total();
        return t;
    }

    public boolean hayVentaPendiente() {
        return !lineas.isEmpty();
    }

    /** Descarta la venta en curso sin emitirla. */
    public void cancelar() {
        lineas.clear();
    }

    /** Comprueba que la venta se pueda emitir: DNI válido y al menos un producto. */
    public void validarVenta(String dni) throws ValidacionException {
        Validador.dni(dni, "dni");
        if (lineas.isEmpty()) throw new ValidacionException("Agrega al menos un producto a la tabla.");
    }

    /**
     * Emite la venta: descuenta el stock de los libros del inventario, guarda la boleta
     * y deja el controlador listo para una nueva venta.
     */
    public Venta emitir(String dni) throws ValidacionException {
        validarVenta(dni);
        String dniLimpio = dni.trim();

        // Se vuelve a comprobar el stock antes de descontar (otra venta pudo bajarlo)
        for (Venta.Linea l : lineas) {
            Optional<Libro> libro = libros.buscarPorCodigo(l.getCodigo());
            if (libro.isPresent() && cantidadEnBoleta(l.getCodigo()) > libro.get().getStock()) {
                throw new ValidacionException("Stock insuficiente de \"" + libro.get().getTitulo()
                        + "\". Quedan " + libro.get().getStock() + " unidad(es).");
            }
        }

        Venta venta = new Venta(ventas.siguienteNumero(), LocalDateTime.now(reloj), dniLimpio, lineas);
        for (Venta.Linea l : venta.getLineas()) {
            libros.buscarPorCodigo(l.getCodigo()).ifPresent(libro -> {
                libro.descontarStock(l.getCantidad());
                libros.guardar(libro);
            });
        }
        ventas.guardar(venta);
        lineas.clear();
        LOG.info("Venta emitida {} por S/ {} (cliente {})", venta.getNumero(),
                Texto.monto(venta.total()), Texto.enmascararDni(dniLimpio));
        return venta;
    }

    private int cantidadEnBoleta(String codigo) {
        int t = 0;
        for (Venta.Linea l : lineas) {
            if (l.getCodigo().equals(codigo)) t += l.getCantidad();
        }
        return t;
    }
}
