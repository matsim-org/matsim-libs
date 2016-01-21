/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.osm;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;

import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class ExtractUsedFacilities {

	private static final Logger logger = Logger.getLogger(ExtractUsedFacilities.class);
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		
		logger.info("Loadin population...");
		popReader.readFile(args[0]);
		logger.info("Loading facilities...");
		facReader.readFile(args[1]);
		logger.info(String.format("Loaded %s facilities.", scenario.getActivityFacilities().getFacilities().size()));
		
		ActivityFacilities newFacilities = FacilitiesUtils.createActivityFacilities();
		Set<ActivityFacility> facSet = new HashSet<>();
		
		logger.info("Extracting used facilities...");
		ProgressLogger.init(scenario.getPopulation().getPersons().size(), 2, 10);
		for(Person person : scenario.getPopulation().getPersons().values()) {
			for(Plan plan : person.getPlans()) {
				for(int i = 0; i < plan.getPlanElements().size(); i+=2) {
					Activity act = (Activity) plan.getPlanElements().get(i);
					ActivityFacility fac = scenario.getActivityFacilities().getFacilities().get(act.getFacilityId());
					facSet.add(fac);
				}
			}
			ProgressLogger.step();
		}
		ProgressLogger.terminate();
		
		for(ActivityFacility fac : facSet) newFacilities.addActivityFacility(fac);
		
		logger.info(String.format("Extracted %s facilities.", newFacilities.getFacilities().size()));
		logger.info("Writing facilities...");
		FacilitiesWriter writer = new FacilitiesWriter(newFacilities);
		writer.write(args[2]);
		logger.info("Done.");
	}

}
