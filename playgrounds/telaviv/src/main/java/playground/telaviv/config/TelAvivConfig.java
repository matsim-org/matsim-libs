/* *********************************************************************** *
 * project: org.matsim.*
 * TelAvivConfig.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.telaviv.config;

public class TelAvivConfig {

//	public static String basePath = "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/cdobler/workspace/matsim/mysimulations/telaviv_2012/";
	public static String basePath = "../../matsim/mysimulations/telaviv_2012";
	
	/*
	 * Name of the field in the person object attributes that defines
	 * to which sub-population an agents belongs to.
	 */
	public static String subPopulationConfigName = "subpopulation";
	
	/*
	 * Agents are either part of the default population or external agents.
	 * The later only update their routes but do not change their activity timings.
	 */
	public static String externalAgent = "externalAgent";

	/*
	 * We have three different sources of external trips:
	 * car, truck and commercial
	 */
	public static String externalTripType = "externalTripType";
	
	public static String externalTripTypeCar = "car";
	public static String externalTripTypeTruck = "truck";
	public static String externalTripTypeCommercial = "commercial";
	
	public static double carPcuEquivalents = 1.0;
	public static double truckPcuEquivalents = 3.5;
	public static double commercialPcuEquivalents = 2.0;	// was previously 1.0; cdobler, oct'13
	
	public static double carMaximumVelocity = 120/3.6;
	public static double truckMaximumVelocity = 90/3.6;
	public static double commercialMaximumVelocity = 100/3.6;
}
