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

package playground.boescpa.lib.tools.networkModification;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.*;
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
 * Connects a given set of facilities to a given network.
 *
 * @author boescpa
 */
public class FacilityToNetworkConnector {

	/**
	 * Two parameters are expected via args: a config to a scenario and an output path for the f2l file.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 2 || args.length > 2) {
			System.out.println("Wrong number of arguments. Will abort.");
			return;
		}

		// Load scenario:
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));
		new FacilitiesReaderMatsimV1(scenario).parse(scenario.getConfig().facilities().getInputFile());
		new MatsimNetworkReader(scenario).parse(scenario.getConfig().network().getInputFile());

		// Link facilities to network:
		Map<String, String> f2l = getF2L(scenario.getNetwork(), scenario.getActivityFacilities());
		writeF2L(f2l, args[1]);
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

	protected static Map<String, String> getF2L(Network network, ActivityFacilities activityFacilities) {
		Map<String, String> f2l = new HashMap<>();
		for (ActivityFacility facility : activityFacilities.getFacilities().values()) {
			Link nearestLink = org.matsim.core.network.NetworkUtils.getNearestRightEntryLink(network, facility.getCoord());
			f2l.put(facility.getId().toString(), nearestLink.getId().toString());
		}
		return f2l;
	}

}
