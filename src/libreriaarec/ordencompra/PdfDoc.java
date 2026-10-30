package libreriaarec.compra;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.Deflater;

/**
 * Generador mínimo de PDF (A4) sin librerías externas.
 * Dibuja texto (Helvetica normal y negrita), líneas, rectángulos y una imagen.
 * Las coordenadas se miden en puntos desde la esquina SUPERIOR izquierda de la hoja.
 */
public class PdfDoc {

    public static final double ANCHO = 595.28;
    public static final double ALTO = 841.89;

    private static final Charset WIN = Charset.forName("windows-1252");

    // Anchos de Helvetica y Helvetica-Bold (milésimas de punto) para los caracteres ASCII 32..126
    private static final int[] H = {
        278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
        556, 556, 556, 556, 556, 556, 556, 556, 556, 556,
        278, 278, 584, 584, 584, 556, 1015,
        667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778, 667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611,
        278, 278, 278, 469, 556, 333,
        556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556, 556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500,
        334, 260, 334, 584};
    private static final int[] HB = {
        278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278,
        556, 556, 556, 556, 556, 556, 556, 556, 556, 556,
        333, 333, 584, 584, 584, 611, 975,
        722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722, 611, 833, 722, 778, 667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611,
        333, 278, 333, 584, 556, 333,
        556, 611, 556, 611, 556, 333, 611, 611, 278, 278, 556, 278, 889, 611, 611, 611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500,
        389, 280, 389, 584};

    private final List<StringBuilder> paginas = new ArrayList<>();
    private StringBuilder actual;
    private byte[] imgDatos;
    private int imgAncho;
    private int imgAlto;

    public PdfDoc() {
        nuevaPagina();
    }

    // ------------------------------------------------------------------
    // Páginas
    // ------------------------------------------------------------------

    public void nuevaPagina() {
        actual = new StringBuilder();
        paginas.add(actual);
    }

    public int numeroDePaginas() {
        return paginas.size();
    }

    /** Vuelve a una página ya creada (0 = primera), por ejemplo para numerar al final. */
    public void irAPagina(int indice) {
        actual = paginas.get(indice);
    }

    // ------------------------------------------------------------------
    // Dibujo
    // ------------------------------------------------------------------

    public void texto(double x, double y, String s, double tam, boolean negrita) {
        texto(x, y, s, tam, negrita, 0);
    }

    /** Escribe texto con la línea base en (x, y). gris: 0 = negro, 1 = blanco. */
    public void texto(double x, double y, String s, double tam, boolean negrita, double gris) {
        if (s == null || s.isEmpty()) return;
        actual.append(n(gris)).append(" g BT /").append(negrita ? "F2 " : "F1 ").append(n(tam))
              .append(" Tf ").append(n(x)).append(' ').append(n(ALTO - y)).append(" Td (")
              .append(escapar(s)).append(") Tj ET 0 g\n");
    }

    public void textoDerecha(double xDerecha, double y, String s, double tam, boolean negrita) {
        texto(xDerecha - ancho(s, tam, negrita), y, s, tam, negrita, 0);
    }

    public void textoCentro(double xCentro, double y, String s, double tam, boolean negrita) {
        texto(xCentro - ancho(s, tam, negrita) / 2, y, s, tam, negrita, 0);
    }

    public void textoCentro(double xCentro, double y, String s, double tam, boolean negrita, double gris) {
        texto(xCentro - ancho(s, tam, negrita) / 2, y, s, tam, negrita, gris);
    }

    public void linea(double x1, double y1, double x2, double y2, double grosor) {
        actual.append(n(grosor)).append(" w ").append(n(x1)).append(' ').append(n(ALTO - y1)).append(" m ")
              .append(n(x2)).append(' ').append(n(ALTO - y2)).append(" l S\n");
    }

    public void rect(double x, double y, double w, double h, double grosor) {
        actual.append(n(grosor)).append(" w ").append(n(x)).append(' ').append(n(ALTO - y - h)).append(' ')
              .append(n(w)).append(' ').append(n(h)).append(" re S\n");
    }

    public void rectRelleno(double x, double y, double w, double h, double gris) {
        actual.append(n(gris)).append(" g ").append(n(x)).append(' ').append(n(ALTO - y - h)).append(' ')
              .append(n(w)).append(' ').append(n(h)).append(" re f 0 g\n");
    }

    // ------------------------------------------------------------------
    // Imagen (logo)
    // ------------------------------------------------------------------

