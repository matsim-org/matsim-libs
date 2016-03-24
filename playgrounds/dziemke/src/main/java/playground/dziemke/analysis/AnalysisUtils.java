package playground.dziemke.analysis;

import java.util.Map;

/**
 * @author dziemke
 */
public class AnalysisUtils {
	
	public static void addToMapIntegerKeyCeiling(Map <Integer, Double> map, double inputValue, int binWidth, //int limitOfLastBin, 
			double weight) {
		double inputValueBin = inputValue / binWidth;
//		int ceilOfLastBin = limitOfLastBin / binWidth;
		int ceilOfValue = (int) Math.ceil(inputValueBin); // Math.ceil returns the next higher integer number (but as a double value)
		/* Note: The ceiling of 0. is 0., so the 0. cannot be ecluded */
		if (ceilOfValue < 0) {
			throw new RuntimeException("Lower end of bin may not be smaller than or equal to zero!");
		}
				
//		if (ceilOfValue >= ceilOfLastBin) {
//			ceilOfValue = ceilOfLastBin;
//		}
						
		if (!map.containsKey(ceilOfValue)) {
			map.put(ceilOfValue, weight);
		} else {
			double value = map.get(ceilOfValue);
			value = value + weight;
			map.put(ceilOfValue, value);
		}			
	}
	
	
	public static void addToMapIntegerKeyFloor(Map <Integer, Double> map, double inputValue, int binWidth, //int limitOfLastBin, 
			double weight) {
		double inputValueBin = inputValue / binWidth;
//		int ceilOfLastBin = limitOfLastBin / binWidth;
		int floorOfValue = (int) Math.floor(inputValueBin); // Math.floor returns the next lower integer number (but as a double value)
		if (floorOfValue < 0) {
			throw new RuntimeException("Lower end of bin may not be smaller than zero!");
		}
				
//		if (ceilOfValue >= ceilOfLastBin) {
//			ceilOfValue = ceilOfLastBin;
//		}
						
		if (!map.containsKey(floorOfValue)) {
			map.put(floorOfValue, weight);
		} else {
			double value = map.get(floorOfValue);
			value = value + weight;
			map.put(floorOfValue, value);
		}			
	}


	public static void addToMapStringKey(Map <String, Double> map, String caption, double weight) {
		if (!map.containsKey(caption)) {
			map.put(caption, weight);
		} else {
			double value = map.get(caption);
			value = value + weight;
			map.put(caption, value);
		}
	}
}