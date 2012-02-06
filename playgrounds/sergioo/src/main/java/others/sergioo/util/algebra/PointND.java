package others.sergioo.util.algebra;

public interface PointND<T> {

	//Methods
	public int getDimension();
	public T getElement(int position);
	public void setElement(int position, T element);
	public double getDistance(PointND<T> other);
	public PointND<T> clone();
	
}
