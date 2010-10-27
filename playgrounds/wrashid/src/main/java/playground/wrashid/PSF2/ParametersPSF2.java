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

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;

import playground.wrashid.PSF.energy.charging.ChargingTimes;
import playground.wrashid.PSF2.chargingSchemes.ActivityIntervalTracker_NonParallelizableHandler;
import playground.wrashid.PSF2.vehicle.energyConsumption.EnergyConsumptionTable;
import playground.wrashid.PSF2.vehicle.energyStateMaintainance.EnergyStateMaintainer;
import playground.wrashid.PSF2.vehicle.vehicleFleet.FleetInitializer;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.obj.GeneralLogObject;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class ParametersPSF2 {

	public static String pathToEnergyConsumptionTable="c:\\data\\My Dropbox\\ETH\\Projekte\\IATBR2009\\old\\matsim\\input\\runRW1002\\VehicleEnergyConsumptionRegressionTable.txt";
	
	private static LinkedList<String> allowedChargingLocations=null; 
	
	public static FleetInitializer fleetInitializer;
	public static LinkedListValueHashMap<Id, Vehicle> vehicles;
	
	public static HashMap<Id, ChargingTimes> chargingTimes;



	public static EnergyConsumptionTable energyConsumptionTable;



	public static EnergyStateMaintainer energyStateMaintainer;



	public static Controler controler;



	public static ActivityIntervalTracker_NonParallelizableHandler activityIntervalTracker;

	private static GeneralLogObject generalLogObject;
	private static GeneralLogObject iterationLogObject;
	
	public static void initVehicleFleet(Controler controler){
		ParametersPSF2.vehicles=ParametersPSF2.fleetInitializer.getVehicles(controler.getPopulation().getPersons().keySet(), ParametersPSF2.energyStateMaintainer);
	};
	
	public static void setChargingLocationFilter(LinkedList<String> allowedChargingLocations){
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
