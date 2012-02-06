package others.sergioo.util.algebra;

public class Matrix3DImpl implements MatrixND<Double> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	private double[][][] data;
	
	//Constructors
	public Matrix3DImpl(int[] dimensions) {
		if(dimensions.length==3)
			data = new double[dimensions[0]][dimensions[1]][dimensions[2]];
	}
	public Matrix3DImpl(int[] dimensions, Double value) {
		if(dimensions.length==3) {
			data = new double[dimensions[0]][dimensions[1]][dimensions[2]];
			for(int i=0; i<data.length; i++)
				for(int j=0; j<data[0].length; j++)
					for(int k=0; k<data[0][0].length; k++)
						data[i][j][k] = value;
		}
	}
	public Matrix3DImpl(Matrix3DImpl matrix3Dimensions) {
		data = new double[matrix3Dimensions.getDimension(0)][matrix3Dimensions.getDimension(1)][matrix3Dimensions.getDimension(2)];
		for(int i=0; i<data.length; i++)
			for(int j=0; j<data[0].length; j++)
				for(int k=0; k<data[0][0].length; k++)
					data[i][j][k] = matrix3Dimensions.getElement(new int[]{i, j, k});
	}
	
	//Methods
	@Override
	public int getNumDimensions() {
		return 3;
	}
	@Override
	public int getDimension(int pos) {
		if(pos==0)
			return data.length;
		else if(pos==1)
			return data[0].length;
		else if(pos==2)
			return data[0][0].length;
		else
			return -1;
	}
	@Override
	public Double getElement(int[] position) {
		return data[position[0]][position[1]][position[2]];
	}
	public Double getElement(int p0, int p1, int p2) {
		return data[p0][p1][p2];
	}
	@Override
	public void setElement(int[] position, Double element) {
		data[position[0]][position[1]][position[2]] = element;
	}
	public void setElement(int p0, int p1, int p2, Double element) {
		data[p0][p1][p2] = element;
	}
	public Matrix3DImpl clone() {
		return new Matrix3DImpl(this);
	}
	public double[][][] getData() {
		return data;
	}
	
}
