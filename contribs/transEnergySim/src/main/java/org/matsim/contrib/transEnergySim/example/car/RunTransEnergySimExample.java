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


import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.ricardoFaria2012.EnergyConsumptionModelRicardoFaria2012;
import org.matsim.contrib.transEnergySim.vehicles.impl.BatteryElectricVehicleImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * 
 */
/**
 * @author jbischoff
 * Sample main file for setting up an Electric Vehicle simulation
 * to run this, you will need:
 * a config, network and a population
 * 
 */


public class RunTransEnergySimExample {
	
	private Scenario sc;
	
	private static final Logger log = Logger.getLogger(RunTransEnergySimExample.class);
	private static final String CONFIG = "test/input/org/matsim/contrib/transEnergySim/controllers/InductiveChargingControllerIT/config.xml";
	private static  String outputDir =  "output/"; // Simulation output 
	private static  String estats ;
	
	private DisChargingControler c;

	public void run(){
		c.run();
		c.writeStatisticsToFile(estats);
		
	}
	public static void main(String[] args){
		if (args.length==1){
			// use the given output if args is not null
			outputDir = args[0];
		}
		estats = outputDir + "/estats.txt";
		RunTransEnergySimExample runner = new RunTransEnergySimExample();
		Config config = ConfigUtils.loadConfig(CONFIG);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(outputDir);
		runner.setUpControler(config);
		runner.run();
	}






	private void setUpControler(Config config) {
		log.info("Setting up emob controler");
		int batteryCapacityInJoules = 25*1000*3600;
		//assuming a 25kwh battery
		EnergyConsumptionModel faria = new EnergyConsumptionModelRicardoFaria2012();
		this.sc = ScenarioUtils.loadScenario(config);
		HashMap<Id<Vehicle>,Vehicle> vehicles = new HashMap<Id<Vehicle>, Vehicle>();
		for (Person p : this.sc.getPopulation().getPersons().values()){
		    Id<Vehicle> vid = Id.create(p.getId(), Vehicle.class);
			vehicles.put(vid, new BatteryElectricVehicleImpl(faria, batteryCapacityInJoules, vid));
			//gives every person of the population an electric car
		}
		
		this.c = new DisChargingControler(sc.getConfig(), vehicles);

	
		
	}
	
	
}
