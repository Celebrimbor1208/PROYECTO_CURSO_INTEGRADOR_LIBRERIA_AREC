package libreriaarec.controlador;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.text.Collator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import libreriaarec.dao.LibroDAO;
import libreriaarec.modelo.Libro;
import libreriaarec.modelo.RegistroLibro;
import libreriaarec.util.Texto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador del módulo de Inventario y Alertas.
 * Maneja la tabla de libros "por registrar" (en orden alfabético, con buscador y filtro),
 * los guarda en el inventario real (DAO) y calcula las alertas de stock bajo.
 */
public class InventarioController {

    /** Stock igual o menor a este valor genera una alerta. */
    public static final int STOCK_MINIMO = 5;

    /** Campo por el que se filtra la tabla. */
    public enum Criterio { GENERO, AUTOR, EDITORIAL }

    /** Resumen de lo que ocurrió al guardar los libros en el inventario. */
    public static final class ResultadoRegistro {

        /** Un libro guardado: con su código y si era nuevo o ya existía. */
        public static final class Item {
            private final String codigo;
            private final String titulo;
            private final boolean nuevo;
            private final int stockFinal;

            Item(String codigo, String titulo, boolean nuevo, int stockFinal) {
                this.codigo = codigo;
                this.titulo = titulo;
                this.nuevo = nuevo;
                this.stockFinal = stockFinal;
            }

            public String getCodigo() { return codigo; }
            public String getTitulo() { return titulo; }
            public boolean isNuevo() { return nuevo; }
            public int getStockFinal() { return stockFinal; }
        }

        private final List<Item> items = new ArrayList<>();

        void agregar(Item item) { items.add(item); }

        public List<Item> getItems() { return ImmutableList.copyOf(items); }

        public int cantidadNuevos() {
            int n = 0;
            for (Item i : items) if (i.nuevo) n++;
            return n;
        }

