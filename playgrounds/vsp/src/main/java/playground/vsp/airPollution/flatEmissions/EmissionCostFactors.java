/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.vsp.airPollution.flatEmissions;
/**
 * @author amit
 * These values are taken from Maibach et al. (2008)
 */
public enum EmissionCostFactors {
	
	NOX (9600. / (1000. * 1000.)), //EURO_PER_GRAMM_NOX
	NMHC (1700. / (1000. * 1000.)), //EURO_PER_GRAMM_NMVOC
	SO2 (11000. / (1000. * 1000.)), //EURO_PER_GRAMM_SO2
	PM (384500. / (1000. * 1000.)), //EURO_PER_GRAMM_PM2_5_EXHAUST
	CO2_TOTAL (70. / (1000. * 1000.)); //EURO_PER_GRAMM_CO2 

	private double costFactors;
	
	public double getCostFactor(){
		return costFactors;
	}
	
	public static double getCostFactor ( String pollutant ) {
		double cf = 0.;
		for (EmissionCostFactors ecf : EmissionCostFactors.values() ){
			if ( ecf.toString().equalsIgnoreCase(pollutant) ) return ecf.getCostFactor();
		}
		return cf;
	}
	
	private EmissionCostFactors(double costFactor){
		this.costFactors = costFactor;
	}
}
