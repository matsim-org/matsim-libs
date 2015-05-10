/* *********************************************************************** *
 * project: org.matsim.*

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
package org.matsim.contrib.transEnergySim.example.car;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.transEnergySim.controllers.AddHandlerAtStartupControler;
import org.matsim.contrib.transEnergySim.controllers.EventHandlerGroup;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionTracker;
import org.matsim.core.config.Config;

import java.util.HashMap;

public class DisChargingControler extends AddHandlerAtStartupControler {

	
	private HashMap<Id<Vehicle>, Vehicle> vehicles;
	private EnergyConsumptionTracker energyConsumptionTracker;

	/**
	 * @author jbischoff
	 * Sample DisChargingControler: Tracks only energy consumption of each agent at each link and dumps it to a file
	 * 
	 */

	
	public DisChargingControler(Config config,  HashMap<Id<Vehicle>, Vehicle> vehicles) {
		super(config);
		init(vehicles);
		
	}

	public DisChargingControler(String[] args,  HashMap<Id<Vehicle>, Vehicle> vehicles) {
		super(args);
		init(vehicles);
	}
	
		

	private void init(HashMap<Id<Vehicle>, Vehicle> vehicles2) {
		this.vehicles = vehicles2;
		EventHandlerGroup handlerGroup = new EventHandlerGroup();
        setEnergyConsumptionTracker(new EnergyConsumptionTracker(vehicles, getScenario().getNetwork()));
		handlerGroup.addHandler(getEnergyConsumptionTracker());
		addHandler(handlerGroup);		
	}
	
	

	public void printStatisticsToConsole() {
		System.out.println("energy consumption stats");
		energyConsumptionTracker.getLog().printToConsole();
		System.out.println("===");

	}

	public void writeStatisticsToFile(String filename) {
		System.out.println("Writing energy consumption stats to" +filename);
		energyConsumptionTracker.getLog().writeToFile(filename);
		System.out.println("Done writing");

	}


	
	public EnergyConsumptionTracker getEnergyConsumptionTracker() {
		return energyConsumptionTracker;
	}

	private void setEnergyConsumptionTracker(EnergyConsumptionTracker energyConsumptionTracker) {
		this.energyConsumptionTracker = energyConsumptionTracker;
	}
	
}
