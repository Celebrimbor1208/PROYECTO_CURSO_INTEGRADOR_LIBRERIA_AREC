package libreriaarec;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 * Panel base que dibuja una imagen de fondo escalada.
 * Si la imagen no existe, usa un color oscuro para que el programa no falle.
 *
 * Coloca tu imagen en: src/libreriaarec/imagenes/fondo.jpg
 */
public class PanelConFondo extends JPanel {

    private Image fondo;

    public PanelConFondo(String rutaRecurso) {
        try {
            java.net.URL url = getClass().getResource(rutaRecurso);
            if (url != null) {
                fondo = new ImageIcon(url).getImage();
            }
        } catch (Exception e) {
            fondo = null;
        }
        setBackground(new Color(40, 40, 40));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (fondo != null) {
            g.drawImage(fondo, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
