package others.sergioo.util.fitting;

import others.sergioo.util.algebra.MatrixND;

public abstract class FittingControl1D {
	
	//Attributes
	protected MatrixND<Double> controlConstants;
	
	//Constructors
	public FittingControl1D(MatrixND<Double> controlConstants) {
		this.controlConstants = controlConstants;
	}
	
	//Methods
	public MatrixND<Double> getControlConstants() {
		return controlConstants;
	}
	public void iterate(MatrixND<Double> data, int dimension) {
		int[] positionSize = new int[data.getNumDimensions()-1];
		int j=0;
		for(int i=0; i<data.getNumDimensions(); i++)
			if(i!=dimension) {
				positionSize[j] = data.getDimension(i);
				j++;
			}
		applyRules(data, 0, new int[positionSize.length], positionSize, dimension);
	}
	private void applyRules(MatrixND<Double> data, int dim, int[] position, int[] positionSize, int dimension) {
		if(dim==positionSize.length)
			applyRule(data, position, dimension);
		else
			for(int i=0; i<positionSize[dim]; i++) {
				position[dim] = i;
				applyRules(data, dim+1, position, positionSize, dimension);
			}
	}
	protected void getMatrixPosition(int[] matrixPosition, int[] position, int dimension) {
		int j=0;
		for(int i=0; i<matrixPosition.length; i++)
			if(i!=dimension) {
				matrixPosition[i] = position[j];
				j++;
			}
	}
	protected abstract void applyRule(MatrixND<Double> data, int[] position, int dimension);

}
