package template;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

// PNG BANANA
public class SpriteBanana {

    private BufferedImage imagem;

    public SpriteBanana() {
        try {
            imagem = ImageIO.read( getClass().getResource( "/sprites/banana.png" ) );
        } catch ( IOException | IllegalArgumentException e ) {
            imagem = null;
        }
    }

    public void desenhar( EngineFrame jogo, double x, double y, double giro ) {
        if ( imagem == null ) {
            return;
        }
        double escala = 1.3; // aq diminui um pouco, pq 2x tava grandona
        double cx = imagem.getWidth() / 2.0;
        double cy = imagem.getHeight() / 2.0;
        double cos = Math.cos( giro );
        double sin = Math.sin( giro );
        for ( int py = 0; py < imagem.getHeight(); py++ ) {
            for ( int px = 0; px < imagem.getWidth(); px++ ) {
                int argb = imagem.getRGB( px, py );
                if ( ( argb >>> 24 ) > 128 ) {
                    double rx = px - cx;
                    double ry = py - cy;
                    double dx = x + ( rx * cos - ry * sin ) * escala;
                    double dy = y + ( rx * sin + ry * cos ) * escala;
                    jogo.fillRectangle( dx, dy, escala, escala, new Color( argb, true ) );
                }
            }
        }
    }

}
