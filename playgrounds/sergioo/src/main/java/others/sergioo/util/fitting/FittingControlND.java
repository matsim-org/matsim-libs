package others.sergioo.util.fitting;

import others.sergioo.util.algebra.MatrixND;

public abstract class FittingControlND {

	//Attributes
	int n;
	protected MatrixND<Double> controlConstants;
	
	//Constructors
	public FittingControlND(MatrixND<Double> controlConstants) {
		this.controlConstants = controlConstants;
	}
	
	//Methods
	public MatrixND<Double> getControlConstants() {
		return controlConstants;
	}
	public void iterate(MatrixND<Double> data, int[] dimensions) {
		int[] positionSize = new int[data.getNumDimensions()-dimensions.length];
		int j=0;
		for(int i=0; i<data.getNumDimensions(); i++) {
			boolean inDimension = false;
			for(int d=0; d<dimensions.length; d++)
				if(i==dimensions[d])
					inDimension = true;
			if(!inDimension) {
				positionSize[j] = data.getDimension(i);
				j++;
			}
		}
		applyRules(data, 0, new int[positionSize.length], positionSize, dimensions);
	}
	protected void applyRules(MatrixND<Double> data, int dim, int[] position, int[] positionSize, int[] dimensions) {
		if(dim==positionSize.length)
			applyRule(data, position, dimensions);
		else
			for(int i=0; i<positionSize[dim]; i++) {
				position[dim] = i;
				applyRules(data, dim+1, position, positionSize, dimensions);
			}
	}
	protected void getMatrixPosition(int[] matrixPosition, int[] position, int[] dimensions) {
		int j=0;
		for(int i=0; i<matrixPosition.length; i++) {
			boolean inDimension = false;
			for(int d=0; d<dimensions.length; d++)
				if(i==dimensions[d])
					inDimension = true;
			if(!inDimension) {
				matrixPosition[i] = position[j];
				j++;
			}
		}
	}
	protected abstract void applyRule(MatrixND<Double> data, int[] position, int[] dimensions);

}
