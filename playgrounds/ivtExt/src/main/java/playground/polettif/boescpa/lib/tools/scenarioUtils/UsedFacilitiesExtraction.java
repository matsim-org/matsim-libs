/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.polettif.boescpa.lib.tools.scenarioUtils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;
import playground.polettif.boescpa.lib.tools.fileCreation.F2LCreator;

/**
 * If there are no intentions to use location choice this file reduces all facilities to the only used ones.
 * For the creation of the f2l, the network should only contain the car-links...
 *
 * @author boescpa
 */
public class UsedFacilitiesExtraction {

	public static void main(String[] args) {
		if (args.length < 2 || args.length > 3) {
			System.out.println("Wrong number of arguments. Will abort.");
			return;
		}

		// Load scenario:
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));
		new MatsimPopulationReader(scenario).readFile(scenario.getConfig().plans().getInputFile());
		new FacilitiesReaderMatsimV1(scenario).parse(scenario.getConfig().facilities().getInputFile());
		new MatsimNetworkReader(scenario.getNetwork()).parse(scenario.getConfig().network().getInputFile());

		// Used facilities:
		ActivityFacilities usedActivityFacilities = getUsedFacilities(scenario.getPopulation(), scenario.getActivityFacilities());
		new FacilitiesWriter(usedActivityFacilities).writeV1(args[1]);

		// Link facilities to network:
		if (args.length > 2) {
			F2LCreator.createF2L(usedActivityFacilities, scenario.getNetwork(), args[2]);
		}
	}

	private static ActivityFacilities getUsedFacilities(Population population, ActivityFacilities activityFacilities) {
		final ActivityFacilities usedActFacilities = new ActivityFacilitiesImpl(activityFacilities.getName());

		for (Person person : population.getPersons().values()) {
			for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					if (!usedActFacilities.getFacilities().containsKey(activity.getFacilityId())) {
						ActivityFacility usedFacility = activityFacilities.getFacilities().get(activity.getFacilityId());
						if (usedFacility != null) {
							usedActFacilities.addActivityFacility(activityFacilities.getFacilities().get(activity.getFacilityId()));
						}
					}
				}
			}
		}

		return usedActFacilities;
	}
}