        public int cantidadExistentes() {
            return items.size() - cantidadNuevos();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(InventarioController.class);

    private final LibroDAO libros;
    private final Collator collator = crearCollator();
    private final List<RegistroLibro> pendientes = new ArrayList<>();

    public InventarioController(LibroDAO libros) {
        this.libros = checkNotNull(libros);
    }

    /** Valida los datos escritos y arma un registro (todavía no lo agrega a la tabla). */
    public RegistroLibro crearRegistro(String titulo, String autor, String genero, String editorial,
                                       LocalDate fecha, String stockTxt, String precioTxt)
            throws ValidacionException {
        String t = Validador.obligatorio(titulo, "Escribe el nombre del libro.", "titulo");
        String a = Validador.obligatorio(autor, "Escribe el nombre del autor.", "autor");
        String g = Validador.obligatorio(genero, "Indica el género del libro.", "genero");
        String e = Validador.obligatorio(editorial, "Indica la editorial que provee el libro.", "editorial");
        int stock = Validador.enteroPositivo(stockTxt, "Ingresa un stock válido (mayor que 0).", "stock");
        double precio = Validador.montoPositivo(precioTxt, "Ingresa un precio válido (mayor que 0).", "precio");
        if (fecha == null) throw new ValidacionException("Indica la fecha de registro.", "fecha");
        return new RegistroLibro(t, a, g, e, fecha, stock, precio);
    }

    /** REGISTRAR EN TABLA: agrega el libro a la tabla de pendientes (queda en orden alfabético). */
    public void agregarPendiente(RegistroLibro registro) throws ValidacionException {
        if (existeDuplicado(registro, null)) {
            throw new ValidacionException("Ese libro (mismo título y autor) ya está en la tabla.\n"
                    + "Selecciónalo en la tabla y usa ACTUALIZAR TABLA para cambiar su stock o precio.");
        }
        pendientes.add(registro);
        ordenar();
    }

    /** ACTUALIZAR TABLA: corrige los datos de un libro pendiente (precio, stock, nombre...). */
    public void actualizarPendiente(RegistroLibro destino, RegistroLibro datos) throws ValidacionException {
        if (existeDuplicado(datos, destino)) {
            throw new ValidacionException("Ya existe otro libro con ese título y autor en la tabla.");
        }
        destino.copiarDe(datos);
        ordenar();
    }

    public void quitarPendiente(RegistroLibro registro) {
        pendientes.remove(registro);
    }

    public int cantidadPendientes() {
        return pendientes.size();
    }

    /** Todos los pendientes, en orden alfabético por título. */
    public List<RegistroLibro> pendientes() {
        return ImmutableList.copyOf(pendientes);
    }

    /**
     * Buscador (por título) y filtrador (por género, autor o editorial) de la tabla.
     * Los criterios vacíos no filtran. No distingue mayúsculas ni tildes.
     */
    public List<RegistroLibro> buscar(String titulo, Criterio criterio, String filtro) {
        List<RegistroLibro> r = new ArrayList<>();
        for (RegistroLibro reg : pendientes) {
            if (!Texto.normalizar(titulo).isEmpty() && !Texto.contiene(reg.getTitulo(), titulo)) continue;
            if (!Texto.normalizar(filtro).isEmpty()) {
                String campo = criterio == Criterio.GENERO ? reg.getGenero()
                        : criterio == Criterio.AUTOR ? reg.getAutor() : reg.getEditorial();
                if (!Texto.contiene(campo, filtro)) continue;
            }
            r.add(reg);
        }
        return r;
    }

    /**
     * REGISTRAR: guarda los libros de la tabla en el inventario. Si el libro ya existía
     * (mismo título y autor) se suma el stock y se actualiza el precio; si no, se crea con un código nuevo.
     */
    public ResultadoRegistro registrarPendientes() throws ValidacionException {
        if (pendientes.isEmpty()) {
            throw new ValidacionException("No hay libros en la tabla para registrar.");
        }
        ResultadoRegistro resultado = new ResultadoRegistro();
        for (RegistroLibro reg : pendientes) {
            Optional<Libro> existente = libros.buscarPorTituloYAutor(reg.getTitulo(), reg.getAutor());
            if (existente.isPresent()) {
                Libro libro = existente.get();
                libro.sumarStock(reg.getStock());
                libro.setPrecio(reg.getPrecio());
                libros.guardar(libro);
                resultado.agregar(new ResultadoRegistro.Item(libro.getCodigo(), libro.getTitulo(), false, libro.getStock()));
            } else {
                Libro libro = new Libro(libros.siguienteCodigo(), reg.getTitulo(), reg.getAutor(), reg.getGenero(),
                        reg.getEditorial(), reg.getFecha(), reg.getPrecio(), reg.getStock());
                libros.guardar(libro);
                resultado.agregar(new ResultadoRegistro.Item(libro.getCodigo(), libro.getTitulo(), true, libro.getStock()));
            }
        }
        LOG.info("Inventario actualizado: {} libro(s) nuevo(s), {} existente(s)",
                resultado.cantidadNuevos(), resultado.cantidadExistentes());
        pendientes.clear();
        return resultado;
    }

    /** Libros del inventario con stock bajo (alertas). */
    public List<Libro> alertasStockBajo() {
        List<Libro> r = new ArrayList<>();
        for (Libro l : libros.listar()) {
            if (l.tieneStockBajo(STOCK_MINIMO)) r.add(l);
        }
        return r;
    }

    // ------------------------------------------------------------------

    private boolean existeDuplicado(RegistroLibro candidato, RegistroLibro ignorar) {
        for (RegistroLibro r : pendientes) {
            if (r == ignorar) continue;
            if (Texto.normalizar(r.getTitulo()).equals(Texto.normalizar(candidato.getTitulo()))
                    && Texto.normalizar(r.getAutor()).equals(Texto.normalizar(candidato.getAutor()))) {
                return true;
            }
        }
        return false;
    }

    private void ordenar() {
        pendientes.sort((a, b) -> collator.compare(a.getTitulo(), b.getTitulo()));
    }

    private static Collator crearCollator() {
        Collator c = Collator.getInstance(Locale.forLanguageTag("es-PE"));
        c.setStrength(Collator.PRIMARY); // ignora mayúsculas y tildes al ordenar
        return c;
    }
}
