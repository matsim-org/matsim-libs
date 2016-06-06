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

/**
 * @author amit
 */

public class PatnaCalibrationUtils {
	
	public enum PatnaDemandLabels {
		ward, // residence
		member, // household size
		sex, age, occupation, //pretty obvious
		monthlyInc, // current monthly income, splitted into several intervals 
		dailyCost, // current daily transport cost, splitted into several intervals
		originWard, // origin
		destiWard, //destination
		purpose,  // HBW, HBE, HBS, HBO, see PatnaUrbanActivityTypes
		mode, // travel mode
		freq // trip fequency
	}

	public static String getTravelModeFromCode( final String travelModeCode) {
		String travelMode ;
		switch (travelModeCode) {
		case "1":	// Bus
		case "2":	// Mini Bus
		case "5":	// Motor driven 3W
		case "7" :	// train
			travelMode = "pt";	break;								
		case "3":	
			travelMode = "car";	break;
		case "4":	// all 2 W motorized 
			travelMode = "motorbike";	break;							
		case "6" :	//bicycle
		case "9" :	//CycleRickshaw
			travelMode = "bike";	break;						
		case "8" : 
			travelMode = "walk";	break;
		default : throw new RuntimeException("Travel mode input code "+travelModeCode+" is not recognized. Aborting ...");
		}
		return travelMode;
	}

	public static int getIncomeInterval( final String monthlyIncome){
		
		int inc = Integer.valueOf(monthlyIncome);

		if( inc <= 500) return 1;
		else if (inc <= 1000) return 2;
		else if (inc <= 3000) return 3;
		else if (inc <= 5000) return 4;
		else if (inc <= 7500) return 5;
		else if (inc <= 1000) return 6;
		else  return 7;
	}
	
	public static int getDailyExpenditureInterval( final String dailyExpenditure){
		
		int cost = Integer.valueOf(dailyExpenditure);
		
		if( cost <= 10) return 1;
		else if (cost <= 25) return 2;
		else if (cost <= 50) return 3;
		else if (cost <= 100) return 4;
		else  return 5;
	}
}


