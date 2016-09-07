/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.input.urban.scenarioSetup;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils.PatnaUrbanActivityTypes;

/**
 * @author amit
 */

public class PatnaCalibrationUtils {

	PatnaCalibrationUtils(){}

	public enum PatnaDemandLabels {
		ward, // residence
		member, // household size
		sex, age, occupation, //pretty obvious
		monthlyIncome, // current monthly income, splitted into several intervals 
		dailyTransportCost, // current daily transport cost, splitted into several intervals
		originZone, // origin ward 
		destinationZone, //destination ward
		tripPurpose,  // HBW, HBE, HBS, HBO, see PatnaUrbanActivityTypes
		mode, // travel mode
		tripFrequency // trip fequency
	}

	public static String getTravelModeFromCode( final String travelModeCode) {
		String travelMode ;
		switch (travelModeCode) {
		case "1":	// Bus
		case "2":	// Mini Bus
		case "5":	// Motor driven 3W
		case "7" :	// train
		case "9" :	//CycleRickshaw
			travelMode = "pt";	break;								
		case "3":	
			travelMode = "car";	break;
		case "4":	// all 2 W motorized 
			travelMode = "motorbike";	break;							
		case "6" :	//bicycle
			travelMode = "bike";	break;						
		case "8" : 
			travelMode = "walk";	break;
		default : throw new RuntimeException("Travel mode input code "+travelModeCode+" is not recognized. Aborting ...");
		}
		return travelMode;
	}

	public static int getIncomeInterval( final String monthlyIncome){

		double inc = Double.valueOf(monthlyIncome);

		if( inc <= 500.) return 1;
		else if (inc <= 1000.) return 2;
		else if (inc <= 3000.) return 3;
		else if (inc <= 5000.) return 4;
		else if (inc <= 7500.) return 5;
		else if (inc <= 1000.) return 6;
		else  return 7;
	}

	public static int getDailyExpenditureInterval( final String dailyExpenditure){

		double cost = Double.valueOf(dailyExpenditure);

		if( cost <= 10.) return 1;
		else if (cost <= 25.) return 2;
		else if (cost <= 50.) return 3;
		else if (cost <= 100.) return 4;
		else  return 5;
	}

	public static String getTripPurpose(final String inputCode) {
		switch (inputCode) {
		case "1": return PatnaUrbanActivityTypes.work.toString();
		case "2": return PatnaUrbanActivityTypes.educational.toString();
		case "3": return PatnaUrbanActivityTypes.social.toString();
		case "4": return PatnaUrbanActivityTypes.other.toString();
		case "9999": 
		default:
			return PatnaUrbanActivityTypes.unknown.toString();
		}
	}
}