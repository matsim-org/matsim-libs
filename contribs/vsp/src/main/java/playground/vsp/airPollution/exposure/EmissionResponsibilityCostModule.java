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
package playground.vsp.airPollution.exposure;

import java.util.Map;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.Pollutant;

import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import playground.vsp.airPollution.flatEmissions.EmissionCostFactors;


/**
 * This class estimates the air pollution exposure costs of all pollutants except CO2
 * and flat costs of CO2 if considering CO2 costs at all.
 *
 * @author benjamin
 *
 */
public class EmissionResponsibilityCostModule {
	private static final Logger logger = LogManager.getLogger(EmissionResponsibilityCostModule.class);
	
	private final double emissionCostMultiplicationFactor = 1.;
	private final ResponsibilityGridTools responsibilityGridTools;
	private final boolean considerCO2Costs = true;

	@Inject
	EmissionResponsibilityCostModule(EmissionsConfigGroup emissionsConfigGroup, ResponsibilityGridTools rgt) {
//		this.emissionCostMultiplicationFactor = emissionsConfigGroup.getEmissionCostMultiplicationFactor();
//		this.considerCO2Costs = emissionsConfigGroup.isConsideringCO2Costs();

		logger.info("Emission costs from Maibach et al. (2008) are multiplied by a factor of " + this.emissionCostMultiplicationFactor);

		if(this.considerCO2Costs){
			logger.info("CO2 emission costs will be calculated... ");
			logger.warn("The first iteration will include only flat CO2 emission costs because, " +
					"the relative densities from activity durations are estimated after first iteration.");
		} else {
			logger.info("CO2 emission costs will NOT be calculated... ");
		}
		
		this.responsibilityGridTools = rgt;
	}
	
	public double calculateWarmEmissionCosts( Map<Pollutant, Double> warmEmissions, Id<Link> linkId, double time ) {
		double warmEmissionCosts = 0.0;
		
		for( Pollutant wp : warmEmissions.keySet()){
			//		return key;
			if( wp != Pollutant.CO2_TOTAL ) {

//				if ( true ) {
//					throw new RuntimeException("pollutants are no longer enums; need to hedge against header changes in upstream input file") ;
//				}

				double costFactor = EmissionCostFactors.getCostFactor(wp.toString());
				warmEmissionCosts += warmEmissions.get(wp) * costFactor ;
			}
		}
		Double relativeDensity = responsibilityGridTools.getFactorForLink(linkId, time);

		if(this.considerCO2Costs) {
			Pollutant co2Total = Pollutant.CO2_TOTAL;
			//		return key;
			return this.emissionCostMultiplicationFactor * warmEmissionCosts * relativeDensity
					+ warmEmissions.get( co2Total )
					* EmissionCostFactors.getCostFactor(co2Total.toString());
		} else {
			return this.emissionCostMultiplicationFactor * warmEmissionCosts * relativeDensity;
		}
	}
	
	public double calculateColdEmissionCosts( Map<Pollutant, Double> coldEmissions, Id<Link> linkId, double time ) {
		double coldEmissionCosts = 0.0;
		
		for( Pollutant cp : coldEmissions.keySet()){
			double costFactor = EmissionCostFactors.getCostFactor(cp.toString());
			coldEmissionCosts += coldEmissions.get(cp) * costFactor ;
		}
		// relative density = person minutes of resp. cell and time bin / average person minutes of all cells from this time bin
		Double relativeDensity = responsibilityGridTools.getFactorForLink(linkId, time);
		return this.emissionCostMultiplicationFactor * coldEmissionCosts * relativeDensity;
	}
}
