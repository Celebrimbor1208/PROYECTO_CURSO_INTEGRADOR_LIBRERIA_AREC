package libreriaarec.compra;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Dibuja la orden de compra en PDF (hoja A4), con la estructura de un formato de orden de compra
 * clásico: datos de la empresa, datos del proveedor, tabla de productos, totales,
 * observaciones y firmas. Si la orden tiene muchos libros, continúa en más hojas.
 */
public class OrdenPdf {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final double M = 36;                       // margen
    private static final double W = PdfDoc.ANCHO - 2 * M;     // ancho útil
    private static final double LIMITE = PdfDoc.ALTO - 50;    // hasta aquí se escribe en cada hoja
    private static final double ALTO_FILA = 24;
    private static final double RESERVA_FINAL = 185;          // espacio para totales + firmas

    private OrdenPdf() { }

    public static void generar(OrdenCompra o, File destino) throws IOException {
        PdfDoc d = new PdfDoc();
        d.definirImagen(cargarLogo());

        double y = M;
        y = cabecera(d, y);
        y = datosEmpresa(d, o, y);
        y = datosProveedor(d, o, y);
        y += 10;
        d.texto(M, y + 8, "Favor despachar a " + Empresa.NOMBRE + " el siguiente material:", 9, true);
        y += 16;
        y = tabla(d, o, y);

        if (y + RESERVA_FINAL > LIMITE) {
            d.nuevaPagina();
            y = M;
        }
        y = totales(d, o, y + 10);
        firmas(d, y + 34);

        int np = d.numeroDePaginas();
        for (int i = 0; i < np; i++) {
            d.irAPagina(i);
            d.textoCentro(PdfDoc.ANCHO / 2, PdfDoc.ALTO - 28,
                    Empresa.NOMBRE + " - Orden de compra " + o.numero + " - Página " + (i + 1) + " de " + np,
                    7, false, 0.4);
        }
        d.guardar(destino);
    }

    private static BufferedImage cargarLogo() {
        try (InputStream in = OrdenPdf.class.getResourceAsStream("/libreriaarec/imagenes/logo.png")) {
            return in == null ? null : ImageIO.read(in);
        } catch (Exception e) {
            return null; // sin logo, el PDF se genera igual
        }
    }

    // ------------------------------------------------------------------
    // Bloques
    // ------------------------------------------------------------------

    private static double cabecera(PdfDoc d, double y) {
        d.rect(M, y, W, 56, 0.8);
        if (d.tieneImagen()) {
            d.dibujarImagen(M + 6, y + 4, 48, 48);
        } else {
            d.textoCentro(M + 36, y + 32, "LOGO", 9, true, 0.6);
        }
        d.textoCentro(PdfDoc.ANCHO / 2, y + 30, "ORDEN DE COMPRA", 22, true);
        d.textoCentro(PdfDoc.ANCHO / 2, y + 46, Empresa.NOMBRE, 9, false, 0.35);
        return y + 56;
    }

    private static double banda(PdfDoc d, double y, String titulo) {
        d.rectRelleno(M, y, W, 16, 0.85);
        d.rect(M, y, W, 16, 0.7);
        d.texto(M + 6, y + 11.5, titulo, 8.5, true);
        return y + 16;
    }

    /** Dibuja una caja de filas con dos columnas de campos "ETIQUETA: valor". */
    private static double filas(PdfDoc d, double y, String[][] izq, String[][] der) {
        int n = izq.length;
        double h = n * 18;
        double mitad = M + W / 2;
        d.rect(M, y, W, h, 0.7);
        d.linea(mitad, y, mitad, y + h, 0.7);
        for (int i = 0; i < n; i++) {
            double fy = y + i * 18;
            if (i > 0) d.linea(M, fy, M + W, fy, 0.4);
            campo(d, M, fy, W / 2, izq[i][0], izq[i][1], izq[i].length > 2);
            campo(d, mitad, fy, W / 2, der[i][0], der[i][1], der[i].length > 2);
        }
        return y + h;
    }

