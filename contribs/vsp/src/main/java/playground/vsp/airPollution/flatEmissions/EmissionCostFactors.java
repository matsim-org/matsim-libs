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
	NO2 (0.), // NO2 is included in NOX; we are not separately computing possible additional damages of NO2.  kai/ihab, feb'19
	HC (0.), // not included in Maibach et al; they only have NMVOC, which should be equivalent to our NMHC, see below.
	NMHC (1700. / (1000. * 1000.)), //EURO_PER_GRAMM_NMVOC
	SO2 (11000. / (1000. * 1000.)), //EURO_PER_GRAMM_SO2
	PM (384500. / (1000. * 1000.)), //EURO_PER_GRAMM_PM2_5_EXHAUST
	// (It is not clear if HBEFA reports PM10 or PM2.5.  It seems more plausible that they report all PM.  It also seems that there is no PM above size 10, so this would
	// be equivalent to PM10.  Meaning that we should use the PM10 cost factor from Maibach, not the PM2.5 factor as currently used.)
	CO (0.), // not in Maibach et al
	FC (0.), // (= fuel consumption) not in Maibach et al
	CO2_TOTAL (70. / (1000. * 1000.)); //EURO_PER_GRAMM_CO2

	private double costFactor;
	
	public double getCostFactor(){
		return costFactor;
	}

	/**
	 * This is (since feb/2019) deliberately programmed such that it throws an exception when the string cannot be resolved.
	 *
	 * @return cost factor
	 */
	public static double getCostFactor ( String pollutant ) {
		return EmissionCostFactors.valueOf( pollutant ).getCostFactor() ;
	}

    EmissionCostFactors(double firstArg) {
		this.costFactor = firstArg;
	}
}
