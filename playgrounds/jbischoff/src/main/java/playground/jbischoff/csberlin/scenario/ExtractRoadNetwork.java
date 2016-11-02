/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.csberlin.scenario;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ExtractRoadNetwork {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile("../../../shared-svn/studies/jbischoff/multimodal/berlin/input/10pct/network.final10pct.xml.gz");
		NetworkFilterManager m = new NetworkFilterManager(network);
		m.addLinkFilter(new NetworkLinkFilter() {
			
			@Override
			public boolean judgeLink(Link l) {
				if (l.getAllowedModes().contains(TransportMode.car)) return true;
				else return false;
			}
		});
		Network network2 = m.applyFilters();
		new NetworkWriter(network2).write("../../../shared-svn/studies/jbischoff/multimodal/berlin/input/10pct/network.final10pct_car.xml.gz");

	}

}
