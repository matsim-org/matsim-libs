package others.sergioo.util.fitting;

import others.sergioo.util.algebra.MatrixND;

public class TotalFittingControlND extends FittingControlND {

	//Constructors
	public TotalFittingControlND(MatrixND<Double> controlConstants) {
		super(controlConstants);
	}
	
	//Methods
	@Override
	protected void applyRule(MatrixND<Double> data, int[] position, int[] dimensions) {
		int[] matrixPosition = new int[data.getNumDimensions()];
		getMatrixPosition(matrixPosition, position, dimensions);
		double[] sum = new double[]{0};
		sum(sum, 0, dimensions, matrixPosition, data);
		if(!(controlConstants.getElement(position)==0 && sum[0]==0)) {
			matrixPosition = new int[data.getNumDimensions()];
			getMatrixPosition(matrixPosition, position, dimensions);
			modifyData(sum[0], 0, dimensions, matrixPosition, data, position);
		}
	}

	private void sum(double[] sum, int ds, int[] dimensions, int[] matrixPosition, MatrixND<Double> data) {
		if(ds==dimensions.length)
			sum[0]+=data.getElement(matrixPosition);
		else
			for(int d=0; d<data.getDimension(dimensions[ds]); d++) {
				matrixPosition[dimensions[ds]] = d;
				sum(sum, ds+1, dimensions, matrixPosition, data);
			}
	}
	
	private void modifyData(double sum, int ds, int[] dimensions, int[] matrixPosition, MatrixND<Double> data, int[] position) {
		if(ds==dimensions.length)
			data.setElement(matrixPosition, data.getElement(matrixPosition)*controlConstants.getElement(position)/sum);
		else
			for(int d=0; d<data.getDimension(dimensions[ds]); d++) {
				matrixPosition[dimensions[ds]] = d;
				modifyData(sum, ds+1, dimensions, matrixPosition, data, position);
			}
	}
}
