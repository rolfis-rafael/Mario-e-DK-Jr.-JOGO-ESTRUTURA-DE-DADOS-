package template;
import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public class TorreDoMario extends EngineFrame {
    // tamanhos da tela, chão e quantos blocos dá pra carregar
    static final int LARGURA = 1000;
    static final int ALTURA = 640;
    static final int CHAO = 550;
    static final int ALTURA_JOGADOR = 52;
    static final int TAMANHO_OBJETO = 30;
    static final int CAPACIDADE = 3;
    static final double DURACAO = 60;
    static final double ALCANCE_OBJETO = 48;
    static final double ALCANCE_BASE = 85;
    static final double ALCANCE_CHUTE = 76;
    static final double DURACAO_ANIMACAO_CHUTE = 0.28;
    static final double DURACAO_TRANSPORTE = 0.36;
    static final double INTERVALO_QUEDA = 0.16;
    static final Color VERMELHO = new Color( 225, 52, 57 );
    static final Color VERDE = new Color( 35, 157, 85 );
    static final Color ESCURO = new Color( 24, 40, 58 );
    private Partida partida;
    private double animacao;
    private SpritesDkJr spritesDkJr;
    private SpriteBanana spriteBanana;

    public TorreDoMario() {
        super( LARGURA, ALTURA, "Mario e Luigi - Pique-Bandeira", 60,
                true, false, false, false, false, false );
    }
    @Override
    public synchronized void create() {
        partida = new Partida();
        try {
            spritesDkJr = new SpritesDkJr();
        } catch ( IOException e ) {
            System.err.println( "Não foi possível carregar src/sprites/dkjr.jpg: " + e.getMessage() );
        }
        spriteBanana = new SpriteBanana();
    }
    @Override
    public synchronized void update( double delta ) {
        if ( isKeyPressed( KEY_R ) ) {
            partida.reiniciar();
            animacao = 0;
            return;
        }
        if ( isKeyPressed( KEY_SPACE ) && partida.iniciada && !partida.encerrada ) {
            partida.pausada = !partida.pausada;
        }
        if ( !partida.emAndamento() ) {
            return;
        }
        double dt = Math.max( 0, delta );
        animacao += dt;
        partida.atualizarTempo( dt );
        if ( partida.encerrada ) {
            return;
        }
        int dirMario = ( isKeyDown( KEY_D ) ? 1 : 0 ) - ( isKeyDown( KEY_A ) ? 1 : 0 );
        int dirLuigi = ( isKeyDown( KEY_RIGHT ) ? 1 : 0 ) - ( isKeyDown( KEY_LEFT ) ? 1 : 0 );
        boolean puloMario = isKeyPressed( KEY_W );
        boolean puloLuigi = isKeyPressed( KEY_UP );
        double restante = Math.min( dt, 0.12 );
        while ( restante > 0.000001 ) {
            double passo = Math.min( restante, 1.0 / 120 );
            partida.movimentar( 0, dirMario, puloMario, passo );
            partida.movimentar( 1, dirLuigi, puloLuigi, passo );
            partida.atualizarObjetos( passo );
            partida.dkJr.atualizar( partida, passo );
            puloMario = puloLuigi = false;
            restante -= passo;
        }
        partida.processarColetas( isKeyPressed( KEY_E ), isKeyPressed( KEY_L ) );
        partida.processarChutes( isKeyPressed( KEY_C ), isKeyPressed( KEY_M ) );
        if ( isKeyPressed( KEY_E ) ) {
            partida.depositar( 0 );
        }
        if ( isKeyPressed( KEY_L ) ) {
            partida.depositar( 1 );
        }
    }
    static class Objeto {
        final int id;
        final int tipo;
        double x;
        double y;
        double vx;
        double vy;
        double bloqueioColeta;
        boolean caindo;
        double atrasoQueda;
        Jogador suporteQueda;
        double deslocamentoQuedaX;
        double deslocamentoQuedaY;
        double larguraQueda;
        double alturaQueda;
        double tempoBrilho;
        double tempoTransporte;
        double origemX;
        double origemY;
        double origemLargura;
        double origemAltura;
        Objeto( int id, int tipo, double x ) {
            this( id, tipo, x, CHAO );
        }
        Objeto( int id, int tipo, double x, double piso ) {
            this.id = id;
            this.tipo = tipo;
            this.x = x;
            this.y = piso - TAMANHO_OBJETO;
        }
        double[] retanguloVisual( double cx, double topo, double largura, double altura ) {
            if ( tempoTransporte <= 0 ) {
                return new double[] { cx, topo, largura, altura };
            }
            double progresso = 1 - tempoTransporte / DURACAO_TRANSPORTE;
            double suave = progresso * progresso * ( 3 - 2 * progresso );
            return new double[] {
                origemX + ( cx - origemX ) * suave,
                origemY + ( topo - origemY ) * suave - Math.sin( progresso * Math.PI ) * 42,
                origemLargura + ( largura - origemLargura ) * suave,
                origemAltura + ( altura - origemAltura ) * suave
            };
        }
        // faz o bloco ir de um lugar pro outro
        void transportarDesde( double cx, double topo, double largura, double altura ) {
            double[] atual = retanguloVisual( cx, topo, largura, altura );
            origemX = atual[0];
            origemY = atual[1];
            origemLargura = atual[2];
            origemAltura = atual[3];
            tempoTransporte = DURACAO_TRANSPORTE;
            tempoBrilho = DURACAO_TRANSPORTE + 0.18;
        }
    }
    static class Plataforma {
        final double x;
        final double y;
        final double largura;
        Plataforma( double x, double y, double largura ) {
            this.x = x;
            this.y = y;
            this.largura = largura;
        }
    }
    static class Jogador {
        final int indice;
        final String nome;
        final double baseX;
        final Pilha<Objeto> mao = new Pilha<>();
        final Pilha<Objeto> base = new Pilha<>();
        double x;
        double y = CHAO - ALTURA_JOGADOR;
        double vy;
        double passos;
        double empurrao;
        double recargaChute;
        double tempoChute;
        double atordoado;
        double invulneravel;
        double recargaRoubo;
        double tempoDerrubada;
        double protecaoBananaMao;
        double protecaoBananaBase;
        int direcaoChute;
        boolean contatoChutePendente;
        int direcao;
        boolean noChao = true;
        boolean andando;
        Jogador( int indice, String nome, double baseX, double x, int direcao ) {
            this.indice = indice;
            this.nome = nome;
            this.baseX = baseX;
            this.x = x;
            this.direcao = direcao;
        }
        double velocidade() {
            return 255 - 20 * mao.size();
        }
    }
    static double alturaBase( Jogador j ) {
        return j.base.isEmpty() ? 24 : Math.min( 24, 295.0 / j.base.size() );
    }
    static double topoMao( Jogador j ) {
        return j.y - 15 - j.mao.size() * 27;
    }
    // REGRAS
    static class Partida {
        final Jogador[] jogadores = new Jogador[2];
        final List<Objeto> objetos = new ArrayList<>();
        final Pilha<Acao> historico = new Pilha<>();
        final DkJr dkJr = new DkJr();
        final Plataforma[] plataformas = {
            new Plataforma( 250, 455, 180 ),
            new Plataforma( 570, 455, 180 ),
            new Plataforma( 190, 350, 100 ),
            new Plataforma( 710, 350, 100 ),
            new Plataforma( 420, 350, 160 )
        };
        final Random aleatorio = new Random();
        double tempoRestante;
        double tempoGeracao;
        int proximoId;
        int prioridade;
        int prioridadeBanana;
        boolean iniciada;
        boolean pausada;
        boolean encerrada;
        Partida() {
            reiniciar();
        }
        final void reiniciar() {
            jogadores[0] = new Jogador( 0, "Mario", 110, 220, 1 );
            jogadores[1] = new Jogador( 1, "Luigi", 890, 780, -1 );
            objetos.clear();
            historico.clear();
            dkJr.reiniciar();
            proximoId = 1;
            prioridade = 0;
            prioridadeBanana = 0;
            tempoRestante = DURACAO;
            tempoGeracao = 0;
            iniciada = true;
            pausada = encerrada = false;
            for ( int i = 0; i < 3; i++ ) {
                adicionarPar( 320 + i * 60, i, CHAO );
            }
            adicionarPar( 290, 1, 455 );
            adicionarPar( 380, 2, 455 );
            adicionarPar( 450, 0, 350 );
        }
        boolean emAndamento() {
            return iniciada && !pausada && !encerrada;
        }
        void adicionarPar( double x, int tipo ) {
            adicionarPar( x, tipo, CHAO );
        }
        void adicionarPar( double x, int tipo, double piso ) {
            objetos.add( new Objeto( proximoId++, tipo, x, piso ) );
            objetos.add( new Objeto( proximoId++, tipo, LARGURA - x, piso ) );
        }
        // conta o tempo e cria mais blocos de vez em qndo
        void atualizarTempo( double dt ) {
            if ( !emAndamento() || dt <= 0 ) {
                return;
            }
            for ( Jogador j : jogadores ) {
                j.recargaChute = Math.max( 0, j.recargaChute - dt );
                j.tempoChute = Math.max( 0, j.tempoChute - dt );
                j.atordoado = Math.max( 0, j.atordoado - dt );
                j.invulneravel = Math.max( 0, j.invulneravel - dt );
                j.recargaRoubo = Math.max( 0, j.recargaRoubo - dt );
                j.protecaoBananaMao = Math.max( 0, j.protecaoBananaMao - dt );
                j.protecaoBananaBase = Math.max( 0, j.protecaoBananaBase - dt );
            }
            tempoRestante = Math.max( 0, tempoRestante - dt );
            if ( tempoRestante == 0 ) {
                encerrada = true;
                return;
            }
            tempoGeracao += dt;
            if ( tempoGeracao >= 1.8 ) {
                tempoGeracao %= 1.8;
                if ( objetos.size() <= 14 ) {
                    int nivel = aleatorio.nextInt( 3 );
                    if ( nivel == 0 ) {
                        adicionarPar( 310 + aleatorio.nextInt( 166 ), aleatorio.nextInt( 3 ), CHAO );
                    } else if ( nivel == 1 ) {
                        adicionarPar( 275 + aleatorio.nextInt( 130 ), aleatorio.nextInt( 3 ), 455 );
                    } else {
                        adicionarPar( 440 + aleatorio.nextInt( 46 ), aleatorio.nextInt( 3 ), 350 );
                    }
                }
            }
        }
        double pisoCruzado( double x, double metade, double pesAntes, double pesDepois ) {
            double piso = CHAO;
            for ( Plataforma p : plataformas ) {
                boolean sobre = x + metade > p.x && x - metade < p.x + p.largura;
                if ( sobre && pesAntes <= p.y + 0.001 && pesDepois >= p.y && p.y < piso ) {
                    piso = p.y;
                }
            }
            return piso;
        }
        void movimentar( int indice, int dir, boolean pular, double dt ) {
            if ( !emAndamento() || dt <= 0 ) {
                return;
            }
            Jogador j = jogadores[indice];
            if ( j.atordoado > 0 ) {
                dir = 0;
                pular = false;
            }
            j.andando = dir != 0;
            if ( j.andando ) {
                j.direcao = dir;
                j.passos += dt * 12;
            }
            j.x += ( dir * j.velocidade() + j.empurrao ) * dt;
            j.x = Math.max( 22, Math.min( LARGURA - 22, j.x ) );
            j.empurrao *= Math.exp( -6 * dt );
            if ( j.noChao && pular ) {
                j.vy = -590;
            }
            double pesAntes = j.y + ALTURA_JOGADOR;
            j.vy += 1450 * dt;
            j.y += j.vy * dt;
            j.noChao = false;
            if ( j.vy >= 0 ) {
                double piso = pisoCruzado( j.x, 14, pesAntes, j.y + ALTURA_JOGADOR );
                if ( j.y + ALTURA_JOGADOR >= piso ) {
                    j.y = piso - ALTURA_JOGADOR;
                    j.vy = 0;
                    j.noChao = true;
                }
            }
        }
        void atualizarObjetos( double dt ) {
            if ( !emAndamento() || dt <= 0 ) {
                return;
            }
            for ( Objeto o : objetos ) {
                atualizarAnimacao( o, dt );
                o.bloqueioColeta = Math.max( 0, o.bloqueioColeta - dt );
                if ( !o.caindo ) {
                    continue;
                }
                double passo = dt;
                if ( o.suporteQueda != null ) {
                    o.x = o.suporteQueda.x + o.deslocamentoQuedaX;
                    o.y = o.suporteQueda.y + o.deslocamentoQuedaY;
                    passo = Math.max( 0, dt - o.atrasoQueda );
                    o.atrasoQueda = Math.max( 0, o.atrasoQueda - dt );
                    if ( o.atrasoQueda > 0 ) {
                        continue;
                    }
                    o.suporteQueda = null;
                    o.tempoBrilho = 0.22;
                }
                double pesAntes = o.y + TAMANHO_OBJETO;
                o.x += o.vx * passo;
                o.x = Math.max( 18, Math.min( LARGURA - 18, o.x ) );
                o.vy += 1150 * passo;
                o.y += o.vy * passo;
                if ( o.vy >= 0 ) {
                    double piso = pisoCruzado( o.x, TAMANHO_OBJETO / 2.0, pesAntes, o.y + TAMANHO_OBJETO );
                    if ( o.y + TAMANHO_OBJETO >= piso ) {
                        o.y = piso - TAMANHO_OBJETO;
                        o.vx = o.vy = 0;
                        o.caindo = false;
                    }
                }
            }
            for ( Jogador j : jogadores ) {
                j.tempoDerrubada = Math.max( 0, j.tempoDerrubada - dt );
                for ( Objeto o : j.mao.doTopoParaBase() ) {
                    atualizarAnimacao( o, dt );
                }
                for ( Objeto o : j.base.doTopoParaBase() ) {
                    atualizarAnimacao( o, dt );
                }
            }
        }
        private void atualizarAnimacao( Objeto o, double dt ) {
            o.tempoTransporte = Math.max( 0, o.tempoTransporte - dt );
            o.tempoBrilho = Math.max( 0, o.tempoBrilho - dt );
        }
        // começa o chute qnd aperta a tecla
        private boolean iniciarChute( Jogador j, boolean solicitado ) {
            if ( !solicitado || j.recargaChute > 0 || j.atordoado > 0 ) {
                return false;
            }
            j.recargaChute = 0.65;
            j.tempoChute = DURACAO_ANIMACAO_CHUTE;
            j.direcaoChute = j.direcao;
            j.contatoChutePendente = true;
            return true;
        }
        private boolean aoAlcanceDoChute( Jogador atacante, Jogador alvo ) {
            double distancia = ( alvo.x - atacante.x ) * atacante.direcaoChute;
            double alcance = ALCANCE_CHUTE;
            return alvo.invulneravel <= 0 && distancia >= 0 && distancia <= alcance
                    && Math.abs( atacante.y - alvo.y ) <= 32;
        }
        private boolean chegouAoContato( Jogador j ) {
            if ( j.atordoado > 0 || j.tempoChute <= 0 ) {
                j.contatoChutePendente = false;
            }
            double progresso = 1 - j.tempoChute / DURACAO_ANIMACAO_CHUTE;
            if ( j.contatoChutePendente && progresso >= 0.42 ) {
                j.contatoChutePendente = false;
                return true;
            }
            return false;
        }
        void processarChutes( boolean mario, boolean luigi ) {
            if ( !emAndamento() ) {
                return;
            }
            iniciarChute( jogadores[0], mario );
            iniciarChute( jogadores[1], luigi );
            boolean ataqueMario = chegouAoContato( jogadores[0] );
            boolean ataqueLuigi = chegouAoContato( jogadores[1] );
            boolean acertouMario = ataqueMario && aoAlcanceDoChute( jogadores[0], jogadores[1] );
            boolean acertouLuigi = ataqueLuigi && aoAlcanceDoChute( jogadores[1], jogadores[0] );
            if ( acertouMario ) {
                atingir( jogadores[0], jogadores[1] );
            }
            if ( acertouLuigi ) {
                atingir( jogadores[1], jogadores[0] );
            }
        }
        private void atingir( Jogador atacante, Jogador alvo ) {
            alvo.empurrao = atacante.direcaoChute * 520;
            alvo.vy = -240;
            alvo.noChao = false;
            alvo.atordoado = 0.18;
            alvo.invulneravel = 0.55;
            List<Integer> derrubados = new ArrayList<>();
            int quantidade = alvo.mao.size();
            while ( !alvo.mao.isEmpty() ) {
                Objeto perdido = alvo.mao.peek();
                double[] atual = perdido.retanguloVisual( alvo.x, topoMao( alvo ), 26, 26 );
                alvo.mao.pop();
                int ordem = derrubados.size();
                perdido.x = atual[0];
                perdido.y = atual[1];
                perdido.larguraQueda = atual[2];
                perdido.alturaQueda = atual[3];
                perdido.tempoTransporte = 0;
                perdido.atrasoQueda = ordem * INTERVALO_QUEDA;
                perdido.suporteQueda = ordem == 0 ? null : alvo;
                perdido.deslocamentoQuedaX = perdido.x - alvo.x;
                perdido.deslocamentoQuedaY = perdido.y - alvo.y;
                perdido.vx = atacante.direcaoChute * ( 155 + ( quantidade - 1 - ordem ) * 85 );
                perdido.vy = -160;
                perdido.caindo = true;
                perdido.bloqueioColeta = 0.3 + perdido.atrasoQueda;
                perdido.tempoBrilho = ordem == 0 ? 0.22 : 0;
                objetos.add( perdido );
                derrubados.add( perdido.id );
            }
            alvo.tempoDerrubada = Math.max( 0, quantidade - 1 ) * INTERVALO_QUEDA;
            historico.push( new Acao( Acao.Tipo.CHUTAR, atacante.indice, derrubados ) );
        }
        // pega a posição do bloquinho do topoh
        double[] topoVisivel( Jogador j, boolean naBase ) {
            Pilha<Objeto> pilha = naBase ? j.base : j.mao;
            if ( pilha.isEmpty() ) {
                return null;
            }
            if ( naBase ) {
                double altura = alturaBase( j );
                return pilha.peek().retanguloVisual( j.baseX,
                        CHAO - 14 - pilha.size() * altura, 52, altura );
            }
            return pilha.peek().retanguloVisual( j.x, topoMao( j ), 26, 26 );
        }
        boolean colidirBanana( double antesX, double antesY, double x, double y, double vx ) {
            if ( !emAndamento() ) {
                return false;
            }
            Jogador escolhido = null;
            boolean baseEscolhida = false;
            boolean empate = false;
            double primeiroContato = Double.POSITIVE_INFINITY;
            for ( Jogador j : jogadores ) {
                for ( int local = 0; local < 2; local++ ) {
                    boolean naBase = local == 1;
                    double[] topo = topoVisivel( j, naBase );
                    if ( topo == null ) {
                        continue;
                    }
                    double contato = fracaoColisao( antesX, antesY, x, y, topo );
                    if ( contato < primeiroContato - 0.000001 ) {
                        escolhido = j;
                        baseEscolhida = naBase;
                        primeiroContato = contato;
                        empate = false;
                    } else if ( Double.isFinite( contato ) && escolhido != null
                            && Math.abs( contato - primeiroContato ) < 0.000001 && j != escolhido ) {
                        empate = true;
                        if ( j.indice == prioridadeBanana ) {
                            escolhido = j;
                            baseEscolhida = naBase;
                        }
                    }
                }
            }
            if ( escolhido == null ) {
                return false;
            }
            if ( empate ) {
                prioridadeBanana = 1 - prioridadeBanana;
            }
            receberBanana( escolhido.indice, baseEscolhida, vx >= 0 ? 1 : -1 );
            return true;
        }
        private double fracaoColisao( double x1, double y1, double x2, double y2, double[] r ) {
            double[] origem = { x1, y1 };
            double[] deslocamento = { x2 - x1, y2 - y1 };
            double[] minimo = { r[0] - r[2] / 2 - 10, r[1] - 10 };
            double[] maximo = { r[0] + r[2] / 2 + 10, r[1] + r[3] + 10 };
            double inicio = 0, fim = 1;
            for ( int eixo = 0; eixo < 2; eixo++ ) {
                if ( Math.abs( deslocamento[eixo] ) < 0.0000001 ) {
                    if ( origem[eixo] < minimo[eixo] || origem[eixo] > maximo[eixo] ) {
                        return Double.POSITIVE_INFINITY;
                    }
                } else {
                    double a = ( minimo[eixo] - origem[eixo] ) / deslocamento[eixo];
                    double b = ( maximo[eixo] - origem[eixo] ) / deslocamento[eixo];
                    inicio = Math.max( inicio, Math.min( a, b ) );
                    fim = Math.min( fim, Math.max( a, b ) );
                    if ( inicio > fim ) {
                        return Double.POSITIVE_INFINITY;
                    }
                }
            }
            return inicio;
        }
        boolean receberBanana( int indice, boolean naBase, int direcao ) {
            if ( !emAndamento() ) {
                return false;
            }
            Jogador j = jogadores[indice];
            Pilha<Objeto> pilha = naBase ? j.base : j.mao;
            double protecao = naBase ? j.protecaoBananaBase : j.protecaoBananaMao;
            if ( pilha.isEmpty() || protecao > 0 ) {
                return false;
            }
            double[] posicao = topoVisivel( j, naBase );
            Objeto perdido = pilha.pop();
            perdido.x = posicao[0];
            perdido.y = posicao[1];
            perdido.tempoTransporte = 0;
            perdido.suporteQueda = null;
            perdido.atrasoQueda = 0;
            perdido.vx = direcao * 145;
            perdido.vy = -130;
            perdido.caindo = true;
            perdido.bloqueioColeta = 0.3;
            perdido.tempoBrilho = 0.25;
            objetos.add( perdido );
            if ( naBase ) {
                j.protecaoBananaBase = 0.55;
            } else {
                j.protecaoBananaMao = 0.55;
            }
            historico.push( new Acao( naBase ? Acao.Tipo.BANANA_BASE : Acao.Tipo.BANANA_MAO,
                    indice, perdido.id ) );
            return true;
        }
        boolean pertoDaBase( Jogador j, double baseX ) {
            return j.noChao && Math.abs( j.y + ALTURA_JOGADOR - CHAO ) < 1
                    && Math.abs( j.x - baseX ) <= ALCANCE_BASE;
        }
        // tira um bloco da base rival e põe na mão
        boolean roubar( int indice ) {
            if ( !emAndamento() ) {
                return false;
            }
            Jogador ladrao = jogadores[indice];
            Jogador dono = jogadores[1 - indice];
            if ( !pertoDaBase( ladrao, dono.baseX ) ) {
                return false;
            }
            if ( !podePegar( ladrao ) || ladrao.recargaRoubo > 0 ) {
                return false;
            }
            if ( dono.base.isEmpty() ) {
                return false;
            }
            Objeto o = dono.base.peek();
            double altura = alturaBase( dono );
            o.transportarDesde( dono.baseX, CHAO - 14 - dono.base.size() * altura, 52, altura );
            dono.base.pop();
            ladrao.mao.push( o );
            ladrao.recargaRoubo = 0.45;
            historico.push( new Acao( Acao.Tipo.ROUBAR, indice, o.id ) );
            return true;
        }
        Objeto maisProximo( Jogador j ) {
            Objeto escolhido = null;
            double menorDistancia = ALCANCE_OBJETO;
            for ( Objeto o : objetos ) {
                double altura = Math.abs( j.y + ALTURA_JOGADOR - o.y - TAMANHO_OBJETO );
                double distancia = Math.hypot( j.x - o.x, altura );
                if ( !o.caindo && o.bloqueioColeta <= 0 && altura <= 36 && distancia <= menorDistancia ) {
                    menorDistancia = distancia;
                    escolhido = o;
                }
            }
            return escolhido;
        }
        boolean podePegar( Jogador j ) {
            if ( j.atordoado > 0 || j.tempoDerrubada > 0 ) {
                return false;
            }
            if ( !j.noChao ) {
                return false;
            }
            if ( j.mao.size() >= CAPACIDADE ) {
                return false;
            }
            return true;
        }
        void processarColetas( boolean mario, boolean luigi ) {
            if ( !emAndamento() ) {
                return;
            }
            if ( mario && pertoDaBase( jogadores[0], jogadores[1].baseX ) && !jogadores[1].base.isEmpty() ) {
                roubar( 0 );
                mario = false;
            }
            if ( luigi && pertoDaBase( jogadores[1], jogadores[0].baseX ) && !jogadores[0].base.isEmpty() ) {
                roubar( 1 );
                luigi = false;
            }
            Objeto a = mario && podePegar( jogadores[0] ) ? maisProximo( jogadores[0] ) : null;
            Objeto b = luigi && podePegar( jogadores[1] ) ? maisProximo( jogadores[1] ) : null;
            if ( a != null && a == b ) {
                double da = Math.abs( jogadores[0].x - a.x );
                double db = Math.abs( jogadores[1].x - b.x );
                int vencedor = da < db ? 0 : ( db < da ? 1 : prioridade );
                if ( da == db ) {
                    prioridade = 1 - prioridade;
                }
                coletar( jogadores[vencedor], a );
            } else {
                coletar( jogadores[0], a );
                coletar( jogadores[1], b );
            }
        }
        private void coletar( Jogador j, Objeto o ) {
            if ( o != null && objetos.remove( o ) ) {
                o.transportarDesde( o.x, o.y, TAMANHO_OBJETO, TAMANHO_OBJETO );
                j.mao.push( o );
                historico.push( new Acao( Acao.Tipo.PEGAR, j.indice, o.id ) );
            }
        }
        // passa um bloco da mão pra base
        boolean depositar( int indice ) {
            if ( !emAndamento() ) {
                return false;
            }
            Jogador j = jogadores[indice];
            if ( j.atordoado > 0 || j.tempoDerrubada > 0 || !pertoDaBase( j, j.baseX ) ) {
                return false;
            }
            if ( j.mao.isEmpty() ) {
                return false;
            }
            Objeto o = j.mao.peek();
            o.transportarDesde( j.x, topoMao( j ), 26, 26 );
            j.mao.pop();
            j.base.push( o );
            historico.push( new Acao( Acao.Tipo.DEPOSITAR, indice, o.id ) );
            return true;
        }
        String resultado() {
            int mario = jogadores[0].base.size();
            int luigi = jogadores[1].base.size();
            return mario == luigi ? "EMPATE!" : ( mario > luigi ? "MARIO VENCEU!" : "LUIGI VENCEU!" );
        }
    }
    @Override
    public synchronized void draw() {
        clearBackground( new Color(103, 190, 250) );
        desenharCenario();
        desenharPlataformas();
        for ( Objeto o : partida.objetos ) {
            double largura = o.suporteQueda == null ? TAMANHO_OBJETO : o.larguraQueda;
            double altura = o.suporteQueda == null ? TAMANHO_OBJETO : o.alturaQueda;
            desenharObjeto( o, o.x, o.y, largura, altura );
            if ( o.suporteQueda != null && o.atrasoQueda <= INTERVALO_QUEDA ) {
                desenharTopo( o.x, o.y, largura, altura );
            }
        }
        for ( Jogador j : partida.jogadores ) {
            desenharBase( j );
            desenharJogador( j );
        }
        partida.dkJr.desenhar( this, spritesDkJr, spriteBanana );
        desenharHUD();
        if ( partida.encerrada ) {
            desenharResultado();
        } else if ( partida.pausada ) {
            desenharModal( "PAUSADO", "ESPAÇO para continuar", "R para reiniciar" );
        }
    }
    private void desenharCenario() {
        fillRectangle( 0, 0, LARGURA, ALTURA, new Color( 103, 190, 250 ) );
        fillRectangle( 0, CHAO, LARGURA, ALTURA - CHAO, new Color( 120, 88, 48 ) );
        fillRectangle( 0, CHAO, LARGURA, 7, new Color( 144, 208, 48 ) );
        fillRectangle( 0, CHAO, LARGURA, 3, new Color( 200, 248, 96 ) );
        drawLine( 0, CHAO, LARGURA, CHAO, ESCURO );
    }
    private void desenharPlataformas() {
        for ( Plataforma p : partida.plataformas ) {
            fillRectangle( p.x + 4, p.y + 4, p.largura, 17, new Color( 26, 52, 63, 70 ) );
            fillRectangle( p.x, p.y, p.largura, 17, new Color( 120, 88, 48 ) );
            fillRectangle( p.x, p.y, p.largura, 5, new Color( 144, 208, 48 ) );
            fillRectangle( p.x, p.y, p.largura, 2, new Color( 200, 248, 96 ) );
            drawRectangle( p.x, p.y, p.largura, 17, ESCURO );
            for ( int x = 25; x < p.largura; x += 25 ) {
                drawRectangle( p.x + x, p.y + 5, 1, 11, new Color( 112, 66, 43 ) );
            }
        }
    }
    private void desenharBase( Jogador j ) {
        Color cor = j.indice == 0 ? VERMELHO : VERDE;
        double altura = alturaBase( j );
        List<Objeto> guardados = j.base.doTopoParaBase();
        for ( int k = 0; k < guardados.size(); k++ ) {
            Objeto o = guardados.get( guardados.size() - 1 - k );
            double y = CHAO - 14 - ( k + 1 ) * altura;
            desenharObjeto( o, j.baseX, y, 52, altura );
        }
        if ( !guardados.isEmpty() ) {
            desenharTopo( j.baseX, CHAO - 14 - guardados.size() * altura, 52, altura );
        }
        fillRectangle( j.baseX - 64, CHAO - 14, 128, 14, cor );
        drawRectangle( j.baseX - 64, CHAO - 14, 128, 14, ESCURO );
        fillRectangle( j.baseX - 55, CHAO - 65, 3, 51, ESCURO );
        fillRectangle( j.baseX - 52, CHAO - 65, 22, 14, cor );
        if ( partida.emAndamento() && partida.pertoDaBase( j, j.baseX ) && !j.mao.isEmpty() ) {
            desenharSeta( j.baseX, CHAO - 14 - ( guardados.size() + 1 ) * altura - 15, GOLD );
        }
    }
    // escolhe a cor de cada tipo de bloco
    private Color corObjeto( int tipo ) {
        switch ( tipo ) {
            case 0: return new Color( 255, 205, 59 );
            case 1: return new Color( 241, 142, 79 );
            default: return new Color( 181, 135, 226 );
        }
    }
    private void desenharObjeto( Objeto o, double cx, double y, double largura, double altura ) {
        double[] retangulo = o.retanguloVisual( cx, y, largura, altura );
        cx = retangulo[0];
        y = retangulo[1];
        largura = retangulo[2];
        altura = retangulo[3];
        double x = cx - largura / 2;
        fillRectangle( x, y, largura, altura, corObjeto( o.tipo ) );
        drawRectangle( x, y, largura, altura, ESCURO );
        if ( altura >= 9 ) {
            fillRectangle( x + 3, y + 3, largura - 6, 2, new Color( 255, 245, 206 ) );
        }
        double raio = Math.min( 6, altura * 0.25 );
        double meio = y + altura * 0.57;
        if ( altura >= 12 ) {
            if ( o.tipo == 0 ) {
                fillCircle( cx, meio, raio, ESCURO );
            } else if ( o.tipo == 1 ) {
                fillRectangle( cx - raio, meio - raio, raio * 2, raio * 2, ESCURO );
            } else {
                for ( int linha = -(int) raio; linha <= raio; linha++ ) {
                    double metade = raio - Math.abs( linha );
                    fillRectangle( cx - metade, meio + linha, metade * 2 + 1, 1, ESCURO );
                }
            }
        }
        if ( o.tempoBrilho > 0 ) {
            Color brilho = new Color( 255, 255, 255,
                    (int) ( 220 * Math.min( 1, o.tempoBrilho / 0.18 ) ) );
            drawRectangle( x - 2, y - 2, largura + 4, altura + 4, brilho );
            drawRectangle( x - 3, y - 3, largura + 6, altura + 6, brilho );
        }
    }
    private void desenharSeta( double cx, double y, Color cor ) {
        fillRectangle( cx - 1, y - 7, 3, 9, cor );
        for ( int k = 0; k < 5; k++ ) {
            fillRectangle( cx - 5 + k, y + k, 11 - k * 2, 2, cor );
        }
    }
    private void desenharTopo( double cx, double y, double largura, double altura ) {
        double x = cx - largura / 2;
        for ( int lado = 0; lado < 2; lado++ ) {
            double bx = lado == 0 ? x - 4 : x + largura + 2;
            fillRectangle( bx, y - 3, 2, altura + 6, GOLD );
            double ponta = lado == 0 ? bx : bx - 4;
            fillRectangle( ponta, y - 3, 6, 2, GOLD );
            fillRectangle( ponta, y + altura + 1, 6, 2, GOLD );
        }
        desenharSeta( cx, y - 12, GOLD );
    }
    private void desenharJogador( Jogador j ) {
        Color camisa = j.indice == 0 ? VERMELHO : VERDE;
        Color pele = new Color( 255, 205, 161 );
        Color azul = new Color( 43, 76, 179 );
        Color sapato = new Color( 108, 65, 39 );
        Color bigode = new Color( 91, 48, 28 );
        double x = j.x - 18;
        double y = j.y;
        double passo = j.andando && j.noChao ? Math.sin( j.passos ) * 3 : 0;
        List<Objeto> carregados = j.mao.doTopoParaBase();
        double cabecaY = y - 10;
        for ( int k = 0; k < carregados.size(); k++ ) {
            desenharObjeto( carregados.get( carregados.size() - 1 - k ), j.x,
                    cabecaY - 5 - ( k + 1 ) * 27, 26, 26 );
        }
        if ( !carregados.isEmpty() ) {
            desenharTopo( j.x, topoMao( j ), 26, 26 );
        }
        boolean chutando = j.tempoChute > 0 && j.atordoado <= 0;
        if ( j.invulneravel > 0 && ( (int) ( j.invulneravel * 18 ) ) % 2 == 1 ) {
            return;
        }
        int lado = chutando ? j.direcaoChute : j.direcao;
        if ( lado == 0 ) {
            lado = 1;
        }
        if ( !chutando ) {
            fillRectangle( x + 4 + passo, y + 42, 10, 10, sapato );
            fillRectangle( x + 22 - passo, y + 42, 10, 10, sapato );
            fillRectangle( x + 8 + passo, y + 31, 8, 12, azul );
            fillRectangle( x + 20 - passo, y + 31, 8, 12, azul );
        } else if ( lado > 0 ) {
            fillRectangle( x + 6, y + 42, 10, 10, sapato );
            fillRectangle( x + 10, y + 31, 8, 12, azul );
            fillRectangle( x + 22, y + 34, 10, 8, azul );
            fillRectangle( x + 30, y + 37, 10, 8, sapato );
        } else {
            fillRectangle( x + 24, y + 42, 10, 10, sapato );
            fillRectangle( x + 22, y + 31, 8, 12, azul );
            fillRectangle( x - 3, y + 34, 10, 8, azul );
            fillRectangle( x - 11, y + 37, 10, 8, sapato );
        }
        fillRectangle( x + 7, y + 22, 22, 12, azul );
        fillRectangle( x + 7, y + 33, 22, 8, azul );
        fillRectangle( x + 12, y + 24, 4, 4, WHITE );
        fillRectangle( x + 20, y + 24, 4, 4, WHITE );
        if ( j.mao.isEmpty() ) {
            fillRectangle( x + 1, y + 24, 6, 10, camisa );
            fillRectangle( x + 29, y + 24, 6, 10, camisa );
            fillRectangle( x + 0, y + 32, 6, 4, WHITE );
            fillRectangle( x + 29, y + 32, 6, 4, WHITE );
        } else {
            fillRectangle( x + 2, y + 8, 6, 22, camisa );
            fillRectangle( x + 28, y + 8, 6, 22, camisa );
            fillRectangle( x + 1, y + 4, 7, 6, WHITE );
            fillRectangle( x + 28, y + 4, 7, 6, WHITE );
        }
        fillRectangle( x + 9, y + 6, 18, 16, pele );
        fillRectangle( x + 7, y, 22, 8, camisa );
        fillRectangle( x + 10, y + 5, 18, 3, camisa );
        if ( lado >= 0 ) {
            fillRectangle( x + 20, y + 10, 3, 4, ESCURO );
            fillRectangle( x + 24, y + 13, 6, 5, pele );
            fillRectangle( x + 15, y + 18, 11, 3, bigode );
        } else {
            fillRectangle( x + 14, y + 10, 3, 4, ESCURO );
            fillRectangle( x + 6, y + 13, 6, 5, pele );
            fillRectangle( x + 11, y + 18, 11, 3, bigode );
        }
    }
    // mostra o tempo q falta
    private void desenharHUD() {
        String segundos = String.format( "%02d", (int) Math.ceil( partida.tempoRestante ) );
        textoCentral( segundos, 40, 40, partida.tempoRestante <= 10 ? GOLD : WHITE );
    }
    private void textoCentral( String texto, double y, int fonte, Color cor ) {
        drawText( texto, LARGURA / 2.0 - measureText( texto, fonte ) / 2.0, y, fonte, cor );
    }
    private void desenharResultado() {
        desenharModal( partida.resultado(),
                "Mario: " + partida.jogadores[0].base.size() + " objetos   |   Luigi: "
                + partida.jogadores[1].base.size() + " objetos", "R para jogar novamente" );
    }
    private void desenharModal( String titulo, String linha1, String linha2 ) {
        fillRectangle( 0, 104, LARGURA, 487, new Color( 11, 25, 39, 175 ) );
        fillRectangle( 185, 237, 630, 213, ESCURO );
        textoCentral( titulo, 285, 38, GOLD );
        textoCentral( linha1, 347, 22, WHITE );
        textoCentral( linha2, 406, 18, LIGHTGRAY );
    }
    public static void main( String[] args ) {
        new TorreDoMario();
    }
}
