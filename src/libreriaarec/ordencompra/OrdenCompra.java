package libreriaarec.ordencompra;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import libreriaarec.reabastecimiento.Solicitud;

/**
 * Orden de compra. Se genera automáticamente a partir de cada solicitud de abastecimiento
 * que ya cerró acuerdo con un proveedor (estado "Acordada" en el módulo de Reabastecimiento).
 *
 * - Proveedor / distribuidor = la editorial o distribuidor con quien se cerró el acuerdo.
 * - Editorial(es) = las editoriales de los libros pedidos.
 */
public class OrdenCompra {

    /** Un libro pedido en la orden. */
    public static class Linea {
        public final String titulo;
        public final String autor;
        public final String editorial;
        public final int cantidad;
        public final double precioRef; // 0 = por cotizar

        public Linea(String titulo, String autor, String editorial, int cantidad, double precioRef) {
            this.titulo = titulo;
            this.autor = autor;
            this.editorial = editorial;
            this.cantidad = cantidad;
            this.precioRef = precioRef;
        }

        public double valorRef() { return cantidad * precioRef; }
    }

    public final int idSolicitud;
    public final String numero;          // ejemplo: OC-20261027-003
    public final LocalDate fecha;        // fecha del trámite
    public final String proveedor;
    public final LocalDate fechaEntrega; // fecha acordada con el proveedor
    public final double totalAcordado;
    public final List<Linea> lineas = new ArrayList<>();
    public final List<String> contactos = new ArrayList<>(); // editoriales contactadas y su respuesta

    private OrdenCompra(Solicitud s) {
        this.idSolicitud = s.id;
        this.numero = String.format("OC-%s-%03d", s.fechaTramite.format(DateTimeFormatter.BASIC_ISO_DATE), s.id);
        this.fecha = s.fechaTramite;
        this.proveedor = s.editorialAcordada;
        this.fechaEntrega = s.fechaEntrega;
        this.totalAcordado = s.totalAcordado;
        for (Solicitud.Item it : s.items) {
            lineas.add(new Linea(it.titulo, it.autor, it.editorial, it.cantidad, it.precioReferencial));
        }
        for (Solicitud.Contacto c : s.contactos) {
            contactos.add(c.editorial + " (" + c.estado + ")");
        }
    }

    /** Convierte en órdenes las solicitudes con acuerdo cerrado. Las más recientes van primero. */
    public static List<OrdenCompra> desde(List<Solicitud> solicitudes) {
        List<OrdenCompra> r = new ArrayList<>();
        for (Solicitud s : solicitudes) {
            if (s.estado == Solicitud.Estado.ACORDADA) r.add(new OrdenCompra(s));
        }
        r.sort((a, b) -> b.idSolicitud - a.idSolicitud);
        return r;
    }

    public int totalLibros() {
        int t = 0;
        for (Linea l : lineas) t += l.cantidad;
        return t;
    }

    public double totalReferencial() {
        double t = 0;
        for (Linea l : lineas) t += l.valorRef();
        return t;
    }

    /** Editoriales de los libros, sin repetir: "Planeta, Siruela". */
    public String editoriales() {
        Set<String> set = new LinkedHashSet<>();
        for (Linea l : lineas) set.add(l.editorial);
        return String.join(", ", set);
    }

    /** Texto para el cuadro de observaciones del PDF. */
    public String observaciones() {
        StringBuilder sb = new StringBuilder();
        if (!contactos.isEmpty()) {
            sb.append("Proveedores contactados: ").append(String.join("; ", contactos)).append(". ");
        }
        sb.append("Orden generada desde la solicitud de abastecimiento N° ").append(idSolicitud).append(".");
        return sb.toString();
    }
}