    /** Define la imagen del logo (se reduce y se pinta sobre fondo blanco). */
    public void definirImagen(BufferedImage origen) {
        if (origen == null) return;
        int max = 200;
        double esc = Math.min(1.0, (double) max / Math.max(origen.getWidth(), origen.getHeight()));
        int w = Math.max(1, (int) Math.round(origen.getWidth() * esc));
        int h = Math.max(1, (int) Math.round(origen.getHeight() * esc));
        BufferedImage rgb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.drawImage(origen, 0, 0, w, h, null);
        g.dispose();

        byte[] crudo = new byte[w * h * 3];
        int k = 0;
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                int p = rgb.getRGB(xx, yy);
                crudo[k++] = (byte) (p >> 16);
                crudo[k++] = (byte) (p >> 8);
                crudo[k++] = (byte) p;
            }
        }
        imgDatos = comprimir(crudo);
        imgAncho = w;
        imgAlto = h;
    }

    public boolean tieneImagen() {
        return imgDatos != null;
    }

    public void dibujarImagen(double x, double y, double w, double h) {
        if (imgDatos == null) return;
        actual.append("q ").append(n(w)).append(" 0 0 ").append(n(h)).append(' ').append(n(x)).append(' ')
              .append(n(ALTO - y - h)).append(" cm /Im1 Do Q\n");
    }

    // ------------------------------------------------------------------
    // Medidas de texto
    // ------------------------------------------------------------------

    public double ancho(String s, double tam, boolean negrita) {
        int[] tabla = negrita ? HB : H;
        double total = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 32 || c > 126) {
                // letras con tilde o eñe: mismo ancho que la letra base
                String d = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
                c = d.charAt(0);
            }
            total += (c >= 32 && c <= 126) ? tabla[c - 32] : 556;
        }
        return total * tam / 1000.0;
    }

    /** Recorta el texto con "..." para que quepa en el ancho máximo. */
    public String ajustar(String s, double max, double tam, boolean negrita) {
        if (s == null) return "";
        if (ancho(s, tam, negrita) <= max) return s;
        String t = s;
        while (!t.isEmpty() && ancho(t + "...", tam, negrita) > max) {
            t = t.substring(0, t.length() - 1);
        }
        return t.trim() + "...";
    }

    /** Parte el texto en renglones que quepan en el ancho máximo. */
    public List<String> dividir(String s, double max, double tam, boolean negrita) {
        List<String> r = new ArrayList<>();
        StringBuilder linea = new StringBuilder();
        for (String palabra : s.split("\\s+")) {
            String prueba = linea.length() == 0 ? palabra : linea + " " + palabra;
            if (ancho(prueba, tam, negrita) <= max || linea.length() == 0) {
                linea = new StringBuilder(prueba);
            } else {
                r.add(linea.toString());
                linea = new StringBuilder(palabra);
            }
        }
        if (linea.length() > 0) r.add(linea.toString());
        return r;
    }

    // ------------------------------------------------------------------
    // Guardado
    // ------------------------------------------------------------------

    public void guardar(File destino) throws IOException {
        boolean conImagen = imgDatos != null;
        int primera = conImagen ? 6 : 5;
        int np = paginas.size();
        int total = primera + 2 * np;
        long[] pos = new long[total];
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.write("%PDF-1.4\n".getBytes(WIN));
        out.write(new byte[]{'%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});

        objeto(out, pos, 1, "<< /Type /Catalog /Pages 2 0 R >>".getBytes(WIN));
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < np; i++) kids.append(primera + 2 * i).append(" 0 R ");
        objeto(out, pos, 2, ("<< /Type /Pages /Kids [ " + kids + "] /Count " + np + " >>").getBytes(WIN));
        objeto(out, pos, 3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>".getBytes(WIN));
        objeto(out, pos, 4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>".getBytes(WIN));
        if (conImagen) {
            objeto(out, pos, 5, stream("<< /Type /XObject /Subtype /Image /Width " + imgAncho + " /Height " + imgAlto
                    + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length " + imgDatos.length + " >>",
                    imgDatos));
        }
        String recursos = "/Resources << /Font << /F1 3 0 R /F2 4 0 R >>"
                + (conImagen ? " /XObject << /Im1 5 0 R >>" : "") + " >>";
        for (int i = 0; i < np; i++) {
            int pag = primera + 2 * i;
            int cont = pag + 1;
            objeto(out, pos, pag, ("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + n(ANCHO) + " " + n(ALTO) + "] "
                    + recursos + " /Contents " + cont + " 0 R >>").getBytes(WIN));
            byte[] contenido = paginas.get(i).toString().getBytes(WIN);
            objeto(out, pos, cont, stream("<< /Length " + contenido.length + " >>", contenido));
        }

        long inicioXref = out.size();
        StringBuilder xref = new StringBuilder("xref\n0 " + total + "\n0000000000 65535 f \n");
        for (int i = 1; i < total; i++) xref.append(String.format(Locale.US, "%010d 00000 n \n", pos[i]));
        xref.append("trailer\n<< /Size ").append(total).append(" /Root 1 0 R >>\nstartxref\n")
            .append(inicioXref).append("\n%%EOF\n");
        out.write(xref.toString().getBytes(WIN));

        Files.write(destino.toPath(), out.toByteArray());
    }

    private static void objeto(ByteArrayOutputStream out, long[] pos, int num, byte[] cuerpo) throws IOException {
        pos[num] = out.size();
        out.write((num + " 0 obj\n").getBytes(WIN));
        out.write(cuerpo);
        out.write("\nendobj\n".getBytes(WIN));
    }

    private static byte[] stream(String diccionario, byte[] datos) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        b.write((diccionario + "\nstream\n").getBytes(WIN));
        b.write(datos);
        b.write("\nendstream".getBytes(WIN));
        return b.toByteArray();
    }

    private static byte[] comprimir(byte[] datos) {
        Deflater d = new Deflater();
        d.setInput(datos);
        d.finish();
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        while (!d.finished()) {
            int k = d.deflate(buf);
            b.write(buf, 0, k);
        }
        d.end();
        return b.toByteArray();
    }

    private static String n(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    private static String escapar(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '(' || c == ')') b.append('\\');
            b.append(c < 32 ? ' ' : c);
        }
        return b.toString();
    }
}
