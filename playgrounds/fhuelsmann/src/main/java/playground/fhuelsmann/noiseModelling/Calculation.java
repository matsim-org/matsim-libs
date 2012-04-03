/* *********************************************************************** *
 * project: org.matsim.*
 * NoiseTool.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.fhuelsmann.noiseModelling;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

public class Calculation {

	/* algorithm to calculate noise emissions */
	public Map<Id,Map<String,Double>> calculate_lme (Map<Id,Map<String, double[]>> linkInfos){
		Map<Id,Map<String,Double>> linkId2Time2NoiseEmissions = new TreeMap<Id,Map<String,Double>> ();
		
		for(Entry < Id, Map<String , double[]> > entry : linkInfos.entrySet()){
			Id linkId = entry.getKey();
			Map<String, double[]> timeperiodeToInfos = entry.getValue();
			
			Map<String,Double> timeperiodeToResult = new TreeMap<String,Double>() ;
			
			for (Entry <String , double[]> element : timeperiodeToInfos.entrySet()){
				String timeperiode = element.getKey();
				double [] timeperiodeInfos = element.getValue();
				// we collect now the parameter
				double maxSpeed = timeperiodeInfos [0];
				double total = 10.0 * timeperiodeInfos [1];
				double heavy = 10.0 * timeperiodeInfos [2];
				
				double p = heavy/total;
				double DTV = total;
				// do the calculation
				double l_pkw = 27.7+(10*Math.log10(1.0 + Math.pow(0.02 * maxSpeed, 3.0)));
				double l_lkw = 23.1 + (12.5 * Math.log10(maxSpeed)) ;
				double D = l_lkw-l_pkw ;
				double Dv = l_pkw - 37.3 + 10* Math.log10((100.0 + (Math.pow(10.0, (D/10.0))-1)*p ) / (100.0+8.23*p));
				double lm = calc_lm(timeperiode , DTV, p);
				double lme = lm + Dv ;
				timeperiodeToResult.put(timeperiode, lme);			
			}
			linkId2Time2NoiseEmissions.put(linkId,timeperiodeToResult);
		}
		addBasicValue(linkId2Time2NoiseEmissions);
		return linkId2Time2NoiseEmissions;
	}

		/*calculate Mittelungspegel*/
	public double calc_lm(String periode, double dtv, double p) {
		double lm = 0.0;
		if (periode.equals("Day")) {
			lm = 37.3 + 10.0 * (Math.log10(0.062 * dtv * (1.0 + 0.082 * p)));
		}
		if (periode.equals("Evening")) {
			lm = 37.3 + 10.0 * (Math.log10(0.042 * dtv * (1.0 + 0.082 * p)));
		}
		if (periode.equals("Night")) {
			lm = 37.3 + 10.0 * (Math.log10(0.011 * dtv * (1.0 + 0.082 * p)));
		}
		return lm;
	}
	
	/*calculation of exceptions if there is no value for lme and if there is lme < 37.5 due to very few vehicles*/
	
	private void addBasicValue (Map <Id,Map<String , Double>> linkId2time2NoiseEmissions){
		for (Entry <Id, Map<String, Double>> entry : linkId2time2NoiseEmissions.entrySet()){
			Map<String, Double> timePeriod2lme = entry.getValue();
			Id linkId = entry.getKey();
			for (Entry <String,Double> component : timePeriod2lme.entrySet()){
				String timePeriod =component.getKey();
				Double lme = component.getValue();
				if(lme < 37.3){					
					timePeriod2lme.put(timePeriod, 37.3);				
					linkId2time2NoiseEmissions.put(linkId, timePeriod2lme);
				}	
			}
			
			if(!timePeriod2lme.containsKey("Day")){
				timePeriod2lme.put("Day",37.3);
			}
			if(!timePeriod2lme.containsKey("Evening")){
				timePeriod2lme.put("Evening",37.3);
			}
			if(!timePeriod2lme.containsKey("Night")){
				timePeriod2lme.put("Night",37.3);
			}	
		}		
	}
		
	
	/* calculation of LDEN nach VBUS*/

	
	public Map<Id,Double> cal_lden (Map<Id,Map<String,Double>> linkId2timePeriod2lden){		
		Map<Id,Double> linkId2lden = new TreeMap <Id,Double> ();
		
		for (Entry <Id,Map<String,Double>> entry : linkId2timePeriod2lden.entrySet()){
			Id linkId = entry.getKey();
			Map<String,Double> timeperiode_To_lme = entry.getValue();
			double sum = 0.0;				
			for (Entry <String , Double> element : timeperiode_To_lme.entrySet()){
				String timeperiode = element.getKey();
				double lme = element.getValue();
				sum = sum + calcValue(timeperiode, lme);	
							
			}
			double lden = 10.0 * (Math.log10((1.0/24.0)*sum)) ;
			linkId2lden.put(linkId, lden);		
		}
		return linkId2lden;
	}
	
	private double calcValue(String timePeriod , double lme){
		if (timePeriod.equals("Day")){
			return 12.0*(Math.pow(10.0, lme/10.0));
		}
		
		else if (timePeriod.equals("Evening")){
			return 4.0*(Math.pow(10.0, (lme+5.0)/10.0));
		}
		
		else {
			return 8.0*(Math.pow(10.0,(lme+10.0)/10.0));
		}	
	}

}
