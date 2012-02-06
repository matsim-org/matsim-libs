package others.sergioo.util.algebra;

public class Matrix2DImpl implements MatrixND<Double> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	private double[][] data;
	
	//Constructors
	public Matrix2DImpl(int[] dimensions) {
		if(dimensions.length==2)
			data = new double[dimensions[0]][dimensions[1]];
	}
	public Matrix2DImpl(int[] dimensions, Double value) {
		if(dimensions.length==2) {
			data = new double[dimensions[0]][dimensions[1]];
			for(int i=0; i<data.length; i++)
				for(int j=0; j<data[0].length; j++)
					data[i][j] = value;
		}
	}
	public Matrix2DImpl(Matrix2DImpl matrix3Dimensions) {
		data = new double[matrix3Dimensions.getDimension(0)][matrix3Dimensions.getDimension(1)];
		for(int i=0; i<data.length; i++)
			for(int j=0; j<data[0].length; j++)
				data[i][j] = matrix3Dimensions.getElement(new int[]{i, j});
	}
	
	//Methods
	@Override
	public int getNumDimensions() {
		return 2;
	}
	@Override
	public int getDimension(int pos) {
		if(pos==0)
			return data.length;
		else if(pos==1)
			return data[0].length;
		else
			return -1;
	}
	@Override
	public Double getElement(int[] position) {
		return data[position[0]][position[1]];
	}
	public Double getElement(int p0, int p1) {
		return data[p0][p1];
	}
	@Override
	public void setElement(int[] position, Double element) {
		data[position[0]][position[1]] = element;
	}
	public void setElement(int p0, int p1, Double element) {
		data[p0][p1] = element;
	}
	public Matrix2DImpl clone() {
		return new Matrix2DImpl(this);
	}
	public double[][] getData() {
		return data;
	}
	
}
