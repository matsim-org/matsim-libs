package util.fitting;

public class MatrixNDimensionsImpl<T> implements MatrixNDimensions<T>, Cloneable {

	//Attributes
	private int[] dimensions;
	private MatrixNDimensionsImpl<T>[] data;
	private T value;
	
	//Constructors
	public MatrixNDimensionsImpl(int[] dimensions) {
		this.dimensions = dimensions;
		if(dimensions.length==0)
			value = null;
		else {
			data = new MatrixNDimensionsImpl[dimensions[dimensions.length-1]];
			for(int i=0; i<data.length; i++)
				data[i] = new MatrixNDimensionsImpl<T>(reduceDimension(dimensions));
		}
	}
	public MatrixNDimensionsImpl(int[] dimensions, T value) {
		this.dimensions = dimensions;
		if(dimensions.length==0)
			this.value = value;
		else {
			data = new MatrixNDimensionsImpl[dimensions[dimensions.length-1]];
			for(int i=0; i<data.length; i++)
				data[i] = new MatrixNDimensionsImpl<T>(reduceDimension(dimensions), value);
		}
	}
	public MatrixNDimensionsImpl(MatrixNDimensionsImpl<T> matrixNDimensions) {
		this.dimensions = matrixNDimensions.getDimensions();
		if(dimensions.length==0)
			value = matrixNDimensions.getElement(new int[]{});
		else {
			data = new MatrixNDimensionsImpl[dimensions[dimensions.length-1]];
			for(int i=0; i<data.length; i++)
				data[i] = new MatrixNDimensionsImpl<T>(matrixNDimensions.data[i]);
		}
	}
	
	//Methods
	@Override
	public int getNumDimensions() {
		return dimensions.length;
	}
	@Override
	public int[] getDimensions() {
		return dimensions;
	}
	@Override
	public T getElement(int[] position) {
		if(position.length==0)
			return value;
		else
			return data[position[position.length-1]].getElement(reduceDimension(position));
	}
	public void setElement(int[] position, T value) {
		if(position.length==0)
			this.value = value;
		else
			data[position[position.length-1]].setElement(reduceDimension(position), value);
	}
	@Override
	public MatrixNDimensions<T> clone() {
		return new MatrixNDimensionsImpl<T>(this);
	}
	private int[] reduceDimension(int[] position) {
		if(position.length==0)
			return null;
		else {
			int[] newPosition = new int[position.length-1];
			for(int i=0; i<newPosition.length; i++)
				newPosition[i]=position[i];
			return newPosition;
		}
	}
	
	//Test main
	public static void main(String[] args) {
		MatrixNDimensions<Double> matrixNDimensions = new MatrixNDimensionsImpl<Double>(new int[]{3,2,4});
		System.out.println(matrixNDimensions.getNumDimensions());
		System.out.println(matrixNDimensions.getDimensions()[2]);
		matrixNDimensions.setElement(new int[]{0,1,2}, 7.0);
		System.out.println(matrixNDimensions.getElement(new int[]{0,1,2}));
		MatrixNDimensions<Double> matrixNDimensions2 = new MatrixNDimensionsImpl<Double>((MatrixNDimensionsImpl<Double>)matrixNDimensions);
		matrixNDimensions2.setElement(new int[]{1,1,2}, 9.0);
		System.out.println(matrixNDimensions2.getElement(new int[]{0,1,2}));
		System.out.println(matrixNDimensions2.getElement(new int[]{1,1,2}));
		System.out.println(matrixNDimensions.getElement(new int[]{1,1,2}));
	}
	
}
