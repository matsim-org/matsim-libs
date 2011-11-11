package util.fitting;

import util.algebra.MatrixND;

public abstract class FittingControl {
	
	//Methods
	public void iterate(MatrixND<Double> data, int dimension) {
		int[] positionSize = new int[data.getNumDimensions()-1];
		int j=0;
		for(int i=0; i<data.getNumDimensions(); i++)
			if(i!=dimension) {
				positionSize[j] = data.getDimensions()[i];
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
	protected int[] getMatrixPosition(int[] position, int dimension) {
		int[] matrixPosition = new int[position.length+1];
		int j=0;
		for(int i=0; i<matrixPosition.length; i++)
			if(i!=dimension) {
				matrixPosition[i] = position[j];
				j++;
			}
		return matrixPosition;
	}
	protected abstract void applyRule(MatrixND<Double> data, int[] position, int dimension);

}
