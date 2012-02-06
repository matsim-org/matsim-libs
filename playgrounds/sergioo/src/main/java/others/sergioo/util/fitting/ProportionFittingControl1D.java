package others.sergioo.util.fitting;

import others.sergioo.util.algebra.MatrixND;

public class ProportionFittingControl1D extends FittingControl1D {

	//Constructors
	public ProportionFittingControl1D(MatrixND<Double> controlConstants) {
		super(controlConstants);
	}
	
	//Methods
	@Override
	protected void applyRule(MatrixND<Double> data, int[] position, int dimension) {
		int[] matrixPosition = new int[data.getNumDimensions()];
		getMatrixPosition(matrixPosition, position, dimension);
		double sum = 0;
		for(int d=0; d<data.getDimension(dimension); d++) {
			matrixPosition[dimension]=d;
			sum += data.getElement(matrixPosition);
		}
		for(int d=0; d<data.getDimension(dimension); d++) {
			matrixPosition[dimension]=d;
			int[] controlPosition = new int[position.length+1];
			for(int i=0; i<position.length; i++)
				controlPosition[i] = position[i];
			controlPosition[controlPosition.length-1]=d;
			double value = sum*controlConstants.getElement(controlPosition);
			data.setElement(matrixPosition, value);
		}
	}
	/*protected void applyRule(MatrixND<Double> data, int[] position, int dimension) {
		int[] matrixPosition = new int[data.getNumDimensions()];
		getMatrixPosition(matrixPosition, position, dimension);
		double sum = 0;
		for(int d=0; d<data.getDimensions()[dimension]; d++) {
			matrixPosition[dimension]=d;
			sum += data.getElement(matrixPosition);
		}
		double lower = Double.MAX_VALUE;
		for(int d=0; d<data.getDimensions()[dimension]; d++) {
			matrixPosition[dimension]=d;
			int[] controlPosition = new int[position.length+1];
			for(int i=0; i<position.length; i++)
				controlPosition[i] = position[i];
			controlPosition[controlPosition.length-1]=d;
			double value = sum*controlConstants.getElement(controlPosition);
			data.setElement(matrixPosition, value);
			if(value<lower)
				lower = value;
		}
		if(lower<1 && lower>0.1)
			for(int d=0; d<data.getDimensions()[dimension]; d++) {
				matrixPosition[dimension]=d;
				data.setElement(matrixPosition, data.getElement(matrixPosition)/lower);
			}
		for(int d=0; d<data.getDimensions()[dimension]; d++) {
			matrixPosition[dimension]=d;
			data.setElement(matrixPosition, (double) Math.round(data.getElement(matrixPosition)));
		}
	}*/
	
}
