/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioParsing.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.balmermi;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.matsim.basic.v01.Id;
import org.matsim.counts.Counts;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.geometry.CoordImpl;

public class CountsFilter {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void dilZhFilter() {

		System.out.println("running dilZhFilter... " + (new Date()));

		Scenario.setUpScenarioConfig();
//		World world = Scenario.readWorld();
//		Facilities facilities = Scenario.readFacilities();
		NetworkLayer network = Scenario.readNetwork();
		Counts counts = Scenario.readCounts();
//		Matrices matrices = Scenario.readMatrices();
//		Plans plans = Scenario.readPlans();

		//////////////////////////////////////////////////////////////////////

		double radius = 30000.0;
		final CoordImpl center = new CoordImpl(683518.0,246836.0);
		final Map<Id, Link> areaOfInterest = new HashMap<Id, Link>();
		System.out.println("  => area of interest (aoi): center=" + center + "; radius=" + radius);

		System.out.println("  extracting links of the aoi... " + (new Date()));
		for (Link link : network.getLinks().values()) {
			final Node from = link.getFromNode();
			final Node to = link.getToNode();
			if ((from.getCoord().calcDistance(center) <= radius) || (to.getCoord().calcDistance(center) <= radius)) {
				System.out.println("    link " + link.getId().toString());
				areaOfInterest.put(link.getId(),link);
			}
		}
		System.out.println("  done. " + (new Date()));
		System.out.println("  => aoi contains: " + areaOfInterest.size() + " links.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  filtering counts... " + (new Date()));
		TreeSet<Id> counts2remove = new TreeSet<Id>();
		for (Id locid : counts.getCounts().keySet()) {
			if (!areaOfInterest.containsKey(locid)) { counts2remove.add(locid); }
		}
		System.out.println("    # counts           : " + counts.getCounts().size());
		System.out.println("    # counts to remove : " + counts2remove.size());
		for (Id locid: counts2remove) { counts.getCounts().remove(locid); }
		System.out.println("    # remaining counts : " + counts.getCounts().size());
		System.out.println("  done. " + (new Date()));

		//////////////////////////////////////////////////////////////////////

//		Scenario.writePlans(plans);
//		Scenario.writeMatrices(matrices);
		Scenario.writeCounts(counts);
		Scenario.writeNetwork(network);
//		Scenario.writeFacilities(facilities);
//		Scenario.writeWorld(world);
		Scenario.writeConfig();

		System.out.println("done. " + (new Date()));
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {
		dilZhFilter();
	}
}
