/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityHomeTest.java
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
package playground.johannes.studies.locChoice;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author illenberger
 *
 */
public class FacilityHomeTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = new Config();
		config.addCoreModules();

		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.readFile(args[0]);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(config.getParam("network", "inputNetworkFile"));
		
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(config.getParam("plans", "inputPlansFile"));
		
		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile(config.getParam("facilities", "inputFacilitiesFile"));
		
		Network network = scenario.getNetwork();
		Population pop = scenario.getPopulation();
		ActivityFacilities facilities = scenario.getActivityFacilities();

		List<Id> linkIds = new ArrayList<Id>();
		
		for(ActivityFacility facility : facilities.getFacilities().values()) {
			boolean isLeisure = false;
			for(ActivityOption option : facility.getActivityOptions().values()) {
				if(option.getType().equalsIgnoreCase("leisure")) {
					isLeisure = true;
					break;
				}
			}
			
			if(isLeisure) {
				if(facility.getLinkId() != null)
					linkIds.add(facility.getLinkId());
				else {
					LinkImpl link = ((NetworkImpl)network).getNearestLink(facility.getCoord());
					if(link != null)
						linkIds.add(link.getId());
					else
						throw new RuntimeException("Unable to obtain link.");
						
				}
			}
		}
		
		int notfound = 0;
		for(Person person : pop.getPersons().values()) {
			Id home = ((Activity) person.getSelectedPlan().getPlanElements().get(0)).getLinkId();
			if(!linkIds.contains(home))
				notfound++;
		}
		
		System.out.println("No l-Facilities not on home link: " + notfound);
		System.out.println("No of leisure links: " + linkIds.size());
		System.out.println("No of total links: " + network.getLinks().size());
	}

}
