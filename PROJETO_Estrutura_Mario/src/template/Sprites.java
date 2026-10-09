package template;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Carrega sprites */
public class Sprites {

    private final BufferedImage[][] imagens = new BufferedImage[2][2];

    public Sprites() throws IOException {
        String[] nomes = { "mario", "luigi" };
        for ( int j = 0; j < 2; j++ ) {
            imagens[j][0] = ImageIO.read( getClass().getResource( "/sprites/" + nomes[j] + "_normal.png" ) );
            imagens[j][1] = ImageIO.read( getClass().getResource( "/sprites/" + nomes[j] + "_chute.png" ) );
        }
    }

    public void desenhar( EngineFrame jogo, int jogador, int quadro,
            double x, double pesY, int direcao ) {
        BufferedImage imagem = imagens[jogador][quadro == 1 ? 1 : 0];
        int escala = 2;
        double esquerda = x - imagem.getWidth() * escala / 2.0;
        double topo = pesY - imagem.getHeight() * escala;
        for ( int y = 0; y < imagem.getHeight(); y++ ) {
            for ( int px = 0; px < imagem.getWidth(); px++ ) {
                int argb = imagem.getRGB( px, y );
                if ( ( argb >>> 24 ) > 128 ) {
                    int xx = direcao > 0 ? imagem.getWidth() - 1 - px : px;
                    jogo.fillRectangle( esquerda + xx * escala, topo + y * escala,
                            escala, escala, new Color( argb, true ) );
                }
            }
        }
    }

}
