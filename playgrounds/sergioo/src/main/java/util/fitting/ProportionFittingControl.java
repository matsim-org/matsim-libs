package util.fitting;

public class ProportionFittingControl extends FittingControl {

	private MatrixNDimensions<Double> controlConstants;
	
	//Constructors
	public ProportionFittingControl(MatrixNDimensions<Double> controlConstants) {
		this.controlConstants = controlConstants;
	}
	
	//Methods
	@Override
	protected void applyRule(MatrixNDimensions<Double> data, int[] position, int dimension) {
		int[] matrixPosition = getMatrixPosition(position, dimension);
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
	}
	
}
