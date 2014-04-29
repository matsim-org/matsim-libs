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
import org.matsim.core.config.ConfigUtils;
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


public class ElectricCarMain {
	
	private Scenario sc;
	
	private static final Logger log = Logger.getLogger(ElectricCarMain.class);
	private static final String CONFIG = "path to your config here";
	private static final String ESTATS =  "output/estats.txt"; //fairly simple statistics about energy consumption on each link
	private DisChargingControler c;
	
		
	
	
	public void run(){
		c.setOverwriteFiles(true);
		c.run();
		c.writeStatisticsToFile(ESTATS);
		
	}
	public static void main(String[] args){
		ElectricCarMain runner = new ElectricCarMain();
		runner.setUpControler(CONFIG);
		runner.run();
	}






	private void setUpControler(String configFile) {
		log.info("Setting up emob controler");
		int batteryCapacityInJoules = 25*1000*3600;
		EnergyConsumptionModel faria = new EnergyConsumptionModelRicardoFaria2012();
		this.sc = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
		HashMap<Id,Vehicle> vehicles = new HashMap<Id, Vehicle>();
		for (Person p : this.sc.getPopulation().getPersons().values()){
			vehicles.put(p.getId(), new BatteryElectricVehicleImpl(faria, batteryCapacityInJoules));
			//gives every person of the population an electric car
		}
		
		this.c = new DisChargingControler(sc.getConfig(), vehicles);

	
		
	}
	
	
}
