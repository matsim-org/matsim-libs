/* *********************************************************************** *
 * project: org.matsim.*
 * Create2kwScenario.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.energyflows;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.facilities.algorithms.WorldConnectLocations;

import playground.christoph.energyflows.facilities.AnalyzeRemovedFacilities;
import playground.christoph.energyflows.facilities.FacilitiesToRemoveFromZH;
import playground.christoph.energyflows.facilities.RemoveFacilitiesFromZH;
import playground.christoph.energyflows.facilities.BuildingsZHCreator;
import playground.christoph.energyflows.population.GetPersonsToAdapt;

public class Create2kwScenario {

	final private static Logger log = Logger.getLogger(Create2kwScenario.class);

	private String networkFile = "../../matsim/mysimulations/2kw/network/network.xml.gz";
	
	private String populationInFile = "../../matsim/mysimulations/2kw/population/plans.xml.gz";
	private String populationOutFile = "../../matsim/mysimulations/2kw/population/plans_adapted.xml.gz";
	
	private String facilitiesF2lFile = "../../matsim/mysimulations/2kw/facilities/f2l.txt";
	private String facilitiesInFile = "../../matsim/mysimulations/2kw/facilities/facilities.xml.gz";
	private String facilitiesOutFile = "../../matsim/mysimulations/2kw/facilities/facilities_adapted.xml.gz";
	
	public static void main(String[] args) throws Exception {
		new Create2kwScenario();
	}
	
	public Create2kwScenario() throws Exception {
		
		Config config = ConfigUtils.createConfig();
		config.setParam("f2l", "inputF2LFile", facilitiesF2lFile);
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesInFile);
//		config.plans().setInputFile(populationInFile);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		Gbl.printMemoryUsage();
//		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		
//		log.info("read network...");
//		new MatsimNetworkReader(scenario).parse(networkFile);
//		log.info("...done.");
//		Gbl.printMemoryUsage();
		
//		log.info("read facilities...");
//		new MatsimFacilitiesReader(scenario).parse(facilitiesInFile);
//		log.info("read " + scenario.getActivityFacilities().getFacilities().size() + " facilities");
//		log.info("...done.");
//		Gbl.printMemoryUsage();
		
		log.info("connecting facilities to network...");
		new WorldConnectLocations(config).connectFacilitiesWithLinks(scenario.getActivityFacilities(), scenario.getNetwork());
		log.info("...done.");
		
//		log.info("read population...");
//		new MatsimPopulationReader(scenario).parse(populationInFile);
//		log.info("...done.");
//		Gbl.printMemoryUsage();
		
		log.info("read facilities to remove...");
		Set<Id> facilitiesToRemove = new FacilitiesToRemoveFromZH().getFacilitiesToRemove();
		log.info("...done.");
				
//		log.info("identify people who have to adapt their plans...");
//		Set<Id> persons = new GetPersonsToAdapt().getPersons(scenario.getPopulation(), facilitiesToRemove);
//		log.info("found " + persons.size() + " agents that have to adapt their plan");
//		log.info("...done.");
		
		log.info("remove facilities...");
		List<ActivityFacility> removedFacilities = new RemoveFacilitiesFromZH().removeFacilities(scenario.getActivityFacilities(), facilitiesToRemove);
		log.info("...done.");
		
		log.info("analyze facilities to remove...");
		AnalyzeRemovedFacilities analyzeRemovedFacilities = new AnalyzeRemovedFacilities(removedFacilities);
		log.info("...done.");
		
		log.info("create residential facilites...");
		BuildingsZHCreator residentialCreator = new BuildingsZHCreator();
//		residentialCreator.connectToNetwork(scenario.getNetwork(), analyzeRemovedFacilities.getHostedFacilities());
//		residentialCreator.calcCapacities(analyzeRemovedFacilities.getRemovedCapacities());
		log.info("...done.");
		
//		log.info("write facilities...");
//		new FacilitiesWriter(scenario.getActivityFacilities()).write(facilitiesOutFile);
//		log.info("wrote " + scenario.getActivityFacilities().getFacilities().size() + " facilities");
//		log.info("...done.");
		
//		log.info("write population...");
//		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFileV5(populationOutFile);
//		log.info("...done.");
	}
}
