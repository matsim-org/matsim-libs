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

package playground.polettif.boescpa.lib.tools.fileCreation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Links facilities to links...
 *
 * @author boescpa
 */
public class F2LCreator {

	public static void main(String[] args) {
		if (args.length < 2 || args.length > 2) {
			System.out.println("Wrong number of arguments. Will abort.");
			return;
		}

		// Load scenario:
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));
		new FacilitiesReaderMatsimV1(scenario).parse(scenario.getConfig().facilities().getInputFile());
		new MatsimNetworkReader(scenario.getNetwork()).parse(scenario.getConfig().network().getInputFile());

		// Link facilities to network:
		createF2L(scenario, args[1]);
	}

	/**
	 * Links facilities to a network (the network should only contain the car-links).
	 *
	 * @param scenario	A MATSim scenario that at least contains ActivityFacilities and Network.
	 * @param path2File	Path (incl. filename) to where the f2l-file will be written.
	 */
	public static void createF2L(Scenario scenario, String path2File) {
		createF2L(scenario.getActivityFacilities(), scenario.getNetwork(), path2File);
	}

	/**
	 * Links facilities to a network (the network should only contain the car-links).
	 *
	 * @param facilities Facilities which will be linked.
	 * @param network	To which facilities will be linked.
	 * @param path2File	Path (incl. filename) to where the f2l-file will be written.
	 */
	public static void createF2L(ActivityFacilities facilities, Network network, String path2File) {
		writeF2L(getF2L(facilities, network), path2File);
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

	protected static Map<String, String> getF2L(ActivityFacilities facilities, Network network) {
		Map<String, String> f2l = new HashMap<>();
		for (ActivityFacility facility : facilities.getFacilities().values()) {
			Link nearestLink = NetworkUtils.getNearestRightEntryLink(network, facility.getCoord());
			f2l.put(facility.getId().toString(), nearestLink.getId().toString());
		}
		return f2l;
	}
}
