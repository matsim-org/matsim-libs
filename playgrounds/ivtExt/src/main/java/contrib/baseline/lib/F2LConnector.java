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

package contrib.baseline.lib;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;

/**
 * Connects facilities to links...
 *
 * @author boescpa
 */
public class F2LConnector {

	public static void main(String[] args) {
		if (args.length < 2 || args.length > 2) {
			System.out.println("Wrong number of arguments. Will abort.");
			return;
		}

		// Load scenario:
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));
		new FacilitiesReaderMatsimV1(scenario).readFile(scenario.getConfig().facilities().getInputFile());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(scenario.getConfig().network().getInputFile());

		// Link facilities to network:
		connectFacilitiesToLinks(scenario, args[1]);
	}

	/**
	 * Connects facilities to a network (the network should only contain the car-links).
	 *
	 * @param scenario	A MATSim scenario that at least contains ActivityFacilities and Network.
	 * @param path2File	Path (incl. filename) to where the connected facilities file will be written. If null, no file will be written.
	 */
	public static void connectFacilitiesToLinks(Scenario scenario, String path2File) {
		connectFacilitiesToLinks(scenario.getActivityFacilities(), scenario.getNetwork(), path2File);
	}

	/**
	 * Connects facilities to a network (the network should only contain the car-links).
	 *
	 * @param facilities Facilities which will be linked.
	 * @param network	To which facilities will be linked.
	 * @param path2File	Path (incl. filename) to where the connected facilities will be written. If null, no file will be written.
	 */
	public static void connectFacilitiesToLinks(ActivityFacilities facilities, Network network, String path2File) {
		linkF2L(facilities, network);
		if (path2File != null) {
			new FacilitiesWriter(facilities).write(path2File);
		}
	}

	private static void linkF2L(ActivityFacilities facilities, Network network) {
		for (ActivityFacility facility : facilities.getFacilities().values()) {
			Link nearestLink = NetworkUtils.getNearestRightEntryLink(network, facility.getCoord());
			((ActivityFacilityImpl)facility).setLinkId(nearestLink.getId());
		}
	}
}
