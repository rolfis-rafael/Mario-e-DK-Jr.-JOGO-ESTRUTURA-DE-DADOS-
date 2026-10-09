package template;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

public class Cenario {

    public static final Color CEU = new Color( 3, 95, 170 );

    // Coordenadas na escala da arte: a linha 192 é à linha 768 do PNG.
    private static final int LINHA_GRAMA = 192;
    private static final int FIM_TERRA = 222;

    private static class Trecho {
        int x;
        int y;
        int largura;
        int altura = 1;
        Color cor;
    }

    private final List<Trecho> trechos = new ArrayList<>(); // Fundo preparado uma só vez.

    /** Prepara a imagem uma vez e alinha a grama do chão. */
    public Cenario() throws IOException {
        BufferedImage original = carregar( "fundo.png" );
        BufferedImage arte = prepararArte( original );
        BufferedImage fundo = new BufferedImage( TorreDoMario.LARGURA, TorreDoMario.ALTURA,
                BufferedImage.TYPE_INT_RGB );
        Graphics2D g = fundo.createGraphics();
        g.setColor( CEU );
        g.fillRect( 0, 0, fundo.getWidth(), fundo.getHeight() );
        g.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR );
        g.drawImage( arte, 0, 0, TorreDoMario.LARGURA, TorreDoMario.CHAO,
                0, 0, arte.getWidth(), LINHA_GRAMA, null );
        // A faixa de grama começa exatamente onde personagens e objetos se apoiam.
        g.drawImage( arte, 0, TorreDoMario.CHAO, TorreDoMario.LARGURA, TorreDoMario.ALTURA,
                0, LINHA_GRAMA, arte.getWidth(), FIM_TERRA, null );
        g.dispose();
        prepararTrechos( fundo );
    }

    /** Lê o PNG  */
    private BufferedImage carregar( String nome ) throws IOException {
        BufferedImage imagem;
        try ( InputStream entrada = Cenario.class.getResourceAsStream( "/sprites/" + nome ) ) {
            if ( entrada != null ) {
                imagem = ImageIO.read( entrada );
            } else {
                File arquivo = new File( "src/sprites/" + nome );
                if ( !arquivo.isFile() ) {
                    arquivo = new File( "sprites/" + nome );
                }
                imagem = ImageIO.read( arquivo );
            }
        }
        if ( imagem == null ) {
            throw new IOException( "Não foi possível ler " + nome );
        }
        return imagem;
    }

    private BufferedImage prepararArte( BufferedImage original ) {
        BufferedImage arte = new BufferedImage( 256, 223, BufferedImage.TYPE_INT_RGB );
        Graphics2D g = arte.createGraphics();
        g.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR );
        g.drawImage( original, 0, 0, arte.getWidth(), arte.getHeight(), null );
        g.dispose();
        return arte;
    }

    /** Junta sequências da mesma cor e prolonga as que continuam na proxima linha. */
    private void prepararTrechos( BufferedImage imagem ) {
        Map<Long, Trecho> anterior = new HashMap<>();
        Map<Integer, Color> cores = new HashMap<>();
        for ( int y = 0; y < imagem.getHeight(); y++ ) {
            Map<Long, Trecho> atual = new HashMap<>();
            int x = 0;
            while ( x < imagem.getWidth() ) {
                int rgb = imagem.getRGB( x, y ) & 0xFFFFFF;
                int fim = x + 1;
                while ( fim < imagem.getWidth() && ( imagem.getRGB( fim, y ) & 0xFFFFFF ) == rgb ) {
                    fim++;
                }
                int largura = fim - x;
                long chave = ( (long) rgb << 20 ) | ( (long) x << 10 ) | largura;
                Trecho trecho = anterior.get( chave );
                if ( trecho == null ) {
                    trecho = new Trecho();
                    trecho.x = x;
                    trecho.y = y;
                    trecho.largura = largura;
                    trecho.cor = cores.computeIfAbsent( rgb, Color::new );
                    trechos.add( trecho );
                } else {
                    trecho.altura++;
                }
                atual.put( chave, trecho );
                x = fim;
            }
            anterior = atual;
        }
    }

    /** Desenha o fundo já preparado, sem carregar arquivos denovo a cada quadro. */
    public void desenhar( EngineFrame jogo ) {
        for ( Trecho trecho : trechos ) {
            jogo.fillRectangle( trecho.x, trecho.y, trecho.largura, trecho.altura, trecho.cor );
        }
    }
}
