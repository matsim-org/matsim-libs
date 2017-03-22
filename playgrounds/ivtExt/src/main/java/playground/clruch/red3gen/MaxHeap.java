package playground.clruch.red3gen;

/**
 *
 */
public interface MaxHeap<Type> {
    public int size();
    public void offer(double key, Type value);
    public void replaceMax(double key, Type value);
    public void removeMax();
    public Type getMax();
    public double getMaxKey();
}