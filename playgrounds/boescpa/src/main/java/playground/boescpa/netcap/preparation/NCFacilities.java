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

package playground.boescpa.netcap.preparation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesWriter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * If there are no intentions to use location choice this file reduces all facilities to the only used ones.
 * For the creation of the f2l, the network should only contain the car-links...
 *
 * @author boescpa
 */
public class NCFacilities {

	public static void main(String[] args) {
		if (args.length < 2 || args.length > 3) {
			System.out.println("Wrong number of arguments. Will abort.");
			return;
		}

		// Load scenario:
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));
		new MatsimPopulationReader(scenario).readFile(scenario.getConfig().plans().getInputFile());
		new FacilitiesReaderMatsimV1(scenario).parse(scenario.getConfig().facilities().getInputFile());
		new MatsimNetworkReader(scenario).parse(scenario.getConfig().network().getInputFile());

		// Used facilities:
		ActivityFacilities usedActivityFacilities = getUsedFacilities(scenario.getPopulation(), scenario.getActivityFacilities());
		new FacilitiesWriter(usedActivityFacilities).writeV1(args[1]);

		// Link facilities to network:
		if (args.length > 2) {
			Map<String, String> f2l = getF2L(scenario, usedActivityFacilities);
			writeF2L(f2l, args[2]);
		}
	}

	private static void writeF2L(Map<String, String> f2l, String path2File) {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(path2File));
			bw.write("fid\tlid\n");
			for (String fid : f2l.keySet()) {
				bw.write(fid + "\t" + f2l.get(fid) + "\n");
			}
		} catch (IOException e) {
			throw new RuntimeException("Error while writing given outputF2LFile.", e);
		} finally {
			if (bw != null) {
				try { bw.close(); }
				catch (IOException e) { System.out.print("Could not close stream."); }
			}
		}
	}

	protected static Map<String, String> getF2L(Scenario scenario, ActivityFacilities usedActivityFacilities) {
		Map<String, String> f2l = new HashMap<>();
		for (ActivityFacility facility : usedActivityFacilities.getFacilities().values()) {
			Link nearestLink = NetworkUtils.getNearestRightEntryLink(scenario.getNetwork(), facility.getCoord());
			f2l.put(facility.getId().toString(), nearestLink.getId().toString());
		}
		return f2l;
	}

	private static ActivityFacilities getUsedFacilities(Population population, ActivityFacilities activityFacilities) {
		final ActivityFacilities usedActFacilities = new ActivityFacilitiesImpl(activityFacilities.getName());

		for (Person person : population.getPersons().values()) {
			for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					if (!usedActFacilities.getFacilities().containsKey(activity.getFacilityId())) {
						usedActFacilities.addActivityFacility(activityFacilities.getFacilities().get(activity.getFacilityId()));
					}
				}
			}
		}

		return usedActFacilities;
	}
}
