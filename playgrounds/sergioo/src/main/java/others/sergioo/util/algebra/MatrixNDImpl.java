package others.sergioo.util.algebra;


public class MatrixNDImpl<T> implements MatrixND<T> {

	//Attributes
	private int[] dimensions;
	private MatrixNDImpl<T>[] data;
	private T value;
	
	//Constructors
	public MatrixNDImpl(int[] dimensions) {
		this.dimensions = dimensions;
		if(dimensions.length==0)
			value = null;
		else {
			data = new MatrixNDImpl[dimensions[dimensions.length-1]];
			for(int i=0; i<data.length; i++)
				data[i] = new MatrixNDImpl<T>(reduceDimension(dimensions));
		}
	}
	public MatrixNDImpl(int[] dimensions, T value) {
		this.dimensions = dimensions;
		if(dimensions.length==0)
			this.value = value;
		else {
			data = new MatrixNDImpl[dimensions[dimensions.length-1]];
			for(int i=0; i<data.length; i++)
				data[i] = new MatrixNDImpl<T>(reduceDimension(dimensions), value);
		}
	}
	public MatrixNDImpl(MatrixNDImpl<T> matrixNDimensions) {
		dimensions = new int[matrixNDimensions.getNumDimensions()];
		for(int d=0; d<matrixNDimensions.getNumDimensions(); d++)
			dimensions[d] = matrixNDimensions.getDimension(d);
		if(dimensions.length==0)
			value = matrixNDimensions.getElement(new int[]{});
		else {
			data = new MatrixNDImpl[dimensions[dimensions.length-1]];
			for(int i=0; i<data.length; i++)
				data[i] = new MatrixNDImpl<T>(matrixNDimensions.data[i]);
		}
	}
	
	//Methods
	@Override
	public int getNumDimensions() {
		return dimensions.length;
	}
	@Override
	public int getDimension(int pos) {
		return dimensions[pos];
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
	public MatrixND<T> clone() {
		return new MatrixNDImpl<T>(this);
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
		MatrixND<Double> matrixNDimensions = new MatrixNDImpl<Double>(new int[]{3,2,4});
		System.out.println(matrixNDimensions.getNumDimensions());
		System.out.println(matrixNDimensions.getDimension(2));
		matrixNDimensions.setElement(new int[]{0,1,2}, 7.0);
		System.out.println(matrixNDimensions.getElement(new int[]{0,1,2}));
		MatrixND<Double> matrixNDimensions2 = new MatrixNDImpl<Double>((MatrixNDImpl<Double>)matrixNDimensions);
		matrixNDimensions2.setElement(new int[]{1,1,2}, 9.0);
		System.out.println(matrixNDimensions2.getElement(new int[]{0,1,2}));
		System.out.println(matrixNDimensions2.getElement(new int[]{1,1,2}));
		System.out.println(matrixNDimensions.getElement(new int[]{1,1,2}));
	}
	
}
