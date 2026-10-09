package template;
import java.util.ArrayList;
import java.util.List;
// (LIFO)
public class Pilha<T> {
    private static class No<T> {
        T valor;
        No<T> abaixo;
        No( T valor, No<T> abaixo ) {
            this.valor = valor;
            this.abaixo = abaixo;
        }
    }
    private No<T> topo;
    private int tamanho;
    public void push( T valor ) {
        topo = new No<>( valor, topo );
        tamanho++;
    }
    public T pop() {
        if ( topo == null ) {
            return null;
        }
        T v = topo.valor;
        topo = topo.abaixo;
        tamanho--;
        return v;
    }
    public T peek() {
        return topo == null ? null : topo.valor;
    }
    public boolean isEmpty() {
        return topo == null;
    }
    // mostra quantos itens tem
    public int size() {
        return tamanho;
    }
    public void clear() {
        topo = null;
        tamanho = 0;
    }
    public List<T> doTopoParaBase() {
        List<T> lista = new ArrayList<>();
        for ( No<T> n = topo; n != null; n = n.abaixo ) {
            lista.add( n.valor );
        }
        return lista;
    }
}
