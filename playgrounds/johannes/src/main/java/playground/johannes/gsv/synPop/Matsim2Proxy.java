/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.synpop.data.Element;
import playground.johannes.synpop.data.Episode;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class Matsim2Proxy {

	private static final Logger logger = Logger.getLogger(Matsim2Proxy.class);
	
	public static void main(String args[]) throws IOException {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		logger.info("Loading matsim population...");
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(args[0]);
		logger.info(String.format("Loaded %s matsim persons.", scenario.getPopulation().getPersons().size()));
		
		logger.info("Loading proxy persons...");
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		parser.parse(args[1]);
		logger.info(String.format("Loaded %s proxy persons.", parser.getPersons().size()));
		
		Map<Id<Person>, ? extends Person> matsimPersons = scenario.getPopulation().getPersons();
		Map<String, ProxyPerson> proxyPresons = new HashMap<>(matsimPersons.size());
		
		for(ProxyPerson person : parser.getPersons()) {
			proxyPresons.put(person.getId(), person);
		}
		
		Set<ProxyPerson> newProxyPersons = new HashSet<>(matsimPersons.size());
		
		int cntReplan = 0;
		
		logger.info("Converting persons...");
		for(Person matsimPerson : matsimPersons.values()) {
			ProxyPerson proxyPerson = proxyPresons.get(matsimPerson.getId().toString());
			newProxyPersons.add(proxyPerson);
			
			Episode proxyPlan = proxyPerson.getPlans().get(0);
			
			if (matsimPerson.getPlans().size() > 1) {
				matsimPerson.removePlan(matsimPerson.getSelectedPlan());
				cntReplan++;
			}
			
			Plan matsimPlan = matsimPerson.getSelectedPlan();
			
			for(int i = 0; i < matsimPlan.getPlanElements().size(); i++) {
				if(i % 2 == 0) {
					Activity matsimAct = (Activity) matsimPlan.getPlanElements().get(i);
					Element proxyAct = proxyPlan.getActivities().get(i / 2);
					
					proxyAct.setAttribute(CommonKeys.ACTIVITY_FACILITY, matsimAct.getFacilityId().toString());
					proxyAct.setAttribute(CommonKeys.ACTIVITY_START_TIME, String.valueOf(matsimAct.getStartTime())); // not sure if this is maintained
					proxyAct.setAttribute(CommonKeys.ACTIVITY_END_TIME, String.valueOf(matsimAct.getEndTime()));
				} else {
					Leg matsimLeg = (Leg) matsimPlan.getPlanElements().get(i);
					Element proxyLeg = proxyPlan.getLegs().get((i-1)/2);
					
					proxyLeg.setAttribute(CommonKeys.LEG_ROUTE_DISTANCE, String.valueOf(matsimLeg.getRoute().getDistance())); // not sure if this is maintained
				}
			}
			
		}
		
		logger.info(String.format("Removed selected plan for %s replanned persons.", cntReplan));
		
		logger.info("Writing proxy persons...");
		XMLWriter writer = new XMLWriter();
		writer.write(args[2], newProxyPersons);
		logger.info("Done.");
	}
}
