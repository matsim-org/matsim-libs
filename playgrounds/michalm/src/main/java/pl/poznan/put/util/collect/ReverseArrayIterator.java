package pl.poznan.put.util.collect;

import java.util.*;


public class ReverseArrayIterator<E>
    implements Iterator<E>
{
    private E[] array;
    private int nextIdx;


    public ReverseArrayIterator(E[] array)
    {
        this.array = array;
        nextIdx = array.length - 1;
    }


    @Override
    public boolean hasNext()
    {
        return nextIdx >= 0;
    }


    @Override
    public E next()
    {
        if (nextIdx < 0) {
            throw new NoSuchElementException();
        }

        return array[nextIdx--];
    }


    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
