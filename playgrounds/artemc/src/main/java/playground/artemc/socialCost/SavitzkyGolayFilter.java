package playground.artemc.socialCost;

import java.util.HashMap;

public class SavitzkyGolayFilter {

	private int windowSize = 5;
	private double[] coefficients;
	private double[] data;
	private boolean positive;
	
	private final double[] coefficients5 = {-3.0,12.0,17.0,12.0,-3.0}; 
	private final double[] coefficients7 = {-2.0,3.0,6.0,7.0,6.0,3.0,-2.0}; 
	HashMap<Integer, double[]> coefficientsMap;
	
	
	public SavitzkyGolayFilter(double[] data){
		this(5, data, false);
	}
	
	public SavitzkyGolayFilter(double[] data, boolean positive){
		this(5, data, positive);
	}
	
	public SavitzkyGolayFilter(int windowSize, double[] data, boolean positive) {
		this.coefficientsMap = new HashMap<Integer, double[]>();
		coefficientsMap.put(5, coefficients5);
		coefficientsMap.put(7, coefficients7);
		coefficients = coefficientsMap.get(windowSize);
		this.data = data;
		this.positive = positive;
	}
	
	public double[] appllyFilter(){
	
		double[] y = new double[coefficients.length];
		for(int i=0; i<y.length;i++){
			y[i]=0;
		}
		
		double[] smoothData = new double[data.length];
		for (int k = 0; k < data.length; k++){
		
			//Calculate new value for k 
			int q = (coefficients.length-1)/2;
			for(int pos=0;pos<coefficients.length;pos++){
				if((k-q+pos)>=0 && (k-q+pos)<data.length){
					y[pos] = coefficients[pos] * data[k-q+pos];	
				}		
				else{
					y[pos] =0;
				}
			}
			
			
			
			double sumValues = 0.0;
			double sumCoefficients = 0.0;
			for(int i=0; i<y.length;i++){
				sumValues=sumValues+y[i];
				sumCoefficients=sumCoefficients+coefficients[i];
			}
			
			smoothData[k] = sumValues/sumCoefficients;
			
			if(positive && smoothData[k]<0) smoothData[k] = 0.0;
		}
		
		return smoothData;
	}


	public int getWindowSize() {
		return windowSize;
	}


	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}
	
	
	
}
