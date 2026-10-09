package template;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
// registra oq aconteceu no jogo, tipo pegar, roubar e chutar
public class Acao {
    public enum Tipo { PEGAR, DEPOSITAR, CHUTAR, ROUBAR, BANANA_MAO, BANANA_BASE }
    public final Tipo tipo;
    public final int jogador;
    public final int objeto;
    public final List<Integer> objetosDerrubados;
    public Acao( Tipo tipo, int jogador, int objeto ) {
        this.tipo = tipo;
        this.jogador = jogador;
        this.objeto = objeto;
        objetosDerrubados = tipo == Tipo.CHUTAR && objeto != 0
                ? Collections.singletonList( objeto ) : Collections.emptyList();
    }
    public Acao( Tipo tipo, int jogador, List<Integer> derrubados ) {
        this.tipo = tipo;
        this.jogador = jogador;
        objetosDerrubados = Collections.unmodifiableList( new ArrayList<>( derrubados ) );
        objeto = objetosDerrubados.isEmpty() ? 0 : objetosDerrubados.get( 0 );
    }
    @Override
    public String toString() {
        if ( tipo == Tipo.BANANA_MAO || tipo == Tipo.BANANA_BASE ) {
            return "DK Jr. derrubou #" + objeto + ( tipo == Tipo.BANANA_BASE ? " da base de " : " da mão de " )
                    + ( jogador == 0 ? "Mario" : "Luigi" );
        }
        if ( tipo == Tipo.ROUBAR ) {
            return ( jogador == 0 ? "Mario" : "Luigi" ) + " roubou #" + objeto + " da base rival";
        }
        if ( tipo == Tipo.CHUTAR ) {
            return ( jogador == 0 ? "Mario" : "Luigi" ) + " chutou"
                    + ( objeto == 0 ? " o adversário" : " e derrubou, do topo para a base: " + objetosDerrubados );
        }
        return ( jogador == 0 ? "Mario" : "Luigi" )
                + ( tipo == Tipo.PEGAR ? " pegou #" : " guardou #" ) + objeto;
    }
}
