package template;
import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public class DkJr {
    // velocidade da banana, gravidade e intervalo aleatório que joga a banana
    static final double TEMPO_VOO = 1.25;
    static final double GRAVIDADE_BANANA = 430;
    static final double[] INTERVALOS_MINIMOS = { 2.4, 2.0, 1.7 };
    static final double[] INTERVALOS_MAXIMOS = { 3.6, 3.0, 2.5 };
    static final double PRIMEIRO_LANCAMENTO = 1.2;
    static class Banana {
        double x;
        double y;
        final double vx;
        double vy;
        double idade;
        Banana( double x, double y, double destinoX, double destinoY ) {
            this.x = x;
            this.y = y;
            vx = ( destinoX - x ) / TEMPO_VOO;
            vy = ( destinoY - y ) / TEMPO_VOO - GRAVIDADE_BANANA * TEMPO_VOO / 2;
        }
    }
    final List<Banana> bananas = new ArrayList<>();
    final Random aleatorio = new Random();
    double tempo;
    double ateLancamento;
    private boolean primeiroTipoBase;
    private int tiposUsados;
    public DkJr() {
        reiniciar();
    }
    final void reiniciar() {
        bananas.clear();
        tempo = 0;
        ateLancamento = PRIMEIRO_LANCAMENTO;
        tiposUsados = 2;
    }
    double x() {
        return TorreDoMario.LARGURA / 2.0;
    }
    // altura fixa do DK
    double y() {
        return 125;
    }
    private boolean sortearTipo() {
        if ( tiposUsados == 2 ) {
            primeiroTipoBase = aleatorio.nextBoolean();
            tiposUsados = 0;
        }
        boolean base = tiposUsados == 0 ? primeiroTipoBase : !primeiroTipoBase;
        tiposUsados++;
        return base;
    }
    static int faseAtaques( double tempoRestante ) {
        if ( tempoRestante <= 15 ) {
            return 2;
        }
        if ( tempoRestante <= 30 ) {
            return 1;
        }
        return 0;
    }
    void lancarPar( TorreDoMario.Partida partida ) {
        boolean naBase = sortearTipo();
        TorreDoMario.Jogador j = partida.jogadores[aleatorio.nextInt(2)];
        {
            double[] topo = partida.topoVisivel( j, naBase );
            double destinoX = naBase ? j.baseX : j.x;
            double destinoY = naBase ? TorreDoMario.CHAO - 26 : j.y - 29;
            if ( topo != null ) {
                destinoX = topo[0];
                destinoY = topo[1] + topo[3] / 2;
            }
            bananas.add( new Banana( x(), y() + 25, destinoX, destinoY ) );
        }
    }
    void atualizar( TorreDoMario.Partida partida, double dt ) {
        if ( !partida.emAndamento() || dt <= 0 ) {
            return;
        }
        tempo += dt;
        int fase = faseAtaques( partida.tempoRestante );
        ateLancamento = Math.min( ateLancamento, INTERVALOS_MAXIMOS[fase] );
        ateLancamento -= dt;
        if ( ateLancamento <= 0 ) {
            lancarPar( partida );
            ateLancamento = INTERVALOS_MINIMOS[fase]
                    + aleatorio.nextDouble() * ( INTERVALOS_MAXIMOS[fase] - INTERVALOS_MINIMOS[fase] );
        }
        for ( int i = bananas.size() - 1; i >= 0; i-- ) {
            Banana b = bananas.get( i );
            double antesX = b.x, antesY = b.y;
            b.x += b.vx * dt;
            b.y += b.vy * dt + GRAVIDADE_BANANA * dt * dt / 2;
            b.vy += GRAVIDADE_BANANA * dt;
            b.idade += dt;
            boolean acertou = partida.colidirBanana( antesX, antesY, b.x, b.y, b.vx );
            boolean saiu = b.x < -24 || b.x > TorreDoMario.LARGURA + 24
                    || b.y > TorreDoMario.CHAO + 24 || b.idade > 3;
            if ( acertou || saiu ) {
                bananas.remove( i );
            }
        }
    }
    // desenha o DK, cipó e a banana
    public void desenhar( EngineFrame jogo, SpritesDkJr sprites, SpriteBanana spriteBanana ) {
        double cipoX = x();
        double topoSprite = y();
        jogo.fillRectangle( cipoX - 3, 0, 6, topoSprite, new Color( 112, 69, 35 ) );
        if ( sprites != null ) {
            sprites.desenhar( jogo, (int) ( tempo / 0.7 ) % 2, cipoX, topoSprite );
        }
        if ( ateLancamento < 0.35 ) {
            desenharBanana( jogo, spriteBanana, cipoX + 22, topoSprite + 65, 0 );
        }
        for ( Banana b : bananas ) {
            desenharBanana( jogo, spriteBanana, b.x, b.y, b.idade * 8 );
        }
    }
    private void desenharBanana( EngineFrame jogo, SpriteBanana sprite, double x, double y, double giro ) {
        if ( sprite != null ) {
            sprite.desenhar( jogo, x, y, giro );
            return;
        }
        for ( int i = 0; i <= 8; i++ ) {
            double angulo = Math.PI * ( 0.15 + i * 0.7 / 8 ) + giro;
            double px = x + Math.cos( angulo ) * 9;
            double py = y + Math.sin( angulo ) * 9;
            jogo.fillRectangle( px - 2, py - 2, 4, 4, new Color( 255, 220, 56 ) );
            if ( i == 0 || i == 8 ) {
                jogo.fillRectangle( px - 1, py - 1, 2, 3, new Color( 95, 61, 25 ) );
            }
        }
    }
}
