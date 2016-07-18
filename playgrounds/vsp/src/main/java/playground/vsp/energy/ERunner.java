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
package playground.vsp.energy;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.energy.trafficstate.TrafficStateControlerListener;


/**
 * @author droeder
 * Class to start an ordinary MATSim-Simulation, using the specified configfile.
 * If an additional plansfile is specified the plans/persons are added with the identifier as prefix to the original population. 
 * These persons are supposed to be drivers of electric vehicles.
 * Furthermore a ControlerListener is added to the MATSim Controler that writes a XML file containing traffic information for each link to file.
 *
 */
public class ERunner {

	private static final String DIR = "D:/VSP/svn/shared/volkswagen_internal/";
//	private static final String DIR = "/home/dgrether/shared-svn/projects/volkswagen_internal/";
	
	private static final String CONFIG = DIR + "scenario/config_empty_scenario.xml";
//	private static final String CONFIG = DIR + "scenario/config_empty_scenario.xml";
	private static final String ADDPLANS = DIR + "scenario/input/testPlans.xml";
	
	//###########
	private Scenario sc;

	public ERunner(){
		
	}
	
	public void loadScenario(String configFile, String identifier, String additionalPlansFile){
		this.sc = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
		
		if(!(additionalPlansFile == null)){
			PopulationFactory f = this.sc.getPopulation().getFactory();
			Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			new MatsimNetworkReader(sc.getNetwork()).readFile(this.sc.getConfig().getParam(NetworkConfigGroup.GROUP_NAME, "inputNetworkFile"));
			new PopulationReader(sc).readFile(additionalPlansFile);
			Person newPerson;
			for(Person p: sc.getPopulation().getPersons().values()){
				newPerson = f.createPerson(Id.create(identifier + p.getId().toString(), Person.class));
				newPerson.addPlan(p.getSelectedPlan());
				this.sc.getPopulation().addPerson(newPerson);
			}
		}
		
	}
	
	public void run(){
		Controler c = new Controler(this.sc);
		c.addOverridingModule(new OTFVisFileWriterModule());
		TrafficStateControlerListener trafficState = new TrafficStateControlerListener();
		c.addControlerListener(trafficState);

		c.getConfig().controler().setDumpDataAtEnd(true);
		c.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		c.run();
	}
	
	public static void main(String[] args){
		ERunner runner = new ERunner();
		
		runner.loadScenario(CONFIG, EPostProcessor.IDENTIFIER, ADDPLANS);
		runner.run();
	}
	

}
