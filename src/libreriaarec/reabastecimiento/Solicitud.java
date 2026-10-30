package libreriaarec.reabastecimiento;



import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de datos de una solicitud de abastecimiento.
 * Por ahora vive en memoria; más adelante se guarda en la base de datos.
 */
public class Solicitud {

    private static int contador = 1;

    /** Etapa general de la solicitud. */
    public enum Estado {
        PENDIENTE("Pendiente de contacto"),
        EN_CONTACTO("En contacto con editoriales"),
        ACORDADA("Acordada");

        private final String texto;
        Estado(String texto) { this.texto = texto; }
        @Override public String toString() { return texto; }
    }

    /** Estado de cada contacto hecho con una editorial. */
    public enum RespuestaContacto {
        ESPERANDO("Esperando respuesta"),
        SIN_RESPUESTA("Sin respuesta"),
        RESPONDIO("Respondió");

        private final String texto;
        RespuestaContacto(String texto) { this.texto = texto; }
        @Override public String toString() { return texto; }
    }

    /** Un libro que falta. */
    public static class Item {
        public final String titulo;
        public final String autor;
        public final String editorial;
        public int cantidad;
        public final double precioReferencial; // 0 = por cotizar

        public Item(String titulo, String autor, String editorial, int cantidad, double precioReferencial) {
            this.titulo = titulo;
            this.autor = autor;
            this.editorial = editorial;
            this.cantidad = cantidad;
            this.precioReferencial = precioReferencial;
        }

        public double subtotal() { return cantidad * precioReferencial; }
    }

    /** Un intento de contacto con una editorial (proveedor). */
    public static class Contacto {
        public final String editorial;
        public RespuestaContacto estado = RespuestaContacto.ESPERANDO;

        public Contacto(String editorial) { this.editorial = editorial; }
    }

    public final int id;
    public final LocalDate fechaTramite;
    public final LocalDate fechaRequerida;   // para cuándo se necesitan los libros
    public final List<Item> items;
    public final List<Contacto> contactos = new ArrayList<>();

    public Estado estado = Estado.PENDIENTE;
    public String editorialAcordada = "";
    public LocalDate fechaEntrega;           // fecha acordada con la editorial
    public double totalAcordado;

    public Solicitud(LocalDate fechaTramite, LocalDate fechaRequerida, List<Item> items) {
        this.id = contador++;
        this.fechaTramite = fechaTramite;
        this.fechaRequerida = fechaRequerida;
        this.items = items;
    }

    public int totalLibros() {
        int t = 0;
        for (Item i : items) t += i.cantidad;
        return t;
    }

    public double totalReferencial() {
        double t = 0;
        for (Item i : items) t += i.subtotal();
        return t;
    }
}
