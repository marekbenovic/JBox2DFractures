package org.jbox2d.fracture.util;

import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * Genericka optimalizovana hashovacia tabulka. Implementuje abstraktnu triedu
 * AbstractSet a interface Set, ktore su v standardnej kniznici. Vnutorne datove
 * struktury su protected, takze pri dedeni je ich mozne pouzivat.
 * Princip - separate chaining.
 * 
 * @author Marek Benovic
 *
 * @param <T>
 */

public class HashTable<T> extends AbstractSet<T> implements Set<T> {
    /**
     * Pocet vlozenych vrcholov
     */
    protected int count;
    
    /**
     * hash & n zaruci, ze vysledny hash sa zmesti do hashovacej tabulky.
     * Velkost tabulky je vzdy n + 1 a n je vzdy cislo R^n - 1.
     */
    protected int n;
    
    /**
     * Hashovacia tabulka
     */
    protected Node<T>[] hashtable;
    
    /**
     * Inicializuje hashovaciu tabulku.
     */
    public HashTable() {
        alocate();
    }
    
    /**
     * Vytvori hashovaciu tabulku a vlozi do nej hodnoty z parametra.
     * @param array Vkladane hodnoty
     */
    public HashTable(T[] array) {
        this();
        addAll(Arrays.asList(array));
    }
        
    /**
     * Vrati pocet vlozenych prvkov.
     * 
     * @return Vrati pocet prvkov v type {@code int}.
     */
    @Override
    public int size() {
        return count;
    }
    
    /**
     * Prida prvok z parametra do struktury. Neosetruje sa, ci uz bol rovnaky
     * prvok vlozeny. Na hashovanie hodnot sa pouziva funkcia {@code hashCode}.
     * Casova zlozitost: O(c).
     * 
     * @param value
     * 
     * @return True
     */
    @Override
    public final boolean add(T value) {
        if (count > n) { //ak je v tabulke prilis vela prvkov, tabulku rozsirim (podobny princip ako v ArrayListe) - aby sa minimalizoval pocet kolizii
            realocate();
        }
        Node<T> zaznam = new Node<>(value); //novy prvok
        int code = value.hashCode() & n;
        zaznam.next = hashtable[code];
        hashtable[code] = zaznam; //prvok vlozim do tabulky a spojak, ktory tam existoval prilepim zan
        count++; //zvysim pocet prvkov struktury
        return true;
    }
    
    /**
     * Odstrani prvok z tabulky. Pri hladani prvku zhoda nastane v pripade, ak sa rovnaju
     * referencie prvkov - pouziva sa na porovnavanie operator {@code ==}.
     * Casova zlozitost: O(c).
     * 
     * @param value Hodnota, ktoru treba z tabulky odstranit.
     * 
     * @return <tt>true</tt>, ak sa objekt podarilo vymazat, <tt>false</tt> ak
     *         sa v strukture nenachadzal.
     */
    @Override
    public boolean remove(Object value) {
        int code = value.hashCode() & n;
        Node<T> zaznam = hashtable[code];
        if (zaznam != null) {
            if (zaznam.value.equals(value)) { //ak je zaznam sucastou tabulky (prvy zaznam v spojaku)
                hashtable[code] = zaznam.next;
                count--;
                return true;
            } else { //ak nieje, bude sa hladat v spojaku medzi zkolidovanymi hodnotami
                for (Node<T> dalsi = zaznam.next; dalsi != null; zaznam = dalsi, dalsi = dalsi.next) {
                    if (dalsi.value.equals(value)) {
                        zaznam.next = dalsi.next;
                        count--;
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * @param o Porovnavany objekt
     * @return Vrati objekt, ktory vracia pri porovnavani funkciou equals true
     */
    public T getObject(Object o) {
        for (Node<T> chain = hashtable[o.hashCode() & n]; chain != null; chain = chain.next) {
            if (chain.value.equals(o)) {
                return chain.value;
            }
        }
        return null;
    }
    
    /**
     * @param o
     * @return Vrati true, pokial sa v strukture nachadza dany objekt. Objekty
     *         porovnava pomocou funkcie equal.
     */
    @Override
    public boolean contains(Object o) {
        return getObject(o) != null;
    }
    
    /**
     * Vymaze vsetky prvky zo struktury.
     */
    @Override
    public void clear() {
        alocate();
    }
    
    /**
     * Prekopiruje hodnoty do pola z parametra. Poradie je pseudonahodne a prvky
     * mozu byt vo vystupe rozhadzane akokolvek. V pripade, ze ma pole
     * z parametra vacsiu velkost, ako pocet vlozenych prvkov, hodnoty sa
     * zapisuju od zaciatku pola od indexu 0.
     * 
     * @param <U> Typ pola
     * @param a Pole, do ktoreho sa prekopiruju hodnoty.
     */
    @Override
    public <U> U[] toArray(U[] a) {
        if (a == null) {
            throw new NullPointerException();
        }
        int ln = a.length;
        if (ln < count) {
            a = (U[]) Array.newInstance(a.getClass().getComponentType(), count);
        }
        int index = 0;
        for (Node<T> zaznam : hashtable) {
            while (zaznam != null) {
                a[index++] = (U) zaznam.value;
                zaznam = zaznam.next;
            }
        }
        if (ln > count) {
            a[count] = null;
        }
        return a;
    }
    
    /**
     * Zvacsi hashovaciu tabulku na dvojnasobok a prehashuje vsetky hodnoty v nej.
     */
    private void realocate() {
        n = (n << 1) | 1; //tabulku rozsirim na dvojnasobok
        Node<T>[] newTable = new Node[n + 1]; //nova tabulka
        for (Node<T> chain : hashtable) { //prejdem prvky tabulky
            while (chain != null) { //prejdem prvky jednotlivych spojovych zoznamov a prehodim prvky do novej tabulky
                Node<T> next = chain.next;
                int code = chain.value.hashCode() & n;
                chain.next = newTable[code];
                newTable[code] = chain;
                chain = next;
            }
        }
        hashtable = newTable; //novu rozsirenu tabulku vlozim namiesto tej starej
    }
    
    private void alocate() {
        count = 0;
        n = 1;
        hashtable = new Node[n + 1];
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException("Not supported yet."); //Nieje naimplementovane
    }
}