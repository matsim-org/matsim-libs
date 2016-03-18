package playground.dziemke.analysis;

import java.util.Map;

/**
 * @author dziemke
 */
public class AnalysisUtils {
	
	public static void addToMapIntegerKey(Map <Integer, Double> map, double inputValue, int binWidth, int limitOfLastBin, double weight) {
		double inputValueBin = inputValue / binWidth;
		int ceilOfLastBin = limitOfLastBin / binWidth;		
		// Math.ceil returns the higher integer number (but as a double value)
		int ceilOfValue = (int)Math.ceil(inputValueBin);
		if (ceilOfValue < 0) {
			throw new RuntimeException("Lower end of bin may not be smaller than zero!");
		}
				
		if (ceilOfValue >= ceilOfLastBin) {
			ceilOfValue = ceilOfLastBin;
		}
						
		if (!map.containsKey(ceilOfValue)) {
			map.put(ceilOfValue, weight);
		} else {
			double value = map.get(ceilOfValue);
			value = value + weight;
			map.put(ceilOfValue, value);
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