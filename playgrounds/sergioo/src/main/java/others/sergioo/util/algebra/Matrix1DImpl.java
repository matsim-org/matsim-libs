package others.sergioo.util.algebra;

public class Matrix1DImpl implements MatrixND<Double> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	private double[] data;
	
	//Constructors
	public Matrix1DImpl(int[] dimensions) {
		if(dimensions.length==1)
			data = new double[dimensions[0]];
	}
	public Matrix1DImpl(int[] dimensions, Double value) {
		if(dimensions.length==1) {
			data = new double[dimensions[0]];
			for(int i=0; i<data.length; i++)
				data[i] = value;
		}
	}
	public Matrix1DImpl(Matrix1DImpl matrix3Dimensions) {
		data = new double[matrix3Dimensions.getDimension(0)];
		for(int i=0; i<data.length; i++)
			data[i] = matrix3Dimensions.getElement(new int[]{i});
	}
	
	//Methods
	@Override
	public int getNumDimensions() {
		return 1;
	}
	@Override
	public int getDimension(int pos) {
		if(pos==0)
			return data.length;
		else
			return -1;
	}
	@Override
	public Double getElement(int[] position) {
		return data[position[0]];
	}
	public Double getElement(int p0) {
		return data[p0];
	}
	@Override
	public void setElement(int[] position, Double element) {
		data[position[0]] = element;
	}
	public void setElement(int p0, Double element) {
		data[p0] = element;
	}
	public Matrix1DImpl clone() {
		return new Matrix1DImpl(this);
	}
	public double[] getData() {
		return data;
	}
	
}
