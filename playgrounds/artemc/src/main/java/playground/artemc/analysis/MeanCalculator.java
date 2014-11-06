package playground.artemc.analysis;

import java.util.ArrayList;
public class MeanCalculator {
	
	public Double getMean(ArrayList<Double> values){
		double sum = 0.0;
		for(Double value:values){
			sum = sum + value;
		}
		
		double mean = sum/(double) values.size();
		
		return mean;
	}

}
