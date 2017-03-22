package playground.clruch.red3gen;

/**
 *
 */
public interface MinHeap<Type> {
    public int size();
    public void offer(double key, Type value);
    public void replaceMin(double key, Type value);
    public void removeMin();
    public Type getMin();
    public double getMinKey();
}
