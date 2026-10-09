package template;
import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
// aq só carrega e mostra as imagens do DK Jr
public class SpritesDkJr {
    private final BufferedImage[] imagens = new BufferedImage[2];
    private final double[] ancoraCipoX = { 40, 23 };
    public SpritesDkJr() throws IOException {
        for ( int i = 0; i < 2; i++ ) {
            imagens[i] = ImageIO.read( getClass().getResource( "/sprites/dkjr" + ( i + 1 ) + ".png" ) );
        }
    }
    public void desenhar( EngineFrame jogo, int quadro, double cipoX, double topoY ) {
        BufferedImage imagem = imagens[quadro % 2];
        double esquerda = cipoX - ancoraCipoX[quadro % 2];
        for ( int y = 0; y < imagem.getHeight(); y++ ) {
            for ( int x = 0; x < imagem.getWidth(); x++ ) {
                int argb = imagem.getRGB( x, y );
                if ( ( argb >>> 24 ) > 128 ) {
                    jogo.fillRectangle( esquerda + x, topoY + y, 1, 1, new Color( argb, true ) );
                }
            }
        }
    }
}