    private static void campo(PdfDoc d, double x, double y, double ancho, String etiqueta, String valor,
                              boolean negritaValor) {
        d.texto(x + 6, y + 12, etiqueta, 7.5, true);
        double vx = x + 6 + d.ancho(etiqueta, 7.5, true) + 5;
        double fin = x + ancho - 8;
        if (valor == null || valor.trim().isEmpty()) {
            d.linea(vx, y + 14, fin, y + 14, 0.4); // línea en blanco para escribir a mano
        } else {
            d.texto(vx, y + 12, d.ajustar(valor, fin - vx, 9, negritaValor), 9, negritaValor);
        }
    }

    private static double datosEmpresa(PdfDoc d, OrdenCompra o, double y) {
        y += 8;
        y = banda(d, y, "DATOS DE LA EMPRESA");
        return filas(d, y,
                new String[][]{
                    {"NOMBRE DE LA EMPRESA:", Empresa.NOMBRE},
                    {"RUC N°:", Empresa.RUC},
                    {"DIRECCIÓN FISCAL:", Empresa.DIRECCION},
                    {"TELÉFONO:", Empresa.TELEFONO}},
                new String[][]{
                    {"ORDEN DE COMPRA N°:", o.numero, "negrita"},
                    {"FECHA:", o.fecha.format(FMT)},
                    {"N° DE SOLICITUD:", String.valueOf(o.idSolicitud)},
                    {"CORREO:", Empresa.CORREO}});
    }

    private static double datosProveedor(PdfDoc d, OrdenCompra o, double y) {
        y += 8;
        y = banda(d, y, "DATOS DEL PROVEEDOR / DISTRIBUIDOR");
        return filas(d, y,
                new String[][]{
                    {"PROVEEDOR:", o.proveedor, "negrita"},
                    {"RUC N°:", ""},
                    {"DIRECCIÓN:", ""},
                    {"CORREO:", ""}},
                new String[][]{
                    {"EDITORIAL(ES):", o.editoriales()},
                    {"CONTACTO VENTAS:", ""},
                    {"TELÉFONO:", ""},
                    {"ENTREGA ACORDADA:", o.fechaEntrega.format(FMT), "negrita"}});
    }

    // ------------------------------------------------------------------
    // Tabla de productos (continúa en otra hoja si hace falta)
    // ------------------------------------------------------------------

    private static double tabla(PdfDoc d, OrdenCompra o, double y) {
        double[] cx = {M, M + 28, M + 218, M + 318, M + 358, M + 398, M + 460, M + W};
        double inicio = y;
        y = cabeceraTabla(d, cx, y);

        int item = 1;
        for (OrdenCompra.Linea l : o.lineas) {
            if (y + ALTO_FILA > LIMITE) {
                cerrarTabla(d, cx, inicio, y);
                d.nuevaPagina();
                inicio = M;
                y = cabeceraTabla(d, cx, M);
            }
            d.textoCentro((cx[0] + cx[1]) / 2, y + 14, String.valueOf(item++), 8.5, false);

            d.texto(cx[1] + 4, y + 10, d.ajustar(l.titulo, cx[2] - cx[1] - 8, 8.5, false), 8.5, false);
            if (l.autor != null && !l.autor.isEmpty()) {
                d.texto(cx[1] + 4, y + 20, d.ajustar(l.autor, cx[2] - cx[1] - 8, 7, false), 7, false, 0.35);
            }
            d.texto(cx[2] + 4, y + 14, d.ajustar(l.editorial, cx[3] - cx[2] - 8, 8, false), 8, false);
            d.textoCentro((cx[3] + cx[4]) / 2, y + 14, String.valueOf(l.cantidad), 8.5, false);
            d.textoCentro((cx[4] + cx[5]) / 2, y + 14, "UND.", 8, false);
            if (l.precioRef > 0) {
                d.textoDerecha(cx[6] - 4, y + 14, monto(l.precioRef), 8.5, false);
                d.textoDerecha(cx[7] - 4, y + 14, monto(l.valorRef()), 8.5, false);
            } else {
                d.textoCentro((cx[5] + cx[6]) / 2, y + 14, "Por cotizar", 7, false, 0.35);
                d.textoDerecha(cx[7] - 4, y + 14, "-", 8.5, false);
            }
            y += ALTO_FILA;
            d.linea(cx[0], y, cx[7], y, 0.4);
        }
        cerrarTabla(d, cx, inicio, y);
        return y;
    }

