package others.sergioo.util.fitting;

import others.sergioo.util.fitting.FittingControlND;
import others.sergioo.util.algebra.MatrixND;

public class ProportionFittingControlND extends FittingControlND {
	
	//Attributes
	private int[] dimensionsTo;
	
	//Constructors
	public ProportionFittingControlND(MatrixND<Double> controlConstants, int[] dimensionsTo) {
		super(controlConstants);
		this.dimensionsTo = dimensionsTo;
	}
	
	//Methods
	@Override
	protected void applyRule(MatrixND<Double> data, int[] position, int[] dimensionsFrom) {
		int[] matrixPosition = new int[data.getNumDimensions()];
		getMatrixPosition(matrixPosition, position, dimensionsFrom);
		double[] sum = new double[]{0};
		sum(sum, 0, dimensionsFrom, matrixPosition, data);
		int[] positionSize = new int[dimensionsFrom.length-dimensionsTo.length];
		int j=0;
		for(int i=0; i<dimensionsFrom.length; i++) {
			boolean inDimension = false;
			for(int d=0; d<dimensionsTo.length; d++)
				if(dimensionsFrom[i]==dimensionsTo[d])
					inDimension=true;
			if(!inDimension) {
				positionSize[j] = data.getDimension(dimensionsFrom[i]);
				j++;
			}
		}
		applyRules2(data, 0, position, new int[positionSize.length], positionSize, dimensionsFrom, sum[0]);
	}
	protected void applyRules2(MatrixND<Double> data, int dim, int[] position, int[] position2, int[] positionSize, int[] dimensionsFrom, double sum){
		if(dim==positionSize.length)
			applyRule2(data, position, position2, dimensionsFrom, sum);
		else
			for(int i=0; i<positionSize[dim]; i++) {
				position2[dim] = i;
				applyRules2(data, dim+1, position, position2, positionSize, dimensionsFrom, sum);
			}
	}
	protected void applyRule2(MatrixND<Double> data, int[] position, int[] position2, int[] dimensionsFrom, double sum) {
		int[] matrixPosition = new int[data.getNumDimensions()];
		getMatrixPosition(matrixPosition, position, dimensionsFrom);
		getMatrixPosition2(matrixPosition, position2, dimensionsFrom);
		double[] sum2 = new double[]{0};
		sum(sum2, 0, dimensionsTo, matrixPosition, data);
		int[] positionf = new int[data.getNumDimensions()-dimensionsTo.length];
		int j=0, k=0, l=0;
		for(int i=0; i<data.getNumDimensions(); i++) {
			boolean inDimensionFrom = false;
			int d=0;
			for(; d<dimensionsFrom.length && !inDimensionFrom; d++)
				if(i==dimensionsFrom[d]) {
					inDimensionFrom = true;
					d--;
				}
			if(!inDimensionFrom) {
				positionf[j] = position[k];
				j++;
				k++;
			}
			else {
				boolean inDimensionTo = false;
				for(int d2=0; d2<dimensionsTo.length; d2++)
					if(dimensionsFrom[d]==dimensionsTo[d2])
						inDimensionTo=true;
				if(!inDimensionTo) {
					positionf[j] = position2[l];
					j++;
					l++;
				}
			}
		}
		if(!(controlConstants.getElement(positionf)==0 && sum==0)) {
			matrixPosition = new int[data.getNumDimensions()];
			getMatrixPosition(matrixPosition, position, dimensionsFrom);
			getMatrixPosition2(matrixPosition, position2, dimensionsFrom);
			modifyData(sum*controlConstants.getElement(positionf), sum2[0], 0, matrixPosition, data);
		}
	}
	protected void getMatrixPosition2(int[] matrixPosition, int[] position, int[] dimensionsFrom) {
		int j=0;
		for(int i=0; i<dimensionsFrom.length; i++) {
			boolean inDimension = false;
			for(int d=0; d<dimensionsTo.length; d++)
				if(dimensionsFrom[i]==dimensionsTo[d])
					inDimension=true;
			if(!inDimension) {
				matrixPosition[dimensionsFrom[i]] = position[j];
				j++;
			}
		}
	}
	private void sum(double[] sum, int ds, int[] dimensions, int[] matrixPosition, MatrixND<Double> data) {
		if(ds==dimensions.length)
			sum[0]+=data.getElement(matrixPosition);
		else
			for(int d=0; d<data.getDimension(dimensions[ds]); d++) {
				matrixPosition[dimensions[ds]]=d;
				sum(sum, ds+1, dimensions, matrixPosition, data);
			}
	}
	private void modifyData(double sum, double sum2, int ds, int[] matrixPosition, MatrixND<Double> data) {
		if(ds==dimensionsTo.length)
			data.setElement(matrixPosition, data.getElement(matrixPosition)*sum/sum2);
		else
			for(int d=0; d<data.getDimension(dimensionsTo[ds]); d++) {
				matrixPosition[dimensionsTo[ds]]=d;
				modifyData(sum, sum2, ds+1, matrixPosition, data);
			}
	}
	
}
