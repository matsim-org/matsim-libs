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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

/**
 * @author johannes
 *
 */
public class Matsim2Proxy {

	public static void main(String args[]) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(args[0]);
		
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		parser.parse(args[1]);
		
		Map<Id<Person>, ? extends Person> matsimPersons = scenario.getPopulation().getPersons();
		Map<String, ProxyPerson> proxyPresons = new HashMap<>(matsimPersons.size());
		
		for(ProxyPerson person : parser.getPersons()) {
			proxyPresons.put(person.getId(), person);
		}
		
		Set<ProxyPerson> newProxyPersons = new HashSet<>(matsimPersons.size());
		
		for(Person matsimPerson : matsimPersons.values()) {
			ProxyPerson proxyPerson = proxyPresons.get(matsimPerson.getId().toString());
			newProxyPersons.add(proxyPerson);
			
			ProxyPlan proxyPlan = proxyPerson.getPlans().get(0);
			Plan matsimPlan = matsimPerson.getSelectedPlan();
			for(int i = 0; i < matsimPlan.getPlanElements().size(); i++) {
				if(i % 2 == 0) {
					Activity matsimAct = (Activity) matsimPlan.getPlanElements().get(i);
					ProxyObject proxyAct = proxyPlan.getActivities().get(i / 2);
					
					proxyAct.setAttribute(CommonKeys.ACTIVITY_FACILITY, matsimAct.getFacilityId().toString());
					proxyAct.setAttribute(CommonKeys.ACTIVITY_START_TIME, String.valueOf(matsimAct.getStartTime())); // not sure if this is maintained
					proxyAct.setAttribute(CommonKeys.ACTIVITY_END_TIME, String.valueOf(matsimAct.getEndTime()));
				} else {
					Leg matsimLeg = (Leg) matsimPlan.getPlanElements().get(i);
					ProxyObject proxyLeg = proxyPlan.getLegs().get((i-1)/2);
					
					proxyLeg.setAttribute(CommonKeys.LEG_ROUTE_DISTANCE, String.valueOf(matsimLeg.getRoute().getDistance())); // not sure if this is maintained
				}
			}
			
		}
		
		XMLWriter writer = new XMLWriter();
		writer.write(args[2], newProxyPersons);
	}
}
