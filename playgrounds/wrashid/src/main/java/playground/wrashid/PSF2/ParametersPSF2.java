/* *********************************************************************** *
 * project: org.matsim.*
 * ParametersPSF2.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.PSF2;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.core.controler.MatsimServices;
import playground.wrashid.PSF.energy.charging.ChargingTimes;
import playground.wrashid.PSF2.chargingSchemes.ActivityIntervalTracker_NonParallelizableHandler;
import playground.wrashid.PSF2.vehicle.energyConsumption.EnergyConsumptionTable;
import playground.wrashid.PSF2.vehicle.energyStateMaintainance.EnergyStateMaintainer;
import playground.wrashid.PSF2.vehicle.vehicleFleet.FleetInitializer;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.obj.GeneralLogObject;

import java.util.HashMap;
import java.util.LinkedList;

public class ParametersPSF2 {

	public static String pathToEnergyConsumptionTable=null;
	
	private static LinkedList<String> allowedChargingLocations=null; 
	
	public static FleetInitializer fleetInitializer;
	//key: personId, value: vehicle
	public static LinkedListValueHashMap<Id<Person>, Vehicle> vehicles;
	
	public static HashMap<Id<Person>, ChargingTimes> chargingTimes;


	public static EnergyConsumptionTable energyConsumptionTable;



	public static EnergyStateMaintainer energyStateMaintainer;



	public static MatsimServices controler;



	public static ActivityIntervalTracker_NonParallelizableHandler activityIntervalTracker;

	private static GeneralLogObject generalLogObject;
	private static GeneralLogObject iterationLogObject;

	public static boolean isEventsFileBasedControler=false;
	
	public static void initVehicleFleet(MatsimServices controler){
        ParametersPSF2.vehicles=ParametersPSF2.fleetInitializer.getVehicles(controler.getScenario().getPopulation().getPersons().keySet(), ParametersPSF2.energyStateMaintainer);
	};
	
	public static void setAllowedChargingLocations(LinkedList<String> allowedChargingLocations){
		ParametersPSF2.allowedChargingLocations=allowedChargingLocations;
	}
	
	public static boolean isChargingPossibleAtActivityLocation(String actType){
		if (chargingAtAllActivityLocationsAllowed()){
			return true;
		}
		
		return allowedChargingLocations.contains(actType);
	}
	
	private static boolean chargingAtAllActivityLocationsAllowed(){
		return allowedChargingLocations==null;
	}

	public static void setPSFGeneralLog(GeneralLogObject generalLog) {
		ParametersPSF2.generalLogObject=generalLog;
	}
	
	public static GeneralLogObject getPSFGeneralLog() {
		return ParametersPSF2.generalLogObject;
	}

	public static GeneralLogObject getPSFIterationLog() {
		return ParametersPSF2.iterationLogObject;
	}
	
	public static void setPSFIterationLog(GeneralLogObject iterationLog) {
		iterationLogObject=iterationLog;
	}
	
	
}
