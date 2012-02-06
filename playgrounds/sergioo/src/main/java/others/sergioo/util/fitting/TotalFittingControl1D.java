package others.sergioo.util.fitting;

import others.sergioo.util.algebra.MatrixND;

public class TotalFittingControl1D extends FittingControl1D{

	//Constructors
	public TotalFittingControl1D(MatrixND<Double> controlConstants) {
		super(controlConstants);
	}
	
	//Methods
	@Override
	protected void applyRule(MatrixND<Double> data, int[] position, int dimension) {
		int[] matrixPosition = new int[data.getNumDimensions()];
		getMatrixPosition(matrixPosition, position, dimension);
		double sum = 0;
		for(int d=0; d<data.getDimension(dimension); d++) {
			matrixPosition[dimension] = d;
			sum += data.getElement(matrixPosition);
		}
		if(!(controlConstants.getElement(position)==0 && sum==0))
			for(int d=0; d<data.getDimension(dimension); d++) {
				matrixPosition[dimension] = d;
				data.setElement(matrixPosition, data.getElement(matrixPosition)*controlConstants.getElement(position)/sum);
			}
	}
	/*protected void applyRule(MatrixND<Double> data, int[] position, int dimension) {
		int[] matrixPosition = getMatrixPosition(position, dimension);
		double sum = 0;
		for(int d=0; d<data.getDimensions()[dimension]; d++) {
			matrixPosition[dimension]=d;
			sum += data.getElement(matrixPosition);
		}
		if(!(controlConstants.getElement(position)==0 && sum==0))
			for(int d=0; d<data.getDimensions()[dimension]; d++) {
				matrixPosition[dimension]=d;
				data.setElement(matrixPosition, (double) Math.round(data.getElement(matrixPosition)*controlConstants.getElement(position)/sum));
			}
		sum = 0;
		for(int d=0; d<data.getDimensions()[dimension]; d++) {
			matrixPosition[dimension]=d;
			sum += data.getElement(matrixPosition);
		}
		double difference = sum-Math.round(controlConstants.getElement(position));
		if(difference!=0)
			for(int i=0; i<Math.abs(difference); i++) {
				matrixPosition[dimension]=(int) (Math.random()*data.getDimensions()[dimension]);
				if(difference>0) {
					while(data.getElement(matrixPosition)==0)
						matrixPosition[dimension]=(int) (Math.random()*data.getDimensions()[dimension]);
					data.setElement(matrixPosition, data.getElement(matrixPosition)-1);
				}
				else
					data.setElement(matrixPosition, data.getElement(matrixPosition)+1);
			}
		sum = 0;
		for(int d=0; d<data.getDimensions()[dimension]; d++) {
			matrixPosition[dimension]=d;
			sum += data.getElement(matrixPosition);
		}
		if(sum-Math.round(controlConstants.getElement(position))!=0)
			System.out.println("Error");
	}*/

}
