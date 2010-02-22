package playground.mzilske.bvg09;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FreeSpeedCalculator {
	
	private static final double freeSpeedScaleFactor = 1.1;
	
	public static double calculateFreeSpeedForEdge(Map<String, String> edge){
		
		/**
		 * The following pt systems combinations share an edge:<br>
		 *  
		 * R, S, U, T, B, R+S, B+T,<br>
		 * P, V, P+V, B+P, B+P+V, P+R<br>
		 * F, ""<br>
		 *
		 */
		
		// add defaults
		HashMap<String, Double> defaultFreespeedMap = new HashMap<String, Double>();		
		defaultFreespeedMap.put("B", new Double(30/3.6));
		defaultFreespeedMap.put("P", new Double(30/3.6));
		defaultFreespeedMap.put("V", new Double(40/3.6));
		defaultFreespeedMap.put("T", new Double(40/3.6));
		defaultFreespeedMap.put("F", new Double(3/3.6));
		defaultFreespeedMap.put("R", new Double(90/3.6));
		defaultFreespeedMap.put("S", new Double(70/3.6));
		defaultFreespeedMap.put("U", new Double(70/3.6));
		defaultFreespeedMap.put(" ", new Double(70/3.6));

		ArrayList<String> vsysset = new ArrayList<String>(Arrays.asList(edge.get("VSYSSET").split(",")));
		if (vsysset.get(0).equalsIgnoreCase("")){
			vsysset.remove(0);
		}
		double length = Double.parseDouble(edge.get("LAENGE").replace(',', '.')) * 1000;
		double freespeed = Double.NaN;
		
		if (vsysset.size() == 0) {
			// no pt system given, so get the generic speed
			freespeed = defaultFreespeedMap.get(" ").doubleValue();
		} else if (vsysset.size() == 1) {
			// one pt system given, so get it or if zero set a default value
			if (FreeSpeedCalculator.getTimeFromString(edge.get("T-OEVSYS(" + vsysset.get(0) + ")")) == 0.0) {					
				freespeed = length / defaultFreespeedMap.get(vsysset.get(0)).doubleValue();				
			} else {
				freespeed = length / FreeSpeedCalculator.getTimeFromString(edge.get("T-OEVSYS(" + vsysset.get(0) + ")"));
			}
		} else if (vsysset.size() > 1){
			// more than one pt system given
			if (vsysset.contains("B") && vsysset.contains("T")){
				// if both (B and T) are given, favor the faster one
				if (FreeSpeedCalculator.getTimeFromString(edge.get("T-OEVSYS(" + "B" + ")")) != 0 && FreeSpeedCalculator.getTimeFromString(edge.get("T-OEVSYS(" + "T" + ")")) != 0){
					// none is zero
					freespeed = length / Math.min(FreeSpeedCalculator.getTimeFromString(edge.get("T-OEVSYS(" + "B" + ")")), FreeSpeedCalculator.getTimeFromString(edge.get("T-OEVSYS(" + "T" + ")")));
				} else if (FreeSpeedCalculator.getTimeFromString(edge.get("T-OEVSYS(" + "B" + ")")) == 0 && FreeSpeedCalculator.getTimeFromString(edge.get("T-OEVSYS(" + "T" + ")")) == 0){
					// both are zero
					freespeed = 0.0;
					for (String key : vsysset) {
						freespeed = Math.max(freespeed, defaultFreespeedMap.get(key).doubleValue());
					}					
				} else {
					// one is zero, take the bigger one
					freespeed = length / Math.max(FreeSpeedCalculator.getTimeFromString(edge.get("T-OEVSYS(" + "B" + ")")), FreeSpeedCalculator.getTimeFromString(edge.get("T-OEVSYS(" + "T" + ")")));
				}
			} else if (vsysset.contains("B")){
				// more than one pt system given, favor B, cause its travel time was set by the company, travel time of P and V is less accurate (guessed?) 
				if (FreeSpeedCalculator.getTimeFromString(edge.get("T-OEVSYS(" + "B" + ")")) != 0){
					freespeed = length / FreeSpeedCalculator.getTimeFromString(edge.get("T-OEVSYS(" + "B" + ")"));
				} else {
					freespeed = 0.0;
					for (String key : vsysset){
						freespeed = Math.max(freespeed, defaultFreespeedMap.get(key).doubleValue());
					}	
				}
			} else {
				// none of the interesting pt systems here, so simply favor the fastest one
				double minTime = Double.MAX_VALUE;
				for (String key : vsysset){
					if(FreeSpeedCalculator.getTimeFromString(edge.get("T-OEVSYS(" + key + ")")) > 0){
						minTime = Math.min(minTime, FreeSpeedCalculator.getTimeFromString(edge.get("T-OEVSYS(" + key + ")")));
					}
				}
				if(minTime != Double.MAX_VALUE){
					freespeed = length / minTime;
				} else {
					throw new RuntimeException("No travel time given for pt systems " + vsysset.toString());
				}
			}
		}	
		
		if(freespeed == Double.NaN){
			throw new RuntimeException("No value for freespeed given");
		}		
		return freespeed * freeSpeedScaleFactor;
	}
	
	
	public static double getTimeFromString(String string){
		return Double.parseDouble(string.substring(0, string.length() - 1));
	}
}
