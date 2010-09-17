/* *********************************************************************** *
 * project: org.matsim.*
 * CleanNetwork.java
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

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkAdaptLength;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkWriteAsTable;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;

import playground.balmermi.modules.NetworkDoubleLinks;

public class CleanNetwork {

	//////////////////////////////////////////////////////////////////////
	// cleanNetwork
	//////////////////////////////////////////////////////////////////////

	public static void cleanNetwork(final String[] args) {

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(args[0]);
		
		NetworkImpl subNetwork = new ScenarioImpl().getNetwork();
		Set<String> modes = new HashSet<String>();
		modes.add(TransportMode.car);
		new TransportModeNetworkFilter(network).filter(subNetwork,modes);
		new NetworkAdaptLength().run(subNetwork);
		new NetworkCleaner().run(subNetwork);
		
		for (Link l : network.getLinks().values()) {
			LinkImpl l2 = (LinkImpl)subNetwork.getLinks().get(l.getId());
			if (l2 != null) {
				l2.setOrigId(((LinkImpl) l).getOrigId());
				l2.setType(((LinkImpl) l).getType());
			}
		}
		new NetworkDoubleLinks("-dl").run(subNetwork);

		NetworkWriteAsTable nwat = new NetworkWriteAsTable(args[1]);
		nwat.run(network);

		new NetworkWriter(network).write(args[1]+"/network.xml.gz");
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {
		Gbl.startMeasurement();
		Gbl.printElapsedTime();

		cleanNetwork(args);

		Gbl.printElapsedTime();
	}
}
