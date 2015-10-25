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


/**
 * @author benjamin
 *
 */
public class EmissionResponsibilityCostModule {
	private static final Logger logger = Logger.getLogger(EmissionResponsibilityCostModule.class);
	
	private final double emissionCostFactor;
	private boolean considerCO2Costs = false;
	
	/*Values taken from IMPACT (Maibach et al.(2008))*/
	private final double EURO_PER_GRAMM_NOX = 9600. / (1000. * 1000.);
	private final double EURO_PER_GRAMM_NMVOC = 1700. / (1000. * 1000.);
	private final double EURO_PER_GRAMM_SO2 = 11000. / (1000. * 1000.);
	private final double EURO_PER_GRAMM_PM2_5_EXHAUST = 384500. / (1000. * 1000.);
	private final double EURO_PER_GRAMM_CO2 = 70. / (1000. * 1000.);

	private ResponsibilityGridTools responsibilityGridTools;

	
	public EmissionResponsibilityCostModule(double emissionCostFactor, boolean considerCO2Costs, ResponsibilityGridTools rgt, Map<Id<Link>, Integer> links2xCells, Map<Id<Link>, Integer> links2yCells) {
		this.emissionCostFactor = emissionCostFactor;
		logger.info("Emission costs from Maibach et al. (2008) are multiplied by a factor of " + this.emissionCostFactor);
		
		if(considerCO2Costs){
			this.considerCO2Costs = true;
			logger.info("CO2 emission costs will be calculated... ");
		} else {
			logger.info("CO2 emission costs will NOT be calculated... ");
		}
		
		this.responsibilityGridTools = rgt;
		
	}
	
	public EmissionResponsibilityCostModule(double emissionCostFactor, ResponsibilityGridTools rgt, Map<Id<Link>, Integer> links2xCells, Map<Id<Link>, Integer> links2yCells) {
		this.emissionCostFactor = emissionCostFactor;
		logger.info("Emission costs from Maibach et al. (2008) are multiplied by a factor of " + this.emissionCostFactor);
		logger.info("CO2 emission costs will NOT be calculated... ");
		this.responsibilityGridTools = rgt;
	}

	public double calculateWarmEmissionCosts(Map<WarmPollutant, Double> warmEmissions, Id<Link> linkId, double time) {
		double warmEmissionCosts = 0.0;
		
		for(WarmPollutant wp : warmEmissions.keySet()){
			if(wp.equals(WarmPollutant.NOX)) {
				double noxCosts = warmEmissions.get(wp) * EURO_PER_GRAMM_NOX;
				warmEmissionCosts += noxCosts;
//				logger.warn("emissions: " + warmEmissions.get(wp) + "\t noxCosts: " + noxCosts);
			} else if(wp.equals(WarmPollutant.NMHC)) {
				double nmhcCosts = warmEmissions.get(wp) * EURO_PER_GRAMM_NMVOC;
				warmEmissionCosts += nmhcCosts;
//				logger.warn("emissions: " + warmEmissions.get(wp) + "\t nmhcCosts: " + nmhcCosts);
			} else if(wp.equals(WarmPollutant.SO2)) {
				double so2Costs = warmEmissions.get(wp) * EURO_PER_GRAMM_SO2;
				warmEmissionCosts += so2Costs;
//				logger.warn("emissions: " + warmEmissions.get(wp) + "\t so2Costs: " + so2Costs);
			} else if(wp.equals(WarmPollutant.PM)) {
				double pmCosts = warmEmissions.get(wp) * EURO_PER_GRAMM_PM2_5_EXHAUST;
				warmEmissionCosts += pmCosts;
//				logger.warn("emissions: " + warmEmissions.get(wp) + "\t  pmCosts: " + pmCosts);
			} else if(wp.equals(WarmPollutant.CO2_TOTAL)){
				if(this.considerCO2Costs) {
					double co2Costs = warmEmissions.get(wp) * EURO_PER_GRAMM_CO2;
					warmEmissionCosts += co2Costs;
//					logger.warn("emissions: " + warmEmissions.get(wp) + "\t co2Costs: " + co2Costs);
				} else ; //do nothing
			}
			else ; //do nothing
		}
		//following log statment increases size of the logFile thus commented. amit, Oct'15
//		logger.info("warm emission costs" + warmEmissionCosts);
		// relative density = person minutes of resp. cell and time bin / average person minutes of all cells from this time bin
		Double relativeDensity = responsibilityGridTools.getFactorForLink(linkId, time);
		//following log statment increases size of the logFile thus commented. amit, Oct'15
//		logger.info("relative density" + relativeDensity 
//				+ " on link " + linkId.toString() 
//				+ "resulting costs " + (this.emissionCostFactor*warmEmissionCosts*relativeDensity));
		return this.emissionCostFactor * warmEmissionCosts * relativeDensity;
	}
	
	public double calculateColdEmissionCosts(Map<ColdPollutant, Double> coldEmissions, Id<Link> linkId, double time) {
		double coldEmissionCosts = 0.0;
		
		for(ColdPollutant cp : coldEmissions.keySet()){
			if(cp.equals(ColdPollutant.NOX)) coldEmissionCosts += coldEmissions.get(cp) * EURO_PER_GRAMM_NOX;
			else if(cp.equals(ColdPollutant.NMHC)) coldEmissionCosts += coldEmissions.get(cp) * EURO_PER_GRAMM_NMVOC;
			else if(cp.equals(ColdPollutant.PM)) coldEmissionCosts += coldEmissions.get(cp) * EURO_PER_GRAMM_PM2_5_EXHAUST;
			else ; //do nothing
		}
		// relative density = person minutes of resp. cell and time bin / average person minutes of all cells from this time bin
		Double relativeDensity = responsibilityGridTools.getFactorForLink(linkId, time);
		//following log statment increases size of the logFile thus commented. amit, Oct'15
//		logger.info("cold emission costs " + coldEmissionCosts);
		return this.emissionCostFactor * coldEmissionCosts * relativeDensity;
	}

}