    private static double cabeceraTabla(PdfDoc d, double[] cx, double y) {
        d.rectRelleno(cx[0], y, W, 18, 0.85);
        String[] t = {"ITEM", "PRODUCTO", "EDITORIAL", "CANT.", "UNIDAD", "P. UNIT. REF.", "VALOR REF."};
        for (int i = 0; i < t.length; i++) {
            d.textoCentro((cx[i] + cx[i + 1]) / 2, y + 12, t[i], 7.5, true);
        }
        d.linea(cx[0], y + 18, cx[7], y + 18, 0.7);
        return y + 18;
    }

    /** Marco exterior y líneas verticales de la tabla en la hoja actual. */
    private static void cerrarTabla(PdfDoc d, double[] cx, double inicio, double fin) {
        d.rect(cx[0], inicio, W, fin - inicio, 0.7);
        for (int i = 1; i < cx.length - 1; i++) {
            d.linea(cx[i], inicio, cx[i], fin, 0.4);
        }
    }

    // ------------------------------------------------------------------
    // Observaciones, totales y firmas
    // ------------------------------------------------------------------

    private static double totales(PdfDoc d, OrdenCompra o, double y) {
        double anchoObs = 300;
        double xt = M + anchoObs + 10;
        double wt = W - anchoObs - 10;

        // Observaciones (izquierda)
        d.rect(M, y, anchoObs, 76, 0.7);
        d.texto(M + 6, y + 11, "OBSERVACIONES Y/O COMENTARIOS:", 7.5, true);
        List<String> lineas = d.dividir(o.observaciones(), anchoObs - 12, 7.5, false);
        for (int i = 0; i < lineas.size() && i < 5; i++) {
            d.texto(M + 6, y + 24 + i * 10, lineas.get(i), 7.5, false);
        }

        // Totales (derecha)
        d.rect(xt, y, wt, 60, 0.7);
        d.linea(xt, y + 20, xt + wt, y + 20, 0.4);
        d.rectRelleno(xt, y + 40, wt, 20, 0.85);
        d.rect(xt, y + 40, wt, 20, 0.7);
        d.texto(xt + 6, y + 13, "N° DE LIBROS", 7.5, true);
        d.textoDerecha(xt + wt - 6, y + 14, String.valueOf(o.totalLibros()), 9, false);
        d.texto(xt + 6, y + 33, "TOTAL REFERENCIAL (S/)", 7.5, true);
        d.textoDerecha(xt + wt - 6, y + 34, o.totalReferencial() > 0 ? monto(o.totalReferencial()) : "-", 9, false);
        d.texto(xt + 6, y + 53, "TOTAL ACORDADO (S/)", 8, true);
        d.textoDerecha(xt + wt - 6, y + 54, monto(o.totalAcordado), 10, true);

        List<String> nota = d.dividir("Montos en soles (S/). Los precios por unidad son referenciales; "
                + "el monto que se paga al proveedor es el TOTAL ACORDADO.", wt, 6.5, false);
        for (int i = 0; i < nota.size(); i++) {
            d.texto(xt, y + 70 + i * 8, nota.get(i), 6.5, false, 0.4);
        }
        return y + 76;
    }

    private static void firmas(PdfDoc d, double y) {
        String[] titulos = {"Requerido por:", "Revisado por:", "Aprobado por:"};
        double separacion = 20;
        double ancho = (W - 2 * separacion) / 3;
        for (int i = 0; i < 3; i++) {
            double x = M + i * (ancho + separacion);
            d.linea(x, y, x + ancho, y, 0.6);
            d.texto(x, y + 11, titulos[i], 8, true);
            d.texto(x, y + 25, "Fecha:", 8, true);
            d.linea(x + 30, y + 26, x + ancho, y + 26, 0.4);
        }
    }

    private static String monto(double v) {
        return String.format(Locale.US, "%.2f", v);
    }
}
