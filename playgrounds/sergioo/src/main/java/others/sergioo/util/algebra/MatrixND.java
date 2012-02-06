package others.sergioo.util.algebra;

import java.io.Serializable;

public interface MatrixND<T> extends Serializable, Cloneable {

	//Methods
	public int getNumDimensions();
	public int getDimension(int pos);
	public T getElement(int[] position);
	public void setElement(int[] position, T element);
	public MatrixND<T> clone();
	
}
