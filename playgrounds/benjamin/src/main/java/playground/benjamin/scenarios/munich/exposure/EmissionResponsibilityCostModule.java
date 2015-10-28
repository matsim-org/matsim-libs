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
package playground.benjamin.scenarios.munich.exposure;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;

import playground.benjamin.internalization.EmissionCostFactors;


/**
 * @author benjamin
 *
 */
public class EmissionResponsibilityCostModule {
	private static final Logger logger = Logger.getLogger(EmissionResponsibilityCostModule.class);
	
	private final double emissionCostMultiplicationFactor;
	private boolean considerCO2Costs = false;
	
	private ResponsibilityGridTools responsibilityGridTools;

	
	public EmissionResponsibilityCostModule(double emissionCostMultiplicationFactor, boolean considerCO2Costs, ResponsibilityGridTools rgt, Map<Id<Link>, Integer> links2xCells, Map<Id<Link>, Integer> links2yCells) {
		this.emissionCostMultiplicationFactor = emissionCostMultiplicationFactor;
		logger.info("Emission costs from Maibach et al. (2008) are multiplied by a factor of " + this.emissionCostMultiplicationFactor);
		
		if(considerCO2Costs){
			this.considerCO2Costs = true;
			logger.info("CO2 emission costs will be calculated... ");
		} else {
			logger.info("CO2 emission costs will NOT be calculated... ");
		}
		
		this.responsibilityGridTools = rgt;
		
	}
	
	public EmissionResponsibilityCostModule(double emissionCostMultiplicationFactor, ResponsibilityGridTools rgt, Map<Id<Link>, Integer> links2xCells, Map<Id<Link>, Integer> links2yCells) {
		this.emissionCostMultiplicationFactor = emissionCostMultiplicationFactor;
		logger.info("Emission costs from Maibach et al. (2008) are multiplied by a factor of " + this.emissionCostMultiplicationFactor);
		logger.info("CO2 emission costs will NOT be calculated... ");
		this.responsibilityGridTools = rgt;
	}

	public double calculateWarmEmissionCosts(Map<WarmPollutant, Double> warmEmissions, Id<Link> linkId, double time) {
		double warmEmissionCosts = 0.0;
		
		for(WarmPollutant wp : warmEmissions.keySet()){
			if(wp.equals(WarmPollutant.CO2_TOTAL) && ! considerCO2Costs) {
				// do nothing
			} else {
				double costFactor = EmissionCostFactors.getCostFactor(wp.toString());
				warmEmissionCosts += warmEmissions.get(wp) * costFactor ;
			}
		}
		//following log statment increases size of the logFile thus commented. amit, Oct'15
//		logger.info("warm emission costs" + warmEmissionCosts);
		// relative density = person minutes of resp. cell and time bin / average person minutes of all cells from this time bin
		Double relativeDensity = responsibilityGridTools.getFactorForLink(linkId, time);
		//following log statment increases size of the logFile thus commented. amit, Oct'15
//		logger.info("relative density" + relativeDensity 
//				+ " on link " + linkId.toString() 
//				+ "resulting costs " + (this.emissionCostFactor*warmEmissionCosts*relativeDensity));
		return this.emissionCostMultiplicationFactor * warmEmissionCosts * relativeDensity;
	}
	
	public double calculateColdEmissionCosts(Map<ColdPollutant, Double> coldEmissions, Id<Link> linkId, double time) {
		double coldEmissionCosts = 0.0;
		
		for(ColdPollutant cp : coldEmissions.keySet()){
			if(cp.equals(WarmPollutant.CO2_TOTAL) && ! considerCO2Costs) {
				// do nothing
			} else {
				double costFactor = EmissionCostFactors.getCostFactor(cp.toString());
				coldEmissionCosts += coldEmissions.get(cp) * costFactor ;
			}
		}
		// relative density = person minutes of resp. cell and time bin / average person minutes of all cells from this time bin
		Double relativeDensity = responsibilityGridTools.getFactorForLink(linkId, time);
		//following log statment increases size of the logFile thus commented. amit, Oct'15
//		logger.info("cold emission costs " + coldEmissionCosts);
		return this.emissionCostMultiplicationFactor * coldEmissionCosts * relativeDensity;
	}

}
