/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionCostModule.java
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
package playground.benjamin.internalization;

import java.util.Map;

import playground.benjamin.emissions.types.ColdPollutant;
import playground.benjamin.emissions.types.WarmPollutant;

/**
 * @author benjamin
 *
 */
public class EmissionCostModule {
	
	public double calculateWarmEmissionCosts(Map<WarmPollutant, Double> warmEmissions) {
		double warmEmissionCosts = 10.0;
		
		for(WarmPollutant wp : warmEmissions.keySet()){
			
		}
		
		return warmEmissionCosts;
	}
	
	public double calculateColdEmissionCosts(Map<ColdPollutant, Double> coldEmissions) {
		double coldEmissionCosts = 5.0;
		
		for(ColdPollutant cp : coldEmissions.keySet()){
			
		}
		
		return coldEmissionCosts;
	}


}
