/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.polettif.publicTransitMapping.workbench;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import playground.polettif.publicTransitMapping.mapping.router.LinkFilterMode;
import playground.polettif.publicTransitMapping.tools.NetworkTools;

import java.util.HashSet;
import java.util.Set;

public class NetworkModeCleaner {
	
	public static void main(final String[] args) {
		String in = "C:/Users/polettif/Desktop/data/network/multimodal/zurich-gtfs.xml.gz";
		String out = "C:/Users/polettif/Desktop/data/network/multimodal/zurich-gtfs-withoutTram.xml.gz";
		Network network = NetworkTools.loadNetwork(in);

		NetworkFilterManager filterManager = new NetworkFilterManager(network);
		Set<String> modes = new HashSet<>();
		modes.add("car");
		modes.add("rail");
		modes.add("light_rail");
		modes.add("bus");
		modes.add("pt");
		filterManager.addLinkFilter(new LinkFilterMode(modes));

		NetworkTools.writeNetwork(filterManager.applyFilters(), out);
	}


	private class LinkFilter implements NetworkLinkFilter {

		@Override
		public boolean judgeLink(Link l) {
			return false;
		}
	}
}