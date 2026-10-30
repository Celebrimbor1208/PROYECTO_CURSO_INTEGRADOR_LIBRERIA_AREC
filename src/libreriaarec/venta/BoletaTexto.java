package libreriaarec.venta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import libreriaarec.modelo.Venta;
import libreriaarec.util.Texto;

/** Arma el texto de la boleta que se muestra e imprime (responsabilidad única: el formato). */
public final class BoletaTexto {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SEPARADOR = "------------------------------------";

    private BoletaTexto() { }

    public static String construir(String numero, LocalDateTime fecha, String dni,
                                   List<Venta.Linea> lineas, boolean preliminar) {
        StringBuilder sb = new StringBuilder();
        sb.append("        LIBRERÍAS AREC\n");
        sb.append("        BOLETA DE VENTA\n");
        if (preliminar) sb.append("      *** VISTA PREVIA ***\n");
        sb.append(SEPARADOR).append("\n");
        sb.append("N°:    ").append(numero).append("\n");
        sb.append("Fecha: ").append(fecha.format(FECHA_HORA)).append("\n");
        sb.append("DNI:   ").append(dni).append("\n");
        sb.append(SEPARADOR).append("\n");

        double total = 0;
        int n = 1;
        for (Venta.Linea l : lineas) {
            sb.append(n++).append(". ").append(recortar(l.getTitulo(), 30)).append("\n");
            sb.append("   [").append(l.getCodigo()).append("] ")
              .append(l.getCantidad()).append(" x ").append(Texto.monto(l.getPrecio()))
              .append(" = ").append(Texto.monto(l.total())).append("\n");
            total += l.total();
        }
        sb.append(SEPARADOR).append("\n");
        sb.append("TOTAL A PAGAR: S/ ").append(Texto.monto(total)).append("\n\n");
        sb.append("     ¡Gracias por su compra!\n");
        return sb.toString();
    }

    private static String recortar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max - 3) + "...";
    }
}
