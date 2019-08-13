/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accidents.computation;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.contrib.accidents.data.AccidentAreaType;
import org.matsim.contrib.accidents.data.ParkingType;

/**
* @author ikaddoura, mmayobre
*/

public class AccidentFrequencyComputation {
	private static final Logger log = Logger.getLogger(AccidentFrequencyComputation.class);

	//computes the expected number of accidents (per km per year); it needs the AADT
	
		public static double computeAccidentFrequency(
		double AADT,
		double speedLimit,
		double roadWidth,
		//int numberExits, Parameter doesn´t change, always 1 independently of number of Exits per km
		int numberSideRoads, // no Side Roads in our Network
		ParkingType parkingType, //difficult --> OSM Community should improve their work ;) --> Default
		AccidentAreaType areaType
	) {		
//		log.info("Received: ({ demand: " + demand + ", speedLimit: " + speedLimit + ", roadWidth: " + roadWidth + ", numberSideRoads: " + numberSideRoads + ", parkingType: " + parkingType + " & areaType: " + areaType +" .})") ;
		double alphaParameter = 6.09 * Math.pow(10, -4);
		double volumePowerParameter = 0.8; 
		
		//Parameter Speed Limit
		double speedLimitParameter;
		if (speedLimit <= 13.89){ // 50 km/h
			speedLimitParameter = 2.25 ;
		} else if (speedLimit <= 16.67){ // 60 km/h
			speedLimitParameter = 2.85 ;
		} else speedLimitParameter = 1 ;
		
		//Parameter Road width
		double roadWidthParameter;
		if (roadWidth <= 7.5){
			roadWidthParameter = 0.83 ;
		} else if (roadWidth <= 8.5){
			roadWidthParameter = 0.68 ;
		} else roadWidthParameter = 0.80 ;
		
		//Parameter Number of Side Roads per km
		double numberSideRoadsParameter;
		if (numberSideRoads == 0){
			numberSideRoadsParameter = 0.72;
		} else if (numberSideRoads <= 5){
			numberSideRoadsParameter = 0.75;
		} else if (numberSideRoads <= 10){
			numberSideRoadsParameter = 1;
		} else numberSideRoadsParameter = 1.25;
		
		//Parameter Parking Type
		double parkingTypeParameter;
		if (parkingType.toString().equals(ParkingType.Prohibited.toString())){
			parkingTypeParameter = 1.19;
		} else if (parkingType.toString().equals(ParkingType.Rarely.toString())){
			parkingTypeParameter = 1.0;
		} else if (parkingType.toString().equals(ParkingType.BaysAtKerb.toString())){
			parkingTypeParameter = 1.77;
		} else {
			parkingTypeParameter = 1.0;
		}
		
		//Parameter Accident Area Type
		double areaTypeParameter;
		if (areaType.toString().equals(AccidentAreaType.Shops.toString())){
			areaTypeParameter = 2.44;
		} else if (areaType.toString().equals(AccidentAreaType.IndustrialResidentialNeighbourhood.toString())){
			areaTypeParameter = 1.58;
		} else if (areaType.toString().equals(AccidentAreaType.ScatteredHousing.toString())){
			areaTypeParameter = 1.0;
		} else {
			areaTypeParameter = 1.0;
		}
			
		double frequency = alphaParameter * Math.pow(AADT, volumePowerParameter) * speedLimitParameter * roadWidthParameter * numberSideRoadsParameter * parkingTypeParameter * areaTypeParameter;
		// number of accidents per km per year
		return frequency;
	}

}

