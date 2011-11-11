package util.algebra;

public interface MatrixND<T> {

	//Methods
	public int getNumDimensions();
	public int[] getDimensions();
	public T getElement(int[] position);
	public void setElement(int[] position, T element);
	public MatrixND<T> clone();
	
}
