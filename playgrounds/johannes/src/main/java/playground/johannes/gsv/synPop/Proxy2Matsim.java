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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.sna.util.ProgressLogger;

/**
 * @author johannes
 *
 */
public class Proxy2Matsim {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		Population pop = scenario.getPopulation();
		PopulationFactory factory = pop.getFactory();

		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1(scenario);
		facReader.readFile(args[1]);
		ActivityFacilities facilities = scenario.getActivityFacilities();
		
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		parser.parse(args[0]);
		
		ProgressLogger.init(parser.getPersons().size(), 1, 10);
		
		for(ProxyPerson proxyPerson : parser.getPersons()) {
			Person person = factory.createPerson(Id.create(proxyPerson.getId(), Person.class));
			pop.addPerson(person);
			
			ProxyPlan proxyPlan = proxyPerson.getPlan();
			Plan plan = factory.createPlan();
			person.addPlan(plan);
			
			for(int i = 0; i < proxyPlan.getActivities().size(); i++) {
				ProxyObject proxyAct = proxyPlan.getActivities().get(i);
				ActivityImpl act = null;
				
				String type = proxyAct.getAttribute(CommonKeys.ACTIVITY_TYPE);
//				if(type.equalsIgnoreCase("home")) {
//					double x = Double.parseDouble(proxyPerson.getAttribute(CommonKeys.PERSON_HOME_COORD_X));
//					double y = Double.parseDouble(proxyPerson.getAttribute(CommonKeys.PERSON_HOME_COORD_Y));
//					act = (ActivityImpl) factory.createActivityFromCoord(type, new CoordImpl(x, y));
//					
//				} else {
//					ActivityFacility facility = facilities.getFacilities().get(Id.create(proxyAct.getAttribute(CommonKeys.ACTIVITY_FACILITY)));
//					act = factory.createActivityFromCoord(type, facility.getCoord());
//				}
				ActivityFacility facility = facilities.getFacilities().get(Id.create(proxyAct.getAttribute(CommonKeys.ACTIVITY_FACILITY), ActivityFacility.class));
				act = (ActivityImpl) factory.createActivityFromCoord(type, facility.getCoord());
				act.setFacilityId(facility.getId());
				act.setStartTime(Integer.parseInt(proxyAct.getAttribute(CommonKeys.ACTIVITY_START_TIME)));
				act.setEndTime(Integer.parseInt(proxyAct.getAttribute(CommonKeys.ACTIVITY_END_TIME)));
				plan.addActivity(act);
				
				if(i < proxyPlan.getLegs().size()) {
					ProxyObject proxyLeg = proxyPlan.getLegs().get(i);
					String mode = proxyLeg.getAttribute(CommonKeys.LEG_MODE);
					if(mode == null) {
						mode = "undefined";
					}
					Leg leg = factory.createLeg(mode);
					plan.addLeg(leg);
				}
			}
			
			ProgressLogger.step();
		}
		
		PopulationWriter writer = new PopulationWriter(pop);
		writer.write(args[2]);
	}

}
