package util.fitting;

public class TotalFittingControl extends FittingControl{

	private MatrixNDimensions<Double> controlConstants;
	
	//Constructors
	public TotalFittingControl(MatrixNDimensions<Double> controlConstants) {
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
			System.out.println("Puta");
	}

}
