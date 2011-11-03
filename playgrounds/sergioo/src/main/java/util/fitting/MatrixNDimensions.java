package util.fitting;

public interface MatrixNDimensions<T> {

	//Methods
	public int getNumDimensions();
	public int[] getDimensions();
	public T getElement(int[] position);
	public void setElement(int[] position, T value);
	public MatrixNDimensions<T> clone();
	
}
