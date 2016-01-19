package playground.michalm.util;

import java.util.*;


public class MapOfMapIterator<K1, K2, V>
    implements Iterator<V>
{
    private Iterator<Map<K2, V>> outerIter;
    private Iterator<V> innerIter;
    private V next;


    public MapOfMapIterator(Map<K1, Map<K2, V>> mapOfMap)
    {
        outerIter = mapOfMap.values().iterator();
        innerIter = Collections.emptyIterator();
        updateNext();
    }


    @Override
    public V next()
    {
        if (next == null) {
            throw new NoSuchElementException();
        }

        V current = next;
        updateNext();
        return current;
    }


    private void updateNext()
    {
        if (innerIter.hasNext()) {
            next = innerIter.next();
            return;
        }

        while (outerIter.hasNext()) {
            Map<K2, V> row = outerIter.next();

            if (row != null) {
                innerIter = row.values().iterator();
                next = innerIter.next(); // always at least one entry inside a row
                return;
            }
        }

        next = null;
    }


    @Override
    public boolean hasNext()
    {
        return next != null;
    }


    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
