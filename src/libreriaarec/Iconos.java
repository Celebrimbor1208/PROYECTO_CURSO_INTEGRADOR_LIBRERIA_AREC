package libreriaarec;

import java.awt.Image;
import javax.swing.ImageIcon;

/** Carga y escala imágenes desde el paquete libreriaarec.imagenes */
public class Iconos {

    public static ImageIcon cargar(String nombre, int ancho, int alto) {
        java.net.URL url = Iconos.class.getResource("/libreriaarec/imagenes/" + nombre);
        if (url == null) {
            System.err.println("No se encontró la imagen: " + nombre);
            return null;
        }
        Image img = new ImageIcon(url).getImage()
                .getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }
}
