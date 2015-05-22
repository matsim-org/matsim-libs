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
package playground.jbischoff.energy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;
import org.matsim.contrib.transEnergySim.vehicles.impl.BatteryElectricVehicleImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.jbischoff.energy.consumption.EnergyConsumptionModelBerlinHigh;
import playground.jbischoff.energy.consumption.EnergyConsumptionModelBerlinLow;
import playground.jbischoff.energy.consumption.EnergyConsumptionModelBerlinMedium;
import playground.jbischoff.energy.controlers.DisChargingControler;




public class ElectricBerlinMain {
	
	private Scenario sc;
	
	private static final Logger log = Logger.getLogger(ElectricBerlinMain.class);
	private static final String DIR = "\\\\vsp-nas/jbischoff/WinHome/Docs/svn-checkouts/volkswagen_internal/";
//	private static final String DIR = "/home/dgrether/shared-svn/projects/volkswagen_internal/";
	public static final String IDENTIFIER = "emob_";
//	private static final String CONFIG = DIR + "scenario/config_empty_scenario.xml";
	private static final String CONFIG = DIR + "scenario/config_congested_scenario2.xml";
	private static final String ADDPLANS = DIR + "scenario/input/testPlans.xml";
	private static final String ESTATS = DIR + "scenario/output/estats.txt";
	private Map<Id<Person>,String> eagentsWithBehaviour;
	private DisChargingControler c;
	private List<Person> emobagents;
	
	
	
	public void updatePersons(String configFile, String identifier, String additionalPlansFile){
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.loadConfig(configFile));
		this.emobagents = new ArrayList<Person>();
		this.eagentsWithBehaviour = new HashMap<Id<Person>,String>();
		PopulationFactory f = s.getPopulation().getFactory();
		new MatsimNetworkReader(s).readFile(s.getConfig().getParam(NetworkConfigGroup.GROUP_NAME, "inputNetworkFile"));
		new MatsimPopulationReader(s).readFile(additionalPlansFile);
		
		Person newPerson;
		for(Person p: s.getPopulation().getPersons().values()){
			newPerson = f.createPerson(Id.create(identifier + p.getId().toString(), Person.class));
										
			newPerson.addPlan(p.getSelectedPlan());
			
				// funktioniert nur für den aktuellen Testfall und solange Drivingbehaviour je Agent über alle Legs unverändert bleibt:
				if (Integer.parseInt(p.getId().toString())<10){
					this.eagentsWithBehaviour.put(newPerson.getId(), "LOW");
					log.info("Setting db to LOW for "+ newPerson.getId());
				}
				else {this.eagentsWithBehaviour.put(newPerson.getId(), "MEDIUM");
					log.info("Setting db to MEDIUM for "+ newPerson.getId());

				}
				this.emobagents.add(newPerson);
		}
	
		
//		this.sc = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
//		ScenarioUtils.
//		this.emobagents = new ArrayList<Person>();
//		this.eagentsWithBehaviour = new HashMap<Id,String>();
//			PopulationFactory f = this.sc.getPopulation().getFactory();
//			Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//			new MatsimNetworkReader(sc).readFile(this.sc.getConfig().getParam(NetworkConfigGroup.GROUP_NAME, "inputNetworkFile"));
//			new MatsimPopulationReader(sc).readFile(additionalPlansFile);
//			Person newPerson;
//			for(Person p: sc.getPopulation().getPersons().values()){
//				newPerson = f.createPerson(this.sc.createId(identifier + p.getId().toString()));
//											
//				newPerson.addPlan(p.getSelectedPlan());
//				
//					// funktioniert nur für den aktuellen Testfall und solange Drivingbehaviour je Agent über alle Legs unverändert bleibt:
//					if (Integer.parseInt(p.getId().toString())<10){
//						this.eagentsWithBehaviour.put(newPerson.getId(), "LOW");
//						log.info("Setting db to LOW for "+ newPerson.getId());
//					}
//					else {this.eagentsWithBehaviour.put(newPerson.getId(), "MEDIUM");
//						log.info("Setting db to MEDIUM for "+ newPerson.getId());
//
//					}
//					this.emobagents.add(newPerson);
//			}
//		
	}
	
	
	
	public void run(){
		c.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());

		c.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		c.run();
		
		c.writeStatisticsToFile(ESTATS);
		
	}
	public static void main(String[] args){
		ElectricBerlinMain runner = new ElectricBerlinMain();
		runner.updatePersons(CONFIG, IDENTIFIER, ADDPLANS);
		runner.setUpControler(CONFIG);
		runner.run();
	}






	private void setUpControler(String configFile) {
		log.info("Setting up emob controler");
		int batteryCapacityInJoules = 25*1000*3600;
		EnergyConsumptionModel blow = new EnergyConsumptionModelBerlinLow();
		EnergyConsumptionModel bmed = new EnergyConsumptionModelBerlinMedium();
		EnergyConsumptionModel bhigh = new EnergyConsumptionModelBerlinHigh();
		
		HashMap<Id<Vehicle>, Vehicle> vehicles=new HashMap<Id<Vehicle>, Vehicle>();
		
		
		for (Entry<Id<Person>,String> e : this.eagentsWithBehaviour.entrySet()){
		    Id<Vehicle> vid = Id.create(e.getKey(), Vehicle.class);
			if (e.getValue().equals("LOW")){
				
				vehicles.put(vid, new BatteryElectricVehicleImpl(blow,batteryCapacityInJoules,vid));
			} else if (e.getValue().equals("MEDIUM")){
				vehicles.put(vid, new BatteryElectricVehicleImpl(bmed,batteryCapacityInJoules,vid));

			}
			  else if (e.getValue().equals("HIGH")){
				vehicles.put(vid, new BatteryElectricVehicleImpl(bhigh,batteryCapacityInJoules,vid));

			}
			  else {
				  	
				  	log.info("No valid driving behaviour data for agent: "+e.getKey()+"Assuming Medium");
					vehicles.put(vid, new BatteryElectricVehicleImpl(bmed,batteryCapacityInJoules,vid));
					
			  }
		}
		
		
		this.sc = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
		
		this.c = new DisChargingControler(sc.getConfig(), vehicles);

		for (Person p : this.emobagents){
			c.getScenario().getPopulation().addPerson(p);
			log.info("Added emob agent: "+p.getId());
		}
		
	}
	
	
}
