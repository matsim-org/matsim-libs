/* *********************************************************************** *
 * project: org.matsim.*
 * CreateNetworkCutIVTCH.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.dissertation;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

public class CreateNetworkCutIVTCH {

	public static void main(String[] args) {		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("/data/matsim/cdobler/Dissertation/InitialRoutes/input_Zurich_IVTCH/network.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);

		reduceNetwork(scenario.getNetwork(), new Coord(683518.0, 246836.0), 30000.0);
		
		new NetworkWriter(scenario.getNetwork()).write("/data/matsim/cdobler/Dissertation/InitialRoutes/input_Zurich_IVTCH/network_cut.xml.gz");
	}
	
	private static void reduceNetwork(Network network, Coord center, double radius) {
		System.out.println("removing links outside of circle ("+center.toString()+";"+radius+""+")... " + (new Date()));
		Set<Id> toRemove = new HashSet<Id>();
		for (Link l : network.getLinks().values()) {
			Coord fc = l.getFromNode().getCoord();
			Coord tc = l.getToNode().getCoord();
			if (CoordUtils.calcDistance(fc, center) > radius) { toRemove.add(l.getId()); }
			else if (CoordUtils.calcDistance(tc, center) > radius) { toRemove.add(l.getId()); }
		}
		System.out.println("=> "+toRemove.size()+" links to remove.");
		for (Id id : toRemove) { network.removeLink(id); }
		System.out.println("=> "+network.getLinks().size()+" links left.");
		System.out.println("done. " + (new Date()));

		System.out.println("cleaning network... " + (new Date()));
		new NetworkCleaner().run(network);
		System.out.println("done. " + (new Date()));
	}
}
