/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.ppSim.jdepSim.zurich;
public class ParkingScenario {

	
	public static void scenario1pml(){
		ParkingLoader.parkingsOutsideZHCityScaling = 1.0;
		ParkingLoader.streetParkingCalibrationFactor = 0.1;
		ParkingLoader.garageParkingCalibrationFactor = 0.11;
		ParkingLoader.privateParkingCalibrationFactorZHCity = 0.1;
		ParkingLoader.populationScalingFactor = 0.01;
		ZHScenarioGlobal.plansFile= "c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/1pml_plans_30km.xml.gz";
	}
	
	public static void scenario1pct(){
		ParkingLoader.parkingsOutsideZHCityScaling = 1.0;
		ParkingLoader.streetParkingCalibrationFactor = 0.01;
		ParkingLoader.garageParkingCalibrationFactor = 0.011;
		ParkingLoader.privateParkingCalibrationFactorZHCity = 0.01;
		ParkingLoader.populationScalingFactor = 0.01;
		ZHScenarioGlobal.plansFile= "c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/1pct_plans_30km.xml.gz";
	}
	
	public static void scenario10pct(){
		ParkingLoader.parkingsOutsideZHCityScaling = 1.0;
		ParkingLoader.streetParkingCalibrationFactor = 0.15;
		ParkingLoader.garageParkingCalibrationFactor = 0.15;
		ParkingLoader.privateParkingCalibrationFactorZHCity = 0.15;
		ParkingLoader.populationScalingFactor = 0.1;
		ZHScenarioGlobal.plansFile= "c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/10pct_plans_30km.xml.gz";
	}

	public static void scenario100pct(){
		ParkingLoader.parkingsOutsideZHCityScaling = 1.0;
		ParkingLoader.streetParkingCalibrationFactor = 0.1;
		ParkingLoader.garageParkingCalibrationFactor = 0.11;
		ParkingLoader.privateParkingCalibrationFactorZHCity = 0.1;
		ParkingLoader.populationScalingFactor = 1.0;
		ZHScenarioGlobal.populationExpensionFactor = 10;
		ZHScenarioGlobal.plansFile= "c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/10pct_plans_30km.xml.gz";
	}
	
}

